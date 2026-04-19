package com.xingchen.backend.service;

import com.xingchen.backend.vo.TagVO;

import java.util.List;

public interface TagService {
    List<TagVO> getTagList();

    List<TagVO> getUserTagList(Long userId);

    List<TagVO> getHotTags(Integer limit);

    TagVO getTagById(Long id);

    TagVO createTag(String name, String color);

    TagVO updateTag(Long id, String name, String color);

    void deleteTag(Long id);
}
