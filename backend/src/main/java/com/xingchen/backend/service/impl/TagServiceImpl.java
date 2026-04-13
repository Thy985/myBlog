package com.xingchen.backend.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.xingchen.backend.exception.BusinessException;
import com.xingchen.backend.common.ErrorCode;
import com.xingchen.backend.entity.Article;
import com.xingchen.backend.entity.ArticleTag;
import com.xingchen.backend.entity.Tag;
import com.xingchen.backend.mapper.ArticleMapper;
import com.xingchen.backend.mapper.ArticleTagMapper;
import com.xingchen.backend.mapper.TagMapper;
import com.xingchen.backend.service.TagService;
import com.xingchen.backend.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final ArticleMapper articleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> getTagList() {
        List<Tag> tags = tagMapper.selectAll();
        List<TagVO> result = new ArrayList<>();
        for (Tag tag : tags) {
            if (tag.getIsDeleted() != null && tag.getIsDeleted() == 1) {
                continue;
            }
            TagVO vo = new TagVO();
            vo.setId(tag.getId());
            vo.setName(tag.getTagName());
            vo.setColor(tag.getColor());
            int count = countArticlesByTagId(tag.getId());
            vo.setArticleCount(count);
            result.add(vo);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> getUserTagList(Long userId) {
        QueryWrapper articleWrapper = QueryWrapper.create()
                .from("t_article")
                .where("user_id = ?", userId)
                .and("status = 1")
                .and("is_deleted = 0");
        List<Article> userArticles = articleMapper.selectListByQuery(articleWrapper);

        if (userArticles.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> articleIds = userArticles.stream()
                .map(Article::getId)
                .collect(Collectors.toList());

        QueryWrapper atWrapper = QueryWrapper.create()
                .from("t_article_tag")
                .in("article_id", articleIds);
        List<ArticleTag> articleTags = articleTagMapper.selectListByQuery(atWrapper);

        if (articleTags.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> tagIds = articleTags.stream()
                .map(ArticleTag::getTagId)
                .distinct()
                .collect(Collectors.toList());

        QueryWrapper tagWrapper = QueryWrapper.create()
                .from("t_tag")
                .in("id", tagIds);
        List<Tag> tags = tagMapper.selectListByQuery(tagWrapper);

        Map<Long, Long> tagArticleCountMap = articleTags.stream()
                .collect(Collectors.groupingBy(ArticleTag::getTagId, Collectors.counting()));

        return tags.stream()
                .map(tag -> {
                    TagVO vo = new TagVO();
                    vo.setId(tag.getId());
                    vo.setName(tag.getTagName());
                    vo.setColor(tag.getColor());
                    long count = tagArticleCountMap.getOrDefault(tag.getId(), 0L);
                    vo.setArticleCount((int) count);
                    return vo;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TagVO> getHotTags(Integer limit) {
        List<TagVO> allTags = getTagList();
        
        return allTags.stream()
                .sorted(Comparator.comparingInt(TagVO::getArticleCount).reversed())
                .limit(limit != null && limit > 0 ? limit : 10)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public TagVO getTagById(Long id) {
        Tag tag = tagMapper.selectOneById(id);
        if (tag == null) {
            return null;
        }
        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(tag.getTagName());
        vo.setColor(tag.getColor());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, timeout = 30)
    public TagVO createTag(String name, String color) {
        Tag tag = new Tag();
        tag.setTagName(name);
        tag.setColor(color);
        tagMapper.insert(tag);

        TagVO vo = new TagVO();
        vo.setId(tag.getId());
        vo.setName(name);
        vo.setColor(color);
        log.info("创建标签成功: {}", name);
        return vo;
    }

    private int countArticlesByTagId(Long tagId) {
        try {
            QueryWrapper wrapper = QueryWrapper.create()
                .from("t_article_tag")
                .where("tag_id = ?", tagId);
            return (int) articleTagMapper.selectCountByQuery(wrapper);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void deleteTag(Long id) {
        Tag tag = tagMapper.selectOneById(id);
        if (tag == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "标签不存在");
        }
        tagMapper.deleteById(id);
    }
}
