package com.ieps.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();

        // 权限和菜单缓存 — 60 分钟
        CaffeineCache permCache = buildCache("ieps-perm", 1, TimeUnit.HOURS);
        // 角色权限映射缓存 — 30 分钟
        CaffeineCache rolePermCache = buildCache("ieps-role-perm", 30, TimeUnit.MINUTES);
        // 角色列表缓存 — 60 分钟
        CaffeineCache roleCache = buildCache("ieps-role", 1, TimeUnit.HOURS);
        // 项目选项枚举缓存 — 永不过期（静态数据）
        CaffeineCache itemOptionsCache = buildCache("ieps-item-options", 0, null);
        // 用户资料缓存 — 30 分钟
        CaffeineCache userInfoCache = buildCache("ieps-user-info", 30, TimeUnit.MINUTES);
        // 通知公告缓存 — 10 分钟
        CaffeineCache informCache = buildCache("ieps-inform", 10, TimeUnit.MINUTES);

        manager.setCaches(List.of(
                permCache, rolePermCache, roleCache,
                itemOptionsCache, userInfoCache, informCache
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit) {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .maximumSize(500);
        if (unit != null && duration > 0) {
            builder.expireAfterWrite(duration, unit);
        }
        return new CaffeineCache(name, builder.build());
    }
}
