# MyBatis-Flex 使用指南

## 1. 仓库概览

MyBatis-Flex 是一个优雅的 MyBatis 增强框架，具有以下特点：

- **轻量级**：仅依赖 MyBatis，无其他第三方依赖
- **基础 CRUD**：支持实体类的基本增删改查和分页查询
- **Row 映射**：无需实体类也可进行数据库操作
- **多数据库支持**：通过方言灵活扩展
- **复合主键支持**：支持不同主键内容生成策略
- **友好的 SQL 查询**：IDE 自动提示，减少错误
- **更多惊喜功能**

## 2. 环境配置

### 2.1 无 Spring 环境配置

**步骤 1**：添加依赖（假设使用 Maven）
```xml
<!-- MyBatis-Flex 核心依赖 -->
<dependency>
    <groupId>io.mybatis</groupId>
    <artifactId>mybatis-flex-core</artifactId>
    <version>最新版本</version>
</dependency>

<!-- MyBatis 依赖 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.10</version>
</dependency>

<!-- 数据库驱动和连接池依赖 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>4.0.3</version>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.28</version>
</dependency>
```

**步骤 2**：配置数据源
```java
HikariDataSource dataSource = new HikariDataSource();
dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/mybatis-flex");
dataSource.setUsername("username");
dataSource.setPassword("password");
```

**步骤 3**：初始化 MyBatis-Flex
```java
MybatisFlexBootstrap.getInstance()
        .setDataSource(dataSource)
        .addMapper(AccountMapper.class)
        .start();
```

### 2.2 Spring 环境配置

**步骤 1**：添加依赖
```xml
<!-- MyBatis-Flex Spring 集成 -->
<dependency>
    <groupId>io.mybatis</groupId>
    <artifactId>mybatis-flex-spring</artifactId>
    <version>最新版本</version>
</dependency>

<!-- Spring 依赖 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-context</artifactId>
    <version>5.3.20</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>5.3.20</version>
</dependency>
```

**步骤 2**：配置 Spring XML
```xml
<!-- 数据源配置 -->
<bean id="dataSource" class="com.zaxxer.hikari.HikariDataSource">
    <property name="jdbcUrl" value="jdbc:mysql://127.0.0.1:3306/mybatis-flex"/>
    <property name="username" value="username"/>
    <property name="password" value="password"/>
</bean>

<!-- SqlSessionFactory 配置 -->
<bean id="sqlSessionFactory" class="io.mybatis.flex.spring.MyBatisFlexSqlSessionFactoryBean">
    <property name="dataSource" ref="dataSource"/>
</bean>

<!-- Mapper 扫描配置 -->
<bean class="io.mybatis.flex.spring.MapperScannerConfigurer">
    <property name="basePackage" value="com.example.mapper"/>
</bean>
```

### 2.3 Spring Boot 环境配置

**步骤 1**：添加依赖
```xml
<!-- MyBatis-Flex Spring Boot Starter -->
<dependency>
    <groupId>io.mybatis</groupId>
    <artifactId>mybatis-flex-spring-boot-starter</artifactId>
    <version>最新版本</version>
</dependency>

<!-- 数据库驱动和连接池依赖 -->
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

**步骤 2**：配置 application.yml
```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/mybatis-flex
    username: username
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

mybatis-flex:
  mapper-locations: classpath:mappers/**/*.xml
```

## 3. 核心 API 调用

### 3.1 MybatisFlexBootstrap

**使用方法**：
```java
// 初始化
MybatisFlexBootstrap.getInstance()
        .setDataSource(dataSource)
        .addMapper(AccountMapper.class)
        .start();

// 获取 Mapper
AccountMapper mapper = MybatisFlexBootstrap.getInstance()
        .getMapper(AccountMapper.class);
```

### 3.2 BaseMapper 接口

**核心方法**：

| 方法名 | 描述 |
|-------|------|
| `selectOneById(id)` | 根据主键查询单个实体 |
| `selectList(queryWrapper)` | 根据条件查询实体列表 |
| `selectCount(queryWrapper)` | 根据条件查询记录数 |
| `paginate(pageNumber, pageSize, queryWrapper)` | 分页查询 |
| `insert(entity)` | 插入实体 |
| `update(entity)` | 更新实体 |
| `deleteById(id)` | 根据主键删除 |
| `deleteByQuery(queryWrapper)` | 根据条件删除 |

**示例**：
```java
// 根据主键查询
Account account = mapper.selectOneById(100);

