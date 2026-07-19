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
import java.util.UUID;

/**
 * JWT 工具类
 * 负责 Access Token / Refresh Token 的签发、解析与校验
 */
@Component
public class JwtUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    @Value("${ieps.jwt.secret}")
    private String jwtSecret;
    
    @Value("${ieps.jwt.access-token-expiration:600000}")
    private long accessTokenExpiration;
    
    @Value("${ieps.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpiration;
    
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
     * 生成 Access Token（短时效，5-10 分钟）
     * 携带用户身份信息，用于 API 鉴权
     */
    public String generateAccessToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + accessTokenExpiration);
        
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
     * 生成 Refresh Token（长效，7 天）
     * 仅含用户编号和唯一 jti，不出现在 API 鉴权流程中，仅用于换取新的 Access Token
     * @return { token, jti } 二元组
     */
    public String[] generateRefreshToken(User user) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + refreshTokenExpiration);
        String jti = UUID.randomUUID().toString().replace("-", "");
        
        String token = Jwts.builder()
                .subject(user.getUserNum())
                .claim("userNum", user.getUserNum())
                .claim("jti", jti)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
        
        return new String[]{token, jti};
    }

    /**
     * 兼容旧调用：等价于 generateAccessToken
     */
    public String generateToken(User user) {
        return generateAccessToken(user);
    }
    
    /**     * 校验 Refresh Token（检查签名、类型和过期时间）
     *
     * @param token Refresh Token 字符串
     * @return true 有效，false 无效
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            
            // 确保是 refresh 类型
            return "refresh".equals(claims.get("type", String.class));
        } catch (ExpiredJwtException e) {
            logger.warn("Refresh Token 已过期: {}", e.getMessage());
        } catch (SecurityException | MalformedJwtException e) {
            logger.warn("Refresh Token 签名无效或格式错误: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的 Refresh Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.warn("Refresh Token 字符串为空: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 从 Refresh Token 中提取 jti
     */
    public String getJtiFromRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("jti", String.class);
        } catch (Exception e) {
            logger.warn("从 Refresh Token 提取 jti 失败: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 Token（access 或 refresh）中提取主题（userNum）
     */
    public String getSubject(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getSubject();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**     * 从 Token 中提取 Claims
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
