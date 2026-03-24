package com.xingchen.backend.constant;

public class CacheKey {

    public static final String ARTICLE_PREFIX = "article:";
    public static final String ARTICLE_DETAIL = "article:%s";
    public static final String ARTICLE_LIST = "article:list:%s";
    public static final String ARTICLE_USER = "article:user:%s";
    public static final String ARTICLE_HOT = "article:hot:%s";

    public static final String USER_PREFIX = "user:";
    public static final String USER_ARTICLES = "user:articles:%s";
    public static final String USER_COLLECTS = "user:collects:%s";

    public static final String ANALYTICS_PREFIX = "analytics:";
    public static final String ANALYTICS_PV = "analytics:pv:";
    public static final String ANALYTICS_UV = "analytics:uv:";
    public static final String ANALYTICS_IP = "analytics:ip:";
    public static final String ANALYTICS_DEDUP = "analytics:dedup:";

    public static final String KB_PREFIX = "kb:";
    public static final String KB_DOC = "kb:doc:";
    public static final String KB_INDEX = "kb:index";

    public static final String AI_PREFIX = "ai:";
    public static final String AI_SESSION = "ai:chat:session:";

    public static final String VERIFICATION_PREFIX = "verification:";
    public static final String VERIFICATION_CODE = "verification:code:";
    public static final String VERIFICATION_SENDTIME = "verification:sendtime:";

    public static final String SA_TOKEN_PREFIX = "satoken:";
}
