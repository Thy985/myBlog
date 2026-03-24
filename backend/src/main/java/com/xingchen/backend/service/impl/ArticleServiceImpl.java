package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.common.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.common.PageResult;
import com.xingchen.backend.dto.ArticleCreateDTO;
import com.xingchen.backend.dto.ArticleUpdateDTO;
import com.xingchen.backend.entity.*;
import com.xingchen.backend.enums.ArticleStatus;
import com.xingchen.backend.mapper.*;
import com.xingchen.backend.service.ArticleService;
import com.xingchen.backend.util.RedisLockUtil;
import com.xingchen.backend.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleServiceImpl implements ArticleService {

    private static final char ESCAPE_CHAR = '\\';
    
    private static final long CACHE_NULL_TTL_MINUTES = 5;
    private static final long CACHE_ARTICLE_TTL_MINUTES = 30;
    private static final long CACHE_LIST_TTL_MINUTES = 10;
    private static final long CACHE_HOT_TTL_MINUTES = 5;
    private static final long LOCK_EXPIRE_SECONDS = 10;
    private static final int LOCK_RETRY_TIMES = 3;
    private static final long LOCK_RETRY_INTERVAL_MS = 100;

    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisLockUtil redisLockUtil;

    private String escapeLikeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        return keyword.replace(ESCAPE_CHAR, ESCAPE_CHAR)
                     .replace("%", ESCAPE_CHAR + "%")
                     .replace("_", ESCAPE_CHAR + "_");
    }

    private final ArticleMapper articleMapper;
    private final ArticleContentMapper articleContentMapper;
    private final ArticleCategoryMapper articleCategoryMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final ArticleCollectMapper articleCollectMapper;
    private final ArticleReadStatsMapper articleReadStatsMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final SearchHistoryMapper searchHistoryMapper;

    @Override
    @Transactional
    public ArticleVO createArticle(Long userId, ArticleCreateDTO dto) {
        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(dto.getTitle());
        article.setTitleImage(dto.getTitleImage());
        article.setDescription(dto.getDescription());
        article.setStatus(1);
        article.setCommentStatus(dto.getCommentStatus() != null ? dto.getCommentStatus() : 1);
        article.setTopStatus(dto.getTopStatus() != null ? dto.getTopStatus() : 0);
        article.setViewStatus(dto.getViewStatus() != null ? dto.getViewStatus() : 1);
        article.setReadNum(0);
        article.setCommentNum(0);
        article.setLikeNum(0);
        article.setCollectNum(0);
        article.setShareNum(0);
        article.setIsDeleted(0);
        article.setCreatedTime(LocalDateTime.now());
        article.setPublishTime(LocalDateTime.now());

        articleMapper.insert(article);
        Long articleId = article.getId();

        if (dto.getContent() != null) {
            ArticleContent content = new ArticleContent();
            content.setArticleId(articleId);
            content.setContent(dto.getContent());
            content.setWordCount(dto.getContent() != null ? dto.getContent().length() : 0);
            articleContentMapper.insert(content);
        }

        if (dto.getCategoryId() != null) {
            ArticleCategory ac = new ArticleCategory();
            ac.setArticleId(article.getId());
            ac.setCategoryId(dto.getCategoryId());
            articleCategoryMapper.insert(ac);
        }

        if (dto.getTagIds() != null && !dto.getTagIds().isEmpty()) {
            for (Long tagId : dto.getTagIds()) {
                ArticleTag at = new ArticleTag();
                at.setArticleId(article.getId());
                at.setTagId(tagId);
                articleTagMapper.insert(at);
            }
        }
        
        clearArticleCache(article.getId());

        return getArticleById(article.getId(), userId);
    }

    @Override
    @Transactional
    public ArticleVO updateArticle(Long userId, Long id, ArticleUpdateDTO dto) {
        Article article = articleMapper.selectOneById(id);
        if (article == null || article.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.ARTICLE_NOT_FOUND);
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ARTICLE_UPDATE_NO_PERMISSION);
        }

        if (dto.getTitle() != null) {
            article.setTitle(dto.getTitle());
        }
        if (dto.getThumbnail() != null) {
            article.setTitleImage(dto.getThumbnail());
        }
        if (dto.getSummary() != null) {
            article.setDescription(dto.getSummary());
        }

        article.setUpdatedTime(LocalDateTime.now());
        articleMapper.update(article);

        if (dto.getContent() != null) {
            ArticleContent content = articleContentMapper.selectByArticleId(id);
            
            if (content == null) {
                content = new ArticleContent();
                content.setArticleId(id);
                content.setContent(dto.getContent());
                content.setWordCount(dto.getContent().length());
                articleContentMapper.insert(content);
            } else {
                content.setContent(dto.getContent());
                content.setWordCount(dto.getContent().length());
                articleContentMapper.update(content);
            }
        }
        
        clearArticleCache(id);

        return getArticleById(id, userId);
    }

    @Override
    @Transactional
    public void deleteArticle(Long userId, Long id) {
        Article article = articleMapper.selectOneById(id);
        if (article == null || article.getIsDeleted() == 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "文章不存在");
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ARTICLE_DELETE_NO_PERMISSION);
        }

        article.setIsDeleted(1);
        articleMapper.update(article);
        
        clearArticleCache(id);
    }

    @Override
    public ArticleVO getArticleById(Long id, Long userId) {
        String cacheKey = "article::" + id + "::" + (userId != null ? userId : "anonymous");
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ArticleVO) {
            log.debug("缓存命中：{}", cacheKey);
            return (ArticleVO) cached;
        }
        
        if (cached instanceof NullObject) {
            log.debug("缓存命中空对象：{}", cacheKey);
            return null;
        }
        
        return redisLockUtil.executeWithLock(
            cacheKey,
            LOCK_EXPIRE_SECONDS,
            LOCK_RETRY_TIMES,
            LOCK_RETRY_INTERVAL_MS,
            () -> loadArticleFromDB(id, userId, cacheKey)
        );
    }
    
    private ArticleVO loadArticleFromDB(Long id, Long userId, String cacheKey) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ArticleVO) {
            log.debug("双重检查缓存命中：{}", cacheKey);
            return (ArticleVO) cached;
        }
        
        Article article = articleMapper.selectOneById(id);
        if (article == null || article.getIsDeleted() == 1) {
            redisTemplate.opsForValue().set(cacheKey, new NullObject(), CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("文章不存在，缓存空对象：{}", cacheKey);
            return null;
        }

        if (article.getViewStatus() == 0 && (userId == null || !article.getUserId().equals(userId))) {
            redisTemplate.opsForValue().set(cacheKey, new NullObject(), CACHE_NULL_TTL_MINUTES, TimeUnit.MINUTES);
            log.debug("文章为私密状态，缓存空对象：{}", cacheKey);
            return null;
        }

        updateReadNum(id);
        
        Article updatedArticle = articleMapper.selectOneById(id);

        ArticleVO vo = new ArticleVO();
        BeanUtils.copyProperties(updatedArticle, vo);
        vo.setStatus(ArticleStatus.toName(updatedArticle.getStatus()));
        vo.setIsTop(updatedArticle.getTopStatus());

        User author = userMapper.selectOneById(updatedArticle.getUserId());
        if (author != null) {
            UserVO authorVO = new UserVO();
            authorVO.setId(author.getId());
            authorVO.setUsername(author.getUsername());
            authorVO.setNickname(author.getNickname());
            authorVO.setAvatar(author.getAvatar());
            vo.setAuthor(authorVO);
        }

        ArticleContent content = articleContentMapper.selectByArticleId(id);
        if (content != null) {
            vo.setContent(content.getContent());
        }

        ArticleCategory articleCategory = articleCategoryMapper.selectByArticleId(id);
        if (articleCategory != null) {
            Category category = categoryMapper.selectOneById(articleCategory.getCategoryId());
            if (category != null) {
                CategoryVO categoryVO = new CategoryVO();
                BeanUtils.copyProperties(category, categoryVO);
                vo.setCategory(categoryVO);
            }
        }

        List<ArticleTag> articleTags = articleTagMapper.selectByArticleId(id);
        if (articleTags != null && !articleTags.isEmpty()) {
            List<TagVO> tagVOList = articleTags.stream().map(at -> {
                Tag tag = tagMapper.selectOneById(at.getTagId());
                if (tag != null) {
                    TagVO tagVO = new TagVO();
                    BeanUtils.copyProperties(tag, tagVO);
                    return tagVO;
                }
                return null;
            }).filter(t -> t != null).collect(java.util.stream.Collectors.toList());
            vo.setTags(tagVOList);
        }

        Article preArticle = articleMapper.selectPreArticle(id);
        if (preArticle != null) {
            ArticleVO preVO = new ArticleVO();
            BeanUtils.copyProperties(preArticle, preVO);
            preVO.setThumbnail(preArticle.getTitleImage());
            vo.setPreArticle(preVO);
        }

        Article nextArticle = articleMapper.selectNextArticle(id);
        if (nextArticle != null) {
            ArticleVO nextVO = new ArticleVO();
            BeanUtils.copyProperties(nextArticle, nextVO);
            nextVO.setThumbnail(nextArticle.getTitleImage());
            vo.setNextArticle(nextVO);
        }
        
        long ttl = CACHE_ARTICLE_TTL_MINUTES + randomOffset(5);
        redisTemplate.opsForValue().set(cacheKey, vo, ttl, TimeUnit.MINUTES);
        log.info("文章详情已缓存到 DB，TTL={} 分钟：{}", ttl, cacheKey);

        return vo;
    }

    @Override
    public PageResult<ArticleListVO> getArticleList(Integer page, Integer size, String keyword, Long categoryId, Long tagId) {
        return getArticleList(page, size, keyword, categoryId, tagId, null);
    }

    @Override
    public PageResult<ArticleListVO> getArticleList(Integer page, Integer size, String keyword, Long categoryId, Long tagId, Long userId) {
        int offset = (page - 1) * size;
        List<Article> articles;
        long total;
        
        if (userId != null) {
            if (keyword != null && !keyword.isEmpty()) {
                String escapedKeyword = escapeLikeKeyword(keyword);
                articles = articleMapper.selectByUserIdAndKeyword(userId, escapedKeyword, offset, size);
                total = articleMapper.countByUserIdAndKeyword(userId, escapedKeyword);
            } else if (categoryId != null) {
                articles = articleMapper.selectByUserIdAndCategoryId(userId, categoryId, offset, size);
                total = articleMapper.countByUserIdAndCategoryId(userId, categoryId);
            } else if (tagId != null) {
                articles = articleMapper.selectByUserIdAndTagId(userId, tagId, offset, size);
                total = articleMapper.countByUserIdAndTagId(userId, tagId);
            } else {
                articles = articleMapper.selectByUserId(userId, offset, size);
                total = articleMapper.countByUserId(userId);
            }
        } else {
            if (keyword != null && !keyword.isEmpty()) {
                String escapedKeyword = escapeLikeKeyword(keyword);
                articles = articleMapper.selectByKeyword(escapedKeyword, offset, size);
                total = articleMapper.countByKeyword(escapedKeyword);
            } else if (categoryId != null) {
                articles = articleMapper.selectByCategoryId(categoryId, offset, size);
                total = articleMapper.countByCategoryId(categoryId);
            } else if (tagId != null) {
                articles = articleMapper.selectByTagId(tagId, offset, size);
                total = articleMapper.countByTagId(tagId);
            } else {
                articles = articleMapper.selectPublicArticlesPage(offset, size);
                total = articleMapper.countPublicArticles();
            }
        }

        List<ArticleListVO> voList = convertToListVOBatch(articles);
        return PageResult.of(voList, total, page, size);
    }

    @Override
    public PageResult<ArticleListVO> getUserArticles(Long userId, Integer page, Integer size) {
        return getUserArticles(userId, page, size, null);
    }

    @Override
    public PageResult<ArticleListVO> getUserArticles(Long userId, Integer page, Integer size, String keyword) {
        int offset = (page - 1) * size;
        List<Article> articles;
        long total;
        
        if (keyword != null && !keyword.trim().isEmpty()) {
            String escapedKeyword = escapeLikeKeyword(keyword);
            articles = articleMapper.selectByUserIdAndKeyword(userId, escapedKeyword, offset, size);
            total = articleMapper.countByUserIdAndKeyword(userId, escapedKeyword);
        } else {
            articles = articleMapper.selectByUserId(userId, offset, size);
            total = articleMapper.countByUserId(userId);
        }
        
        List<ArticleListVO> voList = convertToListVOBatch(articles);
        return PageResult.of(voList, total, page, size);
    }

    @Override
    public List<ArticleVO> getHotArticles(Integer limit) {
        String cacheKey = "hotArticles::" + limit;
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            log.debug("缓存命中：{}", cacheKey);
            return (List<ArticleVO>) cached;
        }
        
        return redisLockUtil.executeWithLock(
            cacheKey,
            LOCK_EXPIRE_SECONDS,
            LOCK_RETRY_TIMES,
            LOCK_RETRY_INTERVAL_MS,
            () -> loadHotArticlesFromDB(limit, cacheKey)
        );
    }
    
    private List<ArticleVO> loadHotArticlesFromDB(Integer limit, String cacheKey) {
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            log.debug("双重检查缓存命中：{}", cacheKey);
            return (List<ArticleVO>) cached;
        }
        
        List<Article> articles = articleMapper.selectHotArticles(limit);
        List<ArticleVO> result = articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            BeanUtils.copyProperties(article, vo);
            vo.setStatus(ArticleStatus.toName(article.getStatus()));
            vo.setIsTop(article.getTopStatus());
            return vo;
        }).collect(Collectors.toList());
        
        long ttl = CACHE_HOT_TTL_MINUTES + randomOffset(2);
        redisTemplate.opsForValue().set(cacheKey, result, ttl, TimeUnit.MINUTES);
        log.info("热门文章已缓存，TTL={} 分钟：{}", ttl, cacheKey);
        
        return result;
    }

    @Override
    @Transactional
    public void publishArticle(Long userId, Long id) {
        Article article = articleMapper.selectOneById(id);
        if (article == null || !article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        article.setStatus(1);
        article.setPublishTime(LocalDateTime.now());
        articleMapper.update(article);
        
        clearArticleCache(id);
    }

    @Override
    @Transactional
    public void offlineArticle(Long userId, Long id) {
        Article article = articleMapper.selectOneById(id);
        if (article == null || !article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        article.setStatus(0);
        articleMapper.update(article);
        
        clearArticleCache(id);
    }

    @Override
    @Transactional
    public void topArticle(Long userId, Long id, Boolean isTop) {
        Article article = articleMapper.selectOneById(id);
        if (article == null || !article.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        article.setTopStatus(isTop ? 1 : 0);
        articleMapper.update(article);
        
        clearArticleCache(id);
    }

    @Override
    @Transactional
    public void likeArticle(Long userId, Long id) {
        ArticleLike existingLike = articleLikeMapper.selectByArticleAndUser(id, userId);
        if (existingLike != null) {
            return;
        }

        ArticleLike like = new ArticleLike();
        like.setArticleId(id);
        like.setUserId(userId);
        articleLikeMapper.insert(like);
        articleMapper.incrementLikeNum(id);
    }

    @Override
    @Transactional
    public void unlikeArticle(Long userId, Long id) {
        ArticleLike existingLike = articleLikeMapper.selectByArticleAndUser(id, userId);
        if (existingLike != null) {
            articleLikeMapper.delete(existingLike);
            articleMapper.decrementLikeNum(id);
        }
    }

    @Override
    @Transactional
    public void collectArticle(Long userId, Long id) {
        ArticleCollect existingCollect = articleCollectMapper.selectByArticleAndUser(id, userId);
        if (existingCollect != null) {
            return;
        }

        ArticleCollect collect = new ArticleCollect();
        collect.setArticleId(id);
        collect.setUserId(userId);
        articleCollectMapper.insert(collect);
        articleMapper.incrementCollectNum(id);
    }

    @Override
    @Transactional
    public void uncollectArticle(Long userId, Long id) {
        ArticleCollect existingCollect = articleCollectMapper.selectByArticleAndUser(id, userId);
        if (existingCollect != null) {
            articleCollectMapper.delete(existingCollect);
            articleMapper.decrementCollectNum(id);
        }
    }

    @Override
    public PageResult<ArticleListVO> getUserCollects(Long userId, Integer page, Integer size) {
        int offset = (page - 1) * size;
        List<Long> articleIds = articleCollectMapper.selectArticleIdsByUserId(userId, offset, size);
        long total = articleCollectMapper.countByUserId(userId);
        
        List<ArticleListVO> result = new ArrayList<>();
        for (Long articleId : articleIds) {
            Article article = articleMapper.selectOneById(articleId);
            if (article != null && article.getIsDeleted() == 0) {
                result.add(convertToListVO(article));
            }
        }
        return PageResult.of(result, total, page, size);
    }

    @Override
    public Map<String, Object> getArticleArchive(Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        List<Article> articles = articleMapper.selectPublicArticles();

        Map<String, List<Article>> archiveMap = new LinkedHashMap<>();
        
        for (Article article : articles) {
            LocalDateTime timeToUse = article.getPublishTime();
            if (timeToUse == null) {
                timeToUse = article.getCreatedTime();
            }
            if (timeToUse != null) {
                String month = timeToUse.toLocalDate().toString().substring(0, 7);
                archiveMap.computeIfAbsent(month, k -> new ArrayList<>()).add(article);
            }
        }
        
        List<Map<String, Object>> archives = new ArrayList<>();
        
        List<String> sortedMonths = new ArrayList<>(archiveMap.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a));
        
        for (String month : sortedMonths) {
            List<Article> monthArticles = archiveMap.get(month);
            
            monthArticles.sort((a, b) -> {
                LocalDateTime timeA = a.getPublishTime() != null ? a.getPublishTime() : a.getCreatedTime();
                LocalDateTime timeB = b.getPublishTime() != null ? b.getPublishTime() : b.getCreatedTime();
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            });
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("articles", monthArticles.stream()
                .map(a -> {
                    Map<String, Object> articleMap = new HashMap<>();
                    articleMap.put("id", a.getId());
                    articleMap.put("title", a.getTitle());
                    articleMap.put("titleImage", a.getTitleImage());
                    articleMap.put("createTime", a.getCreatedTime());
                    articleMap.put("publishTime", a.getPublishTime());
                    articleMap.put("createMonth", month);
                    return articleMap;
                })
                .collect(Collectors.toList()));
            archives.add(monthData);
        }

        result.put("list", archives);
        result.put("total", (long) archives.size());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) archives.size() / size));
        return result;
    }

    @Override
    public Map<String, Object> getUserArticleArchive(Long userId, Integer page, Integer size) {
        Map<String, Object> result = new HashMap<>();
        
        QueryWrapper wrapper = QueryWrapper.create()
                .from("t_article")
                .where("user_id = ?", userId)
                .and("status = 1")
                .and("is_deleted = 0");
        List<Article> articles = articleMapper.selectListByQuery(wrapper);

        Map<String, List<Article>> archiveMap = new LinkedHashMap<>();
        
        for (Article article : articles) {
            LocalDateTime timeToUse = article.getPublishTime();
            if (timeToUse == null) {
                timeToUse = article.getCreatedTime();
            }
            if (timeToUse != null) {
                String month = timeToUse.toLocalDate().toString().substring(0, 7);
                archiveMap.computeIfAbsent(month, k -> new ArrayList<>()).add(article);
            }
        }
        
        List<Map<String, Object>> archives = new ArrayList<>();
        
        List<String> sortedMonths = new ArrayList<>(archiveMap.keySet());
        sortedMonths.sort((a, b) -> b.compareTo(a));
        
        for (String month : sortedMonths) {
            List<Article> monthArticles = archiveMap.get(month);
            
            monthArticles.sort((a, b) -> {
                LocalDateTime timeA = a.getPublishTime() != null ? a.getPublishTime() : a.getCreatedTime();
                LocalDateTime timeB = b.getPublishTime() != null ? b.getPublishTime() : b.getCreatedTime();
                if (timeA == null && timeB == null) return 0;
                if (timeA == null) return 1;
                if (timeB == null) return -1;
                return timeB.compareTo(timeA);
            });
            
            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("articles", monthArticles.stream()
                .map(a -> {
                    Map<String, Object> articleMap = new HashMap<>();
                    articleMap.put("id", a.getId());
                    articleMap.put("title", a.getTitle());
                    articleMap.put("titleImage", a.getTitleImage());
                    articleMap.put("createTime", a.getCreatedTime());
                    articleMap.put("publishTime", a.getPublishTime());
                    articleMap.put("createMonth", month);
                    return articleMap;
                })
                .collect(Collectors.toList()));
            archives.add(monthData);
        }

        result.put("list", archives);
        result.put("total", (long) archives.size());
        result.put("page", page);
        result.put("size", size);
        result.put("pages", (int) Math.ceil((double) archives.size() / size));
        return result;
    }

    private ArticleListVO convertToListVO(Article article) {
        ArticleListVO vo = new ArticleListVO();
        BeanUtils.copyProperties(article, vo);
        vo.setStatus(ArticleStatus.toName(article.getStatus()));
        vo.setIsTop(article.getTopStatus());

        return vo;
    }

    private List<ArticleListVO> convertToListVOBatch(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> articleIds = articles.stream().map(Article::getId).collect(Collectors.toList());
        List<Long> userIds = articles.stream().map(Article::getUserId).distinct().collect(Collectors.toList());

        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<User> users = userMapper.selectListByQuery(
                QueryWrapper.create().in("id", userIds)
            );
            userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        }

        Map<Long, ArticleCategory> articleCategoryMap = new HashMap<>();
        List<ArticleCategory> articleCategories = articleCategoryMapper.selectByArticleIds(articleIds);
        for (ArticleCategory ac : articleCategories) {
            articleCategoryMap.put(ac.getArticleId(), ac);
        }

        Set<Long> categoryIds = articleCategories.stream()
            .map(ArticleCategory::getCategoryId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, Category> categoryMap = new HashMap<>();
        if (!categoryIds.isEmpty()) {
            List<Category> categories = categoryMapper.selectListByQuery(
                QueryWrapper.create().in("id", categoryIds)
            );
            categoryMap = categories.stream().collect(Collectors.toMap(Category::getId, c -> c));
        }

        List<ArticleListVO> result = new ArrayList<>();
        for (Article article : articles) {
            ArticleListVO vo = new ArticleListVO();
            BeanUtils.copyProperties(article, vo);
            vo.setStatus(ArticleStatus.toName(article.getStatus()));
            vo.setIsTop(article.getTopStatus());

            User author = userMap.get(article.getUserId());
            if (author != null) {
                vo.setAuthorName(author.getNickname() != null ? author.getNickname() : author.getUsername());
                vo.setAuthorAvatar(author.getAvatar());
            }

            ArticleCategory articleCategory = articleCategoryMap.get(article.getId());
            if (articleCategory != null) {
                Category category = categoryMap.get(articleCategory.getCategoryId());
                if (category != null) {
                    vo.setCategoryName(category.getCategoryName());
                }
            }

            result.add(vo);
        }

        return result;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"article", "articleList", "userArticles", "hotArticles"}, allEntries = true)
    public void updateReadNum(Long id) {
        Article article = articleMapper.selectOneById(id);
        if (article != null && article.getIsDeleted() == 0) {
            article.setReadNum((article.getReadNum() != null ? article.getReadNum() : 0) + 1);
            articleMapper.update(article);
            
            recordReadStats(id);
        }
    }

    private void recordReadStats(Long articleId) {
        LocalDate today = LocalDate.now();
        ArticleReadStats stats = articleReadStatsMapper.selectByArticleAndDate(articleId, today);
        
        if (stats == null) {
            stats = new ArticleReadStats();
            stats.setArticleId(articleId);
            stats.setStatsDate(today);
            stats.setReadCount(1);
            stats.setUniqueVisitors(1);
            stats.setCreateTime(LocalDateTime.now());
            stats.setUpdateTime(LocalDateTime.now());
            articleReadStatsMapper.insert(stats);
        } else {
            articleReadStatsMapper.incrementReadCount(articleId, today);
        }
    }
    
    private void clearArticleCache(Long articleId) {
        Set<String> keys = new HashSet<>();
        
        keys.add("article::" + articleId + "::anonymous");
        keys.add("article::" + articleId + "::*");
        
        keys.add("articleList::*");
        keys.add("userArticles::*");
        keys.add("hotArticles::*");
        
        for (String pattern : keys) {
            Set<String> actualKeys = redisTemplate.keys(pattern);
            if (actualKeys != null && !actualKeys.isEmpty()) {
                redisTemplate.delete(actualKeys);
                log.debug("已清理缓存：{}", pattern);
            }
        }
    }
    
    private long randomOffset(long maxMinutes) {
        return new Random().nextInt((int) maxMinutes);
    }
    
    private static class NullObject implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
    }

    @Override
    public Map<String, Object> getArticleReadStats(Long id) {
        Map<String, Object> stats = new HashMap<>();
        Article article = articleMapper.selectOneById(id);
        if (article != null) {
            int total = article.getReadNum() != null ? article.getReadNum() : 0;
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            LocalDate monthAgo = today.minusDays(30);
            
            stats.put("total", total);
            
            Integer todayCount = articleReadStatsMapper.sumReadCountSince(id, today);
            stats.put("today", todayCount != null ? todayCount : 0);
            
            Integer weekCount = articleReadStatsMapper.sumReadCountSince(id, weekAgo);
            stats.put("week", weekCount != null ? weekCount : 0);
            
            Integer monthCount = articleReadStatsMapper.sumReadCountSince(id, monthAgo);
            stats.put("month", monthCount != null ? monthCount : 0);
            
            List<Map<String, Object>> trend = articleReadStatsMapper.selectTrendData(id, weekAgo, today);
            stats.put("trend", trend);
        }
        return stats;
    }

    @Override
    public List<ArticleListVO> getRelatedArticles(Long articleId, Integer limit) {
        Article currentArticle = articleMapper.selectOneById(articleId);
        if (currentArticle == null) {
            return new ArrayList<>();
        }

        final Long currentCategoryId;
        ArticleCategory articleCategory = articleCategoryMapper.selectByArticleId(articleId);
        currentCategoryId = articleCategory != null ? articleCategory.getCategoryId() : null;

        final List<Long> currentTagIds;
        List<ArticleTag> articleTags = articleTagMapper.selectByArticleId(articleId);
        currentTagIds = articleTags != null && !articleTags.isEmpty() 
            ? articleTags.stream().map(ArticleTag::getTagId).collect(Collectors.toList())
            : new ArrayList<>();

        List<Article> allArticles = articleMapper.selectPublicArticles();
        
        if (allArticles.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Long> articleIds = allArticles.stream()
            .filter(a -> !a.getId().equals(articleId))
            .map(Article::getId)
            .collect(Collectors.toList());
        
        Map<Long, Long> articleCategoryMap = new HashMap<>();
        List<ArticleCategory> allArticleCategories = articleCategoryMapper.selectByArticleIds(articleIds);
        for (ArticleCategory ac : allArticleCategories) {
            articleCategoryMap.put(ac.getArticleId(), ac.getCategoryId());
        }
        
        Map<Long, List<Long>> articleTagsMap = new HashMap<>();
        List<ArticleTag> allArticleTags = articleTagMapper.selectByArticleIds(articleIds);
        for (ArticleTag at : allArticleTags) {
            articleTagsMap.computeIfAbsent(at.getArticleId(), k -> new ArrayList<>()).add(at.getTagId());
        }
        
        List<ScoredArticle> scoredArticles = new ArrayList<>();
        for (Article article : allArticles) {
            if (article.getId().equals(articleId)) {
                continue;
            }
            
            int score = 0;
            
            Long articleCatId = articleCategoryMap.get(article.getId());
            if (currentCategoryId != null && currentCategoryId.equals(articleCatId)) {
                score += 10;
            }
            
            List<Long> articleTagIds = articleTagsMap.get(article.getId());
            if (!currentTagIds.isEmpty() && articleTagIds != null) {
                for (Long tagId : articleTagIds) {
                    if (currentTagIds.contains(tagId)) {
                        score += 5;
                    }
                }
            }
            
            score += (article.getReadNum() != null ? article.getReadNum() : 0) / 100;
            
            scoredArticles.add(new ScoredArticle(article, score));
        }
        
        scoredArticles.sort((a, b) -> b.score - a.score);
        
        if (scoredArticles.size() > limit) {
            scoredArticles = scoredArticles.subList(0, limit);
        }
        
        return scoredArticles.stream()
            .map(sa -> convertToListVO(sa.article))
            .collect(Collectors.toList());
    }
    
    // 内部类：用于存储文章和相关性分数
    private static class ScoredArticle {
        Article article;
        int score;
        
        ScoredArticle(Article article, int score) {
            this.article = article;
            this.score = score;
        }
    }
}
