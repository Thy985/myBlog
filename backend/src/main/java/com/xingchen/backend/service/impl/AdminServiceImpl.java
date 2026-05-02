package com.xingchen.backend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.dto.UserCreateDTO;
import com.xingchen.backend.entity.*;
import com.xingchen.backend.enums.UserStatus;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.AdminService;
import com.xingchen.backend.vo.UserAdminVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;
    private final BlogSettingMapper blogSettingMapper;
    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;

    /**
     * 获取管理后台仪表板数据
     * 
     * 【优化说明】
     * 1. 使用COUNT(*)替代selectAll()，避免全表扫描和大量数据传输
     * 2. 使用数据库LIMIT直接查询最近5篇文章，避免内存中截取
     * 3. 使用数据库ORDER BY确保按发布时间排序
     * 
     * @return 包含用户数、文章数、评论数和最近文章的统计数据Map
     *         - userCount: 用户总数
     *         - articleCount: 文章总数  
     *         - commentCount: 评论总数
     *         - recentArticles: 最近的5篇文章列表
     */
    @Override
    public Map<String, Object> getDashboardData() {
        Map<String, Object> data = new HashMap<>();

        // 【优化】使用COUNT(*)统计用户数量，避免全表扫描
        // 原代码：List<User> allUsers = userMapper.selectAll(); data.put("userCount", (long) allUsers.size());
        // 问题：如果用户表有10万条记录，会一次性加载10万条数据到内存
        long userCount = userMapper.countAll();
        data.put("userCount", userCount);

        // 【优化】使用COUNT(*)统计文章数量
        long articleCount = articleMapper.countAll();
        data.put("articleCount", articleCount);

        // 【优化】使用COUNT(*)统计评论数量
        long commentCount = commentMapper.countAll();
        data.put("commentCount", commentCount);

        // 【优化】使用数据库LIMIT和ORDER BY查询最近5篇文章
        // 原代码：List<Article> recentArticles = articleMapper.selectAll(); data.put("recentArticles", recentArticles.stream().limit(5).collect(Collectors.toList()));
        // 问题1：selectAll()会加载所有文章到内存
        // 问题2：没有排序，取出的可能是任意5篇文章
        // 问题3：stream().limit(5)在内存中截取，浪费资源
        List<Article> recentArticles = articleMapper.selectRecentArticles(5);
        data.put("recentArticles", recentArticles);

        return data;
    }

    @Override
    public List<UserAdminVO> getUserList(Integer page, Integer size, String keyword, String status) {
        int offset = (page - 1) * size;
        Integer statusValue = UserStatus.toCode(status);
        if (!UserStatus.ACTIVE.getName().equals(status) && !UserStatus.DISABLED.getName().equals(status)) {
            statusValue = null;
        }
        List<User> users = userMapper.selectByPage(keyword, statusValue != null ? String.valueOf(statusValue) : null, offset, size);
        
        // 【优化】批量查询用户角色，避免N+1问题
        return convertToAdminVOBatch(users);
    }

    @Override
    public long countUsers(String keyword, String status) {
        Integer statusValue = UserStatus.toCode(status);
        if (!UserStatus.ACTIVE.getName().equals(status) && !UserStatus.DISABLED.getName().equals(status)) {
            statusValue = null;
        }
        return userMapper.countByCondition(keyword, statusValue != null ? String.valueOf(statusValue) : null);
    }

    @Override
    public UserAdminVO createUser(UserCreateDTO dto) {
        User existingUser = userMapper.selectByUsername(dto.getUsername());
        if (existingUser != null) {
            throw new BusinessException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(hashPassword(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setStatus(1);
        user.setCreatedTime(LocalDateTime.now());
        user.setLoginCount(0);
        user.setRegisterTime(LocalDateTime.now());

        userMapper.insert(user);
        return convertToAdminVO(user);
    }

    @Override
    public void updateUserStatus(Long id, String status) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(UserStatus.toCode(status));
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.update(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        user.setIsDeleted(1);
        userMapper.update(user);
    }

    /**
     * 为用户分配角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色ID列表
     */
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "用户不存在");
        }
        
        userRoleMapper.deleteByUserId(userId);
        
        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    @Override
    public Map<String, String> getSettings() {
        Map<String, String> result = new HashMap<>();
        BlogSetting setting = getOrCreateBlogSetting();
        
        if (setting.getBlogName() != null) {
            result.put("blogName", setting.getBlogName());
        }
        if (setting.getAuthor() != null) {
            result.put("author", setting.getAuthor());
        }
        if (setting.getIntroduction() != null) {
            result.put("introduction", setting.getIntroduction());
        }
        if (setting.getSeoTitle() != null) {
            result.put("seoTitle", setting.getSeoTitle());
        }
        if (setting.getSeoKeywords() != null) {
            result.put("seoKeywords", setting.getSeoKeywords());
        }
        if (setting.getSeoDescription() != null) {
            result.put("seoDescription", setting.getSeoDescription());
        }
        return result;
    }

    @Override
    public void updateSetting(String key, String value) {
        BlogSetting setting = getOrCreateBlogSetting();

        setting.setUpdateTime(LocalDateTime.now());

        switch (key) {
            case "blogName":
                setting.setBlogName(value);
                break;
            case "author":
                setting.setAuthor(value);
                break;
            case "introduction":
                setting.setIntroduction(value);
                break;
            case "seoTitle":
                setting.setSeoTitle(value);
                break;
            case "seoKeywords":
                setting.setSeoKeywords(value);
                break;
            case "seoDescription":
                setting.setSeoDescription(value);
                break;
            case "beianCode":
                setting.setBeianCode(value);
                break;
            case "beianLink":
                setting.setBeianLink(value);
                break;
            case "github":
                setting.setGithubHome(value);
                break;
            case "gitee":
                setting.setGiteeHome(value);
                break;
            case "csdn":
                setting.setCsdnHome(value);
                break;
            case "zhihu":
                setting.setZhihuHome(value);
                break;
            case "avatar":
                setting.setAvatar(value);
                break;
            case "logo":
                setting.setLogo(value);
                break;
            default:
                throw new IllegalArgumentException("未知的设置项: " + key);
        }

        if (setting.getId() == null) {
            setting.setCreateTime(LocalDateTime.now());
            blogSettingMapper.insert(setting);
        } else {
            blogSettingMapper.update(setting);
        }
    }
    
    /**
     * 获取或创建博客设置（复用方法，避免重复查询）
     */
    private BlogSetting getOrCreateBlogSetting() {
        BlogSetting setting = blogSettingMapper.selectFirst();
        return setting != null ? setting : new BlogSetting();
    }

    /**
     * 单个用户转换为VO（适用于单用户场景）
     */
    private UserAdminVO convertToAdminVO(User user) {
        UserAdminVO vo = new UserAdminVO();
        BeanUtils.copyProperties(user, vo);

        List<String> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        vo.setRoles(roles);

        return vo;
    }

    /**
     * 批量用户转换为VO（优化N+1查询问题）
     * 【优化说明】
     * 1. 收集所有用户ID
     * 2. 批量查询所有用户的角色（1次查询替代N次）
     * 3. 构建用户ID->角色列表的映射
     * 4. 批量转换VO
     */
    private List<UserAdminVO> convertToAdminVOBatch(List<User> users) {
        if (users == null || users.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 收集所有用户ID
        List<Long> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        // 2. 批量查询所有用户的角色（避免N+1查询）
        Map<Long, List<String>> userRolesMap = userRoleMapper.selectRoleCodesByUserIds(userIds);

        // 3. 批量转换VO
        return users.stream()
                .map(user -> {
                    UserAdminVO vo = new UserAdminVO();
                    BeanUtils.copyProperties(user, vo);
                    // 从缓存获取角色列表
                    vo.setRoles(userRolesMap.getOrDefault(user.getId(), new ArrayList<>()));
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private String hashPassword(String password) {
        return BCrypt.hashpw(password);
    }
}