// 分页查询
Page<Account> accountPage = mapper.paginate(5, 10, queryWrapper);
```

## 4. 实体类映射

### 4.1 注解使用

**@Table 注解**：
```java
@Table("tb_account")
public class Account {
    // 字段定义
}
```

**@Id 注解**：
```java
@Id(keyType = KeyType.Auto)
private Long id;
```

**KeyType 枚举**：
- `Auto`：自动增长
- `None`：无主键
- `Assign`：手动赋值
- `Sequence`：序列
- `Uuid`：UUID

### 4.2 实体类生成

**方法 1**：通过 IDE 构建项目
**方法 2**：执行 Maven 构建命令
```bash
mvn clean package
```

**注意**：构建后会自动生成用于 QueryWrapper 的 `ACCOUNT` 类。

## 5. 查询构建

### 5.1 基本查询

**查询所有字段**：
```java
QueryWrapper query = new QueryWrapper();
query.select().from(ACCOUNT);
// SQL: SELECT * FROM tb_account
```

**查询指定字段**：
```java
QueryWrapper query = new QueryWrapper();
query.select(ACCOUNT.ID, ACCOUNT.USER_NAME).from(ACCOUNT);
// SQL: SELECT tb_account.id, tb_account.user_name FROM tb_account
```

### 5.2 条件查询

**WHERE 条件**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .where(ACCOUNT.ID.ge(100))
    .and(ACCOUNT.USER_NAME.like("michael"));
// SQL: SELECT * FROM tb_account WHERE tb_account.id >= ? AND tb_account.user_name LIKE ?
```

**子查询**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .where(ACCOUNT.ID.ge(100))
    .and(
        exists(
            selectOne().from(ARTICLE).where(ARTICLE.ID.ge(100))
        )
    );
// SQL: SELECT * FROM tb_account WHERE tb_account.id >= ? AND EXIST (SELECT 1 FROM tb_article WHERE tb_article.id >= ?)
```

### 5.3 复杂条件

**AND/OR 组合**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .where(ACCOUNT.ID.ge(100))
    .and(ACCOUNT.SEX.eq(1).or(ACCOUNT.SEX.eq(2)))
    .or(ACCOUNT.AGE.in(18, 19, 20).or(ACCOUNT.USER_NAME.like("michael")));
// SQL: SELECT * FROM tb_account WHERE tb_account.id >= ? AND (tb_account.sex = ? OR tb_account.sex = ?) OR (tb_account.age IN (?,?,?) OR tb_account.user_name LIKE ?)
```

### 5.4 分组查询

**GROUP BY**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .groupBy(ACCOUNT.USER_NAME);
// SQL: SELECT * FROM tb_account GROUP BY tb_account.user_name
```

**HAVING**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .groupBy(ACCOUNT.USER_NAME)
    .having(ACCOUNT.AGE.between(18, 25));
// SQL: SELECT * FROM tb_account GROUP BY tb_account.user_name HAVING tb_account.age BETWEEN ? AND ?
```

### 5.5 排序

**ORDER BY**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
        .select()
        .from(ACCOUNT)
        .orderBy(ACCOUNT.AGE.asc(), ACCOUNT.USER_NAME.desc().nullsLast());
// SQL: SELECT * FROM tb_account ORDER BY age ASC, user_name DESC NULLS LAST
```

### 5.6 连接查询

**LEFT JOIN**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .leftJoin(ARTICLE).on(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID))
    .where(ACCOUNT.AGE.ge(10));
// SQL: SELECT * FROM tb_account LEFT JOIN tb_article ON tb_account.id = tb_article.account_id WHERE tb_account.age >= ?
```

### 5.7 分页查询

**LIMIT/OFFSET**：
```java
QueryWrapper queryWrapper = QueryWrapper.create()
    .select()
    .from(ACCOUNT)
    .orderBy(ACCOUNT.ID.desc())
    .limit(10)
    .offset(20);

// MySQL: SELECT * FROM `tb_account` ORDER BY `id` DESC LIMIT 20, 10
// PostgreSQL: SELECT * FROM "tb_account" ORDER BY "id" DESC LIMIT 20 OFFSET 10
// Oracle: 自动转换为 ROWNUM 分页
```

## 6. 事务管理

### 6.1 无 Spring 环境

**使用 MyBatis 原生事务**：
```java
SqlSession sqlSession = MybatisFlexBootstrap.getInstance().getSqlSessionFactory().openSession(false);
try {
    AccountMapper mapper = sqlSession.getMapper(AccountMapper.class);
    // 执行数据库操作
    mapper.insert(account);
    mapper.update(anotherAccount);
    // 提交事务
    sqlSession.commit();
} catch (Exception e) {
    // 回滚事务
    sqlSession.rollback();
    throw e;
} finally {
    sqlSession.close();
}
```

