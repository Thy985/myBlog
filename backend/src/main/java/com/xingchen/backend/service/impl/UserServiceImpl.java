package com.xingchen.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.LoginDTO;
import com.xingchen.backend.dto.PasswordUpdateDTO;
import com.xingchen.backend.dto.UserRegisterDTO;
import com.xingchen.backend.dto.UserUpdateDTO;
import com.xingchen.backend.entity.LoginHistory;
import com.xingchen.backend.entity.User;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.UserService;
import com.xingchen.backend.service.VerificationCodeService;
import com.xingchen.backend.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final LoginHistoryMapper loginHistoryMapper;
    private final VerificationCodeService verificationCodeService;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final ArticleLikeMapper articleLikeMapper;

    @Override
    @Transactional
    public Map<String, Object> login(LoginDTO dto, String ip, String device) {
        User user = userMapper.selectByUsername(dto.getUsername());
        
        if (user == null && dto.getUsername().contains("@")) {
            user = userMapper.selectByEmail(dto.getUsername());
        }
        
        if (user == null) {
            user = userMapper.selectByPhone(dto.getUsername());
        }

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_DISABLED);
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        userMapper.updateLoginInfo(user.getId(), ip);

        LoginHistory history = new LoginHistory();
        history.setUserId(user.getId());
        history.setLoginTime(LocalDateTime.now());
        history.setLoginIp(ip);
        history.setDevice(device);
        loginHistoryMapper.insert(history);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("user", getUserInfo(user.getId()));
        return result;
    }

    @Override
    public void logout(String token) {
        StpUtil.logoutByTokenValue(token);
    }

    @Override
    public Map<String, Object> refreshToken(String token) {
        StpUtil.renewTimeout(token, 7 * 24 * 60 * 60);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("expireTime", 7 * 24 * 60 * 60);
        return result;
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            return null;
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public List<String> getUserRoles(Long userId) {
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    @Transactional
    public Object register(UserRegisterDTO dto) {
        log.info("开始注册流程 - 用户名: {}, 邮箱: {}", dto.getUsername(), dto.getEmail());
        
        try {
            verificationCodeService.verifyCode(dto.getEmail(), dto.getCode(), "register");
        } catch (Exception e) {
            log.warn("验证码验证失败 - 邮箱: {}, 验证码: {}", dto.getEmail(), dto.getCode());
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码无效或已过期");
        }
        log.info("验证码验证成功");
        
        User existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (dto.getEmail() != null && !dto.getEmail().isEmpty()) {
            existingUser = userMapper.selectByEmail(dto.getEmail());
            if (existingUser != null) {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
            }
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(1);
        user.setLoginCount(0);
        user.setIsDeleted(0);
        user.setCreatedTime(LocalDateTime.now());
        user.setRegisterTime(LocalDateTime.now());

        userMapper.insert(user);
        log.info("用户注册成功 - 用户ID: {}, 用户名: {}", user.getId(), user.getUsername());
        return getUserInfo(user.getId());
    }

    @Override
    @Transactional
    public void updateUserInfo(Long userId, UserUpdateDTO dto) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void updatePassword(Long userId, PasswordUpdateDTO dto) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.ORIGINAL_PASSWORD_ERROR);
        }

        userMapper.updatePassword(userId, BCrypt.hashpw(dto.getNewPassword()));
    }

    @Override
    public Map<String, Object> getUserStats(Long userId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("articleCount", articleMapper.countPublishedByUserId(userId));
        stats.put("commentCount", commentMapper.countByUserId(userId));
        Long likeCount = articleLikeMapper.selectCountByUserId(userId);
        stats.put("likeCount", likeCount != null ? likeCount : 0);
        return stats;
    }

    @Override
    public List<Map<String, Object>> getUserRecentActivities(Long userId) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        activities.addAll(getArticleActivities(userId));
        activities.addAll(getCommentActivities(userId));
        activities.addAll(getLikeActivities(userId));
        
        activities.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("createdAt");
            LocalDateTime timeB = (LocalDateTime) b.get("createdAt");
            if (timeA == null && timeB == null) return 0;
            if (timeA == null) return 1;
            if (timeB == null) return -1;
            return timeB.compareTo(timeA);
        });
        
        return activities.subList(0, Math.min(activities.size(), 10));
    }

    private List<Map<String, Object>> getArticleActivities(Long userId) {
        List<Map<String, Object>> activities = new ArrayList<>();
        QueryWrapper wrapper = QueryWrapper.create()
            .eq("user_id", userId)
            .eq("is_deleted", 0)
            .orderBy("create_time", false);
        var articles = articleMapper.selectListByQuery(wrapper);
        for (var article : articles) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "article_" + article.getId());
            activity.put("description", "发布了文章《" + article.getTitle() + "》");
            activity.put("createdAt", article.getCreatedTime());
            activities.add(activity);
        }
        return activities;
    }

    private List<Map<String, Object>> getCommentActivities(Long userId) {
        List<Map<String, Object>> activities = new ArrayList<>();
        QueryWrapper wrapper = QueryWrapper.create()
            .eq("user_id", userId)
            .eq("status", 1)
            .eq("is_deleted", 0)
            .orderBy("create_time", false);
        var comments = commentMapper.selectListByQuery(wrapper);
        for (var comment : comments) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "comment_" + comment.getId());
            activity.put("description", "评论了文章");
            activity.put("createdAt", comment.getCreateTime());
            activities.add(activity);
        }
        return activities;
    }

    private List<Map<String, Object>> getLikeActivities(Long userId) {
        List<Map<String, Object>> activities = new ArrayList<>();
        QueryWrapper wrapper = QueryWrapper.create()
            .eq("user_id", userId)
            .orderBy("create_time", false);
        var likes = articleLikeMapper.selectListByQuery(wrapper);
        for (var like : likes) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("id", "like_" + like.getArticleId() + "_" + like.getUserId());
            activity.put("description", "点赞了文章");
            activity.put("createdAt", like.getCreatedTime());
            activities.add(activity);
        }
        return activities;
    }
}
