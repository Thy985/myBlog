package com.xingchen.backend.mapper;

import com.mybatisflex.core.BaseMapper;
import com.xingchen.backend.entity.PasswordResetToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface PasswordResetTokenMapper extends BaseMapper<PasswordResetToken> {
    
    @Select("SELECT * FROM t_password_reset_token WHERE email = #{email} AND code = #{code} AND used = 0 AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1")
    PasswordResetToken selectValidToken(@Param("email") String email, @Param("code") String code);
    
    @Select("SELECT * FROM t_password_reset_token WHERE token = #{token} AND used = 0 AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1")
    PasswordResetToken selectValidTokenByToken(@Param("token") String token);
    
    @Update("UPDATE t_password_reset_token SET used = 1 WHERE id = #{id}")
    int markAsUsed(@Param("id") Long id);
    
    @Update("UPDATE t_password_reset_token SET used = 1 WHERE email = #{email}")
    int invalidateByEmail(@Param("email") String email);
}
