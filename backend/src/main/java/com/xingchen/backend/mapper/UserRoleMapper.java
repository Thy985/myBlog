package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.UserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    @Delete("DELETE FROM t_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    @Select("SELECT r.role_code FROM t_role r INNER JOIN t_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /**
     * 批量查询多个用户的角色编码（优化N+1查询）
     */
    @Select("<script>" +
            "SELECT ur.user_id, r.role_code FROM t_role r " +
            "INNER JOIN t_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id IN " +
            "<foreach collection='userIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Map<String, Object>> selectRoleCodesByUserIdsForMap(@Param("userIds") List<Long> userIds);

    /**
     * 批量查询用户角色编码Map（优化N+1查询）
     * 返回：userId -> List<roleCode>
     */
    default Map<Long, List<String>> selectRoleCodesByUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }

        List<Map<String, Object>> results = selectRoleCodesByUserIdsForMap(userIds);
        Map<Long, List<String>> userRolesMap = new HashMap<>();

        for (Map<String, Object> result : results) {
            Long userId = (Long) result.get("user_id");
            String roleCode = (String) result.get("role_code");

            userRolesMap.computeIfAbsent(userId, k -> new ArrayList<>()).add(roleCode);
        }

        // 为没有角色的用户设置空列表
        for (Long userId : userIds) {
            userRolesMap.putIfAbsent(userId, new ArrayList<>());
        }

        return userRolesMap;
    }
}
