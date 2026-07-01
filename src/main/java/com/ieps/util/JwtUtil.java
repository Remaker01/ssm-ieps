package com.ieps.util;

import com.ieps.pojo.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类
 * 负责 Token 的签发、解析与校验
 */
@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    /**
     * JWT 密钥（从配置文件中读取）
     */
    @Value("${ieps.jwt.secret}")
    private String jwtSecret;
    
    /**
     * JWT 过期时间（毫秒），默认 30 分钟
     */
    @Value("${ieps.jwt.expiration:1800000}")
    private long jwtExpiration;
    
    /**
     * 构建签名密钥
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        // 确保密钥至少 256 位（32 字节）
        if (keyBytes.length < 32) {
            byte[] paddedKey = new byte[32];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
            return Keys.hmacShaKeyFor(paddedKey);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * 生成 JWT Token
     *
     * @param user 用户信息（包含 userNum、userStatus、roleId）
     * @return JWT token 字符串
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtExpiration);
        
        return Jwts.builder()
                .subject(user.getUserNum())
                .claim("userNum", user.getUserNum())
                .claim("userStatus", user.getUserStatus())
                .claim("roleId", user.getRoleId())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }
    
    /**
     * 从 Token 中提取 Claims
     *
     * @param token JWT token
     * @return Claims，若解析失败返回 null
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            logger.warn("JWT Token 已过期: {}", e.getMessage());
        } catch (SecurityException | MalformedJwtException e) {
            logger.warn("JWT Token 签名无效或格式错误: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("JWT Token 字符串为空: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 校验 Token 是否有效
     *
     * @param token JWT token
     * @return true 有效，false 无效
     */
    public boolean validateToken(String token) {
        return parseToken(token) != null;
    }
    
    /**
     * 从 Token 中还原 User 对象
     *
     * @param token JWT token
     * @return User 对象，无效则返回 null
     */
    public User getUserFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        
        User user = new User();
        user.setUserNum(claims.get("userNum", String.class));
        user.setUserStatus(claims.get("userStatus", Integer.class));
        user.setRoleId(claims.get("roleId", Integer.class));
        return user;
    }
}
