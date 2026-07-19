package com.ieps.config;

import com.ieps.common.Const;
import com.ieps.pojo.User;
import com.ieps.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Swagger / SpringDoc 文档页面访问控制拦截器
 * - localhost(127.0.0.1 / ::1) 直接放行
 * - 非本地 IP 需携带有效 JWT 且角色为学院管理员及以上 (roleId >= 200004)
 */
@Component
public class DocsAccessInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(DocsAccessInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String remoteAddr = request.getRemoteAddr();

        // localhost 直接放行
        if ("127.0.0.1".equals(remoteAddr) || "0:0:0:0:0:0:0:1".equals(remoteAddr)) {
            return true;
        }

        // 非本地 IP：检查 JWT 和管理员角色
        String token = extractToken(request);
        if (token == null) {
            logger.warn("Docs access denied from {}: no token provided", remoteAddr);
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"msg\":\"API 文档仅限管理员访问，请先登录管理员账号\"}");
            return false;
        }

        User user = jwtUtil.getUserFromToken(token);
        if (user == null) {
            logger.warn("Docs access denied from {}: invalid token", remoteAddr);
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"msg\":\"登录已过期，请重新登录管理员账号\"}");
            return false;
        }

        if (user.getRoleId() < Const.ROLEID_ACADEMY) {
            logger.warn("Docs access denied from {}: user {} roleId {} is not admin",
                    remoteAddr, user.getUserNum(), user.getRoleId());
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":403,\"msg\":\"API 文档仅限管理员访问\"}");
            return false;
        }

        // 管理员通过，将用户信息写入请求属性供后续使用
        request.setAttribute(Const.REQUEST_CURRENT_USER, user);
        return true;
    }

    /**
     * 从请求中提取 JWT token
     * 优先从 Authorization 头读取，也支持 ?token=xxx 查询参数
     * （用于浏览器直接访问 /docs 页面时，非 AJAX 请求无法自动携带 Authorization 头）
     */
    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return request.getParameter("token");
    }
}
