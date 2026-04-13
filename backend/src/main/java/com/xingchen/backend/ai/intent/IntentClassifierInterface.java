package com.xingchen.backend.ai.intent;

import com.xingchen.backend.ai.model.Intent;

/**
 * 意图分类器接口
 * 定义所有意图分类器应该实现的方法
 */
public interface IntentClassifierInterface {

    /**
     * 分类用户意图
     * @param message 用户输入消息
     * @return 意图对象
     */
    Intent classify(String message);

    /**
     * 检查分类器是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取分类器名称
     * @return 分类器名称
     */
    String getName();
}