### 6.2 Spring 环境

**使用 @Transactional 注解**：
```java
@Transactional
public void transferMoney(long fromId, long toId, BigDecimal amount) {
    // 执行转账操作
    accountService.decreaseBalance(fromId, amount);
    accountService.increaseBalance(toId, amount);
}
```

## 7. 性能优化建议

1. **合理使用索引**：在查询条件字段上创建索引
2. **减少查询字段**：只查询必要的字段
3. **批量操作**：使用批量插入/更新减少数据库交互
4. **缓存使用**：合理使用 MyBatis 二级缓存
5. **分页优化**：避免大偏移量查询
6. **预编译语句**：利用 MyBatis 的预编译功能
7. **连接池配置**：根据应用场景调整连接池参数

## 8. 常见错误处理

1. **SQL 语法错误**：检查 QueryWrapper 构建的条件是否正确
2. **类型转换错误**：确保实体类字段类型与数据库字段类型匹配
3. **主键冲突**：使用合适的主键生成策略
4. **事务回滚**：确保事务边界正确设置
5. **连接超时**：检查数据库连接配置
6. **NPE 错误**：处理可能为 null 的字段

## 9. 版本兼容性问题

1. **数据库兼容性**：
   - MySQL：完全支持
   - PostgreSQL：完全支持
   - Oracle：需要注意分页语法差异
   - SQL Server：需要注意 TOP 语法差异
   - DB2：需要注意 OFFSET 语法差异

2. **MyBatis 版本**：
   - 推荐使用 MyBatis 3.5.10 及以上版本
   - 不支持 MyBatis 3.4.x 及以下版本

3. **Java 版本**：
   - 支持 Java 8 及以上版本
   - 推荐使用 Java 11+ 以获得更好的性能

## 10. 最佳实践

1. **使用 QueryWrapper**：
   - 利用 IDE 自动提示功能
   - 避免手写 SQL 错误
   - 提高代码可读性

2. **实体类设计**：
   - 使用 Lombok 简化代码
   - 合理设置字段类型
   - 遵循 Java 命名规范

3. **Mapper 接口**：
   - 仅定义接口，无需实现类
   - 合理分组相关操作

4. **分页查询**：
   - 使用 `paginate` 方法
   - 避免手动计算偏移量

5. **条件构建**：
   - 复杂条件使用括号分组
   - 合理使用子查询

## 11. 潜在陷阱

1. **SQL 注入**：
   - 避免直接拼接 SQL
   - 使用 QueryWrapper 的参数化查询

2. **性能问题**：
   - 避免在循环中执行数据库操作
   - 合理使用批量操作

3. **事务陷阱**：
   - 避免长事务
   - 确保事务边界正确

4. **内存溢出**：
   - 避免一次性查询大量数据
   - 合理使用分页

5. **并发问题**：
   - 注意乐观锁和悲观锁的使用
   - 合理设置事务隔离级别

## 12. 更多示例

### 12.1 函数查询

```java
QueryWrapper query = new QueryWrapper()
        .select(
            ACCOUNT.ID,
            ACCOUNT.USER_NAME,
            max(ACCOUNT.BIRTHDAY),
            avg(ACCOUNT.SEX).as("sex_avg")
        ).from(ACCOUNT);

// SQL: SELECT tb_account.id, tb_account.user_name, MAX(tb_account.birthday), AVG(tb_account.sex) AS sex_avg FROM tb_account
```

### 12.2 别名查询

```java
QueryWrapper query = new QueryWrapper()
    .select(
        ACCOUNT.ID,
        ACCOUNT.USER_NAME,
        ARTICLE.ID.as("articleId"),
        ARTICLE.TITLE
    )
    .from(ACCOUNT.as("a"), ARTICLE.as("b"))
    .where(ACCOUNT.ID.eq(ARTICLE.ACCOUNT_ID));

// SQL: SELECT a.id, a.user_name, b.id AS articleId, b.title FROM tb_account AS a, tb_article AS b WHERE a.id = b.account_id
```

## 13. 总结

MyBatis-Flex 是一个功能强大且易用的 MyBatis 增强框架，通过提供简洁的 API 和丰富的功能，大大简化了数据库操作的开发工作。使用 MyBatis-Flex，开发者可以：

- 快速实现数据库操作
- 减少手写 SQL 的错误
- 提高代码可读性和可维护性
- 适配多种数据库环境
- 享受优雅的开发体验

通过本指南的学习，相信您已经对 MyBatis-Flex 有了全面的了解，可以在实际项目中灵活运用它来提高开发效率。