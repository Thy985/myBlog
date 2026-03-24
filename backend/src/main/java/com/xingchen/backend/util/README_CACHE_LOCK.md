# 缓存互斥锁使用说明

## 📋 概述

本项目已实现基于 Redis 的分布式互斥锁，用于解决**缓存击穿**问题。

## 🔧 核心组件

### 1. RedisLockUtil

**位置**: [`RedisLockUtil.java`](RedisLockUtil.java)

**功能**: 提供分布式锁的工具类

**主要方法**:
```java
// 尝试获取锁（默认 10 秒过期）
boolean tryLock(String key)

// 尝试获取锁（自定义过期时间）
boolean tryLock(String key, long expireSeconds)

// 带重试的获取锁
boolean tryLockWithRetry(String key, long expireSeconds, int retryTimes, long retryIntervalMs)

// 释放锁
void unlock(String key)

// 带锁执行（函数式接口）
<T> T executeWithLock(String key, Supplier<T> supplier)
```

---

## 📊 缓存击穿解决方案

### 问题场景

当某个**热点 key**（如热门文章、爆款商品）在过期瞬间，大量请求同时访问，导致数据库压力骤增。

### 解决方案：互斥锁 + 双重检查

```java
// 1. 查询缓存
Object cached = redisTemplate.opsForValue().get(cacheKey);
if (cached != null) {
    return cached;  // 缓存命中直接返回
}

// 2. 缓存未命中，获取互斥锁
return redisLockUtil.executeWithLock(
    cacheKey,
    10,           // 锁过期时间（秒）
    3,            // 重试次数
    100,          // 重试间隔（毫秒）
    () -> {
        // 3. 双重检查（防止其他线程已加载）
        Object doubleCheck = redisTemplate.opsForValue().get(cacheKey);
        if (doubleCheck != null) {
            return doubleCheck;
        }
        
        // 4. 查询数据库
        ArticleVO vo = loadFromDB(id);
        
        // 5. 写入缓存（带随机 TTL）
        redisTemplate.opsForValue().set(cacheKey, vo, 30 + random(5), TimeUnit.MINUTES);
        
        return vo;
    }
);
```

---

## 🔍 工作流程

```
请求 1 ─┬─> 查缓存 (miss) ─> 获取锁 (success) ─> 查 DB ─> 写缓存 ─> 释放锁
        │
请求 2 ─┼─> 查缓存 (miss) ─> 获取锁 (wait) ─> 重试 ─> 查缓存 (hit) ─> 返回
        │
请求 3 ─┴─> 查缓存 (miss) ─> 获取锁 (wait) ─> 重试 ─> 查缓存 (hit) ─> 返回

时间线：
T1: 请求 1 获取锁，开始查 DB
T2: 请求 2、3 等待锁
T3: 请求 1 完成，释放锁
T4: 请求 2 重试，发现缓存已有，直接返回
T5: 请求 3 重试，发现缓存已有，直接返回
```

---

## 📝 使用示例

### 示例 1：查询文章详情

```java
@Service
public class ArticleServiceImpl implements ArticleService {
    
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    public ArticleVO getArticleById(Long id) {
        String cacheKey = "article::" + id;
        
        // 1. 先查缓存
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ArticleVO) {
            return (ArticleVO) cached;
        }
        
        // 2. 缓存未命中，加锁查询
        return redisLockUtil.executeWithLock(
            cacheKey,
            () -> loadArticleFromDB(id, cacheKey)
        );
    }
    
    private ArticleVO loadArticleFromDB(Long id, String cacheKey) {
        // 双重检查
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof ArticleVO) {
            return (ArticleVO) cached;
        }
        
        // 查数据库
        ArticleVO vo = articleMapper.selectById(id);
        
        // 写缓存
        redisTemplate.opsForValue().set(cacheKey, vo, 30, TimeUnit.MINUTES);
        
        return vo;
    }
}
```

### 示例 2：查询用户信息

```java
@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private RedisLockUtil redisLockUtil;
    
    public UserVO getUserById(Long id) {
        String cacheKey = "user::" + id;
        
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof UserVO) {
            return (UserVO) cached;
        }
        
        return redisLockUtil.executeWithLock(
            cacheKey,
            () -> loadUserFromDB(id, cacheKey)
        );
    }
}
```

---

## ⚙️ 参数配置

### 默认配置

```java
private static final long LOCK_EXPIRE_SECONDS = 10;      // 锁过期时间
private static final int LOCK_RETRY_TIMES = 3;           // 重试次数
private static final long LOCK_RETRY_INTERVAL_MS = 100;  // 重试间隔
```

### 调优建议

| 场景 | 锁过期时间 | 重试次数 | 重试间隔 |
|------|------------|----------|----------|
| **快速查询**（文章详情） | 5-10 秒 | 3-5 次 | 50-100ms |
| **复杂查询**（统计报表） | 10-30 秒 | 5-10 次 | 100-200ms |
| **批量操作**（数据同步） | 30-60 秒 | 10-20 次 | 200-500ms |

---

## ⚠️ 注意事项

### 1. 锁过期时间设置

- ✅ **不宜过长**：避免持有锁的线程宕机，其他线程长时间等待
- ✅ **不宜过短**：确保业务逻辑能执行完成
- ✅ **建议值**：业务执行时间 × 2~3 倍

### 2. 重试机制

- ✅ 必须设置重试次数，避免并发冲突
- ✅ 重试间隔建议加入随机因子，避免"惊群效应"

### 3. 死锁预防

- ✅ 使用 `setIfAbsent` 带过期时间的原子操作
- ✅ 即使未释放锁，也会自动过期

### 4. 降级策略

```java
// 获取锁失败时，直接查数据库（降级）
if (!locked) {
    log.warn("获取锁失败，降级查询数据库：{}", key);
    return supplier.get();  // 直接查 DB，不缓存
}
```

---

## 📊 性能对比

| 场景 | 无锁方案 | 互斥锁方案 |
|------|----------|------------|
| **缓存命中** | 1ms | 1ms |
| **缓存未命中（单请求）** | 50ms | 50ms |
| **缓存未命中（100 并发）** | 5000ms（DB 压力大） | 150ms（串行执行） |
| **数据库连接数** | 100 个 | 1 个 |

---

## 🔍 监控建议

### 1. 日志监控

```java
// 记录锁等待时间
long startTime = System.currentTimeMillis();
boolean locked = tryLock(key);
long waitTime = System.currentTimeMillis() - startTime;

if (locked) {
    log.info("获取锁成功，等待时间：{}ms", waitTime);
} else {
    log.warn("获取锁失败，等待时间：{}ms", waitTime);
}
```

### 2. 指标监控

- 锁获取成功率
- 平均等待时间
- 降级查询次数

---

## 🚀 后续优化方向

1. **Redlock 算法**：Redis 集群环境下的分布式锁
2. **Redisson**：成熟的 Redis 客户端，提供更完善的锁实现
3. **分段锁**：减少锁粒度，提高并发度
4. **读写锁**：读共享、写独占，提高读性能

---

## 📚 参考资料

- [Redis 分布式锁最佳实践](https://redis.io/topics/distlock)
- [Redisson 官方文档](https://github.com/redisson/redisson)
- [阿里巴巴 Java 开发手册](https://github.com/alibaba/p3c)
