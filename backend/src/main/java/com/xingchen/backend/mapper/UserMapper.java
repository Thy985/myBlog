package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("SELECT * FROM t_user WHERE username = #{username} AND is_deleted = 0")
    User selectByUsername(@Param("username") String username);

    @Select("SELECT * FROM t_user WHERE email = #{email} AND is_deleted = 0")
    User selectByEmail(@Param("email") String email);

    @Select("SELECT * FROM t_user WHERE phone = #{phone} AND is_deleted = 0")
    User selectByPhone(@Param("phone") String phone);

    @Select("SELECT * FROM t_user WHERE is_deleted = 0 ORDER BY created_time DESC")
    List<User> selectAll();

    @Select("<script>SELECT * FROM t_user WHERE id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> AND is_deleted = 0</script>")
    List<User> selectByIds(@Param("ids") List<Long> ids);

    @Select("SELECT * FROM t_user WHERE is_deleted = 0 AND status = 1 ORDER BY login_count DESC LIMIT #{limit}")
    List<User> selectActiveUsers(@Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM t_user WHERE is_deleted = 0")
    long countUsers();

    @Update("UPDATE t_user SET login_count = login_count + 1, last_login_time = NOW(), last_login_ip = #{ip} WHERE id = #{id}")
    int updateLoginInfo(@Param("id") Long id, @Param("ip") String ip);

    @Update("UPDATE t_user SET password = #{password}, password_modify_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT COUNT(*) FROM t_user WHERE created_time >= #{start} AND created_time < #{end} AND is_deleted = 0")
    int countByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("SELECT COUNT(*) FROM t_user WHERE DATE(created_time) = #{date} AND is_deleted = 0")
    Long countNewUsersToday(@Param("date") LocalDate date);

    @Select("SELECT u.id, u.username, u.email, u.avatar, COUNT(a.id) as articleCount " +
            "FROM t_user u " +
            "LEFT JOIN t_article a ON u.id = a.user_id AND a.is_deleted = 0 " +
            "WHERE u.is_deleted = 0 " +
            "GROUP BY u.id, u.username, u.email, u.avatar " +
            "ORDER BY articleCount DESC, u.login_count DESC " +
            "LIMIT #{limit}")
    List<Map<String, Object>> selectActiveUsersWithArticleCount(@Param("limit") Integer limit);

    @Select("SELECT r.name as roleName, COUNT(ur.user_id) as count " +
            "FROM t_role r " +
            "LEFT JOIN t_user_role ur ON r.id = ur.role_id " +
            "LEFT JOIN t_user u ON ur.user_id = u.id AND u.is_deleted = 0 " +
            "GROUP BY r.id, r.name " +
            "ORDER BY count DESC")
    List<Map<String, Object>> selectUserRoleDistribution();

    @Select("SELECT COUNT(*) FROM t_user WHERE is_deleted = 0")
    long countAll();

    @Select("<script>" +
            "SELECT * FROM t_user WHERE is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "ORDER BY created_time DESC " +
            "LIMIT #{offset}, #{size}" +
            "</script>")
    List<User> selectByPage(@Param("keyword") String keyword, @Param("status") String status, 
                            @Param("offset") int offset, @Param("size") int size);

    @Select("<script>" +
            "SELECT COUNT(*) FROM t_user WHERE is_deleted = 0 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "AND (username LIKE CONCAT('%', #{keyword}, '%') OR email LIKE CONCAT('%', #{keyword}, '%') OR nickname LIKE CONCAT('%', #{keyword}, '%')) " +
            "</if>" +
            "<if test='status != null and status != \"\"'>" +
            "AND status = #{status} " +
            "</if>" +
            "</script>")
    long countByCondition(@Param("keyword") String keyword, @Param("status") String status);
}
