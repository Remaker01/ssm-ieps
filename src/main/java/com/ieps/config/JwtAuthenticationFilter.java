package com.ieps.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ieps.common.Const;
import com.ieps.common.ServerResponse;
import com.ieps.pojo.User;
import com.ieps.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器
 * 从 Authorization 头提取 Token 并校验，通过后将用户信息设置到请求属性中
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 白名单路径——不需要认证即可访问
     */
    private final List<String> whiteList = List.of(
        // 公开页面路由
        "/", "/home", "/login", "/register", "/logout", "/forget-password",
        "/informs/**", "/items", "/items/detail", "/downloads",

        // 兼容旧 .do 路由
        "/goHome.do", "/login.do", "/register.do", "/logout.do",
        "/goLogin.do", "/goRegister.do", "/goForgetPwd.do",
        "/goShowInformDetail.do", "/goShowInfoemDetail.do", "/goShowInformMore.do",
        "/goShowItemMore.do", "/goShowItemDetail.do", "/goShowDownLoadMore.do",

        // 验证码相关
        "/getVerifyCode", "/getVerifyCode.do",
        "/checkVerifyCode", "/checkVerifyCode.do",
        "/checkVerifyNum", "/checkVerifyNum.do",

        // 公共 AJAX 数据接口（无需登录）
        "/checkUser", "/checkUser.do",
        "/getInformDetailById", "/getInformDetailById.do",
        "/getItemDetailWithItemNum", "/getItemDetailWithItemNum.do",
        "/getUserInfoWithItemNum", "/getUserInfoWithItemNum.do",
        "/getFinishedItemList", "/getFinishedItemList.do",
        "/getInformListByAdminWithUserNum", "/getInformListByAdminWithUserNum.do",
        "/getFileListByAdminWithKind", "/getFileListByAdminWithKind.do",
        "/previewFile", "/previewFile.do",
        "/downloadFile", "/downloadFile.do",
        "/downloadFileWithItemNum", "/downloadFileWithItemNum.do",

        // 静态资源
        "/static/**", "/pages/**", "/upload/**", "/hub/**",
        "/favicon.ico", "/error",
        "/**/*.html", "/**/*.css", "/**/*.js",
        "/**/*.png", "/**/*.jpg", "/**/*.jpeg", "/**/*.gif", "/**/*.ico",
        "/**/*.woff", "/**/*.woff2", "/**/*.ttf", "/**/*.svg", "/**/*.map"
    );

    private final PathMatcher pathMatcher = new AntPathMatcher();
    
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        
        // 检查是否在白名单中
        for (String pattern : whiteList) {
            if (pathMatcher.match(pattern, requestURI)) {
                filterChain.doFilter(request, response);
                return;
            }
        }
        
        // GET 请求的页面路由（白名单外的无后缀路径视为页面导航，放行）
        // 注意：API/数据接口应在此之后才走 token 校验
        if ("GET".equalsIgnoreCase(method) && !requestURI.contains(".")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // 从请求头获取 Token
        String authHeader = request.getHeader("Authorization");
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeUnauthorizedResponse(response, "未提供有效的认证令牌，请先登录！");
            return;
        }
        
        String token = authHeader.substring(7);
        
        if (!jwtUtil.validateToken(token)) {
            writeUnauthorizedResponse(response, "认证令牌无效或已过期，请重新登录！");
            return;
        }
        
        // 解析用户信息并设置到请求属性
        User user = jwtUtil.getUserFromToken(token);
        if (user == null) {
            writeUnauthorizedResponse(response, "认证令牌解析失败，请重新登录！");
            return;
        }
        
        request.setAttribute(Const.REQUEST_CURRENT_USER, user);
        
        filterChain.doFilter(request, response);
    }
    
    /**
     * 写入 401 未授权 JSON 响应
     */
    private void writeUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        
        ServerResponse<?> serverResponse = ServerResponse.createByErrorCodeMessage(
                HttpServletResponse.SC_UNAUTHORIZED, message);
        
        objectMapper.writeValue(response.getWriter(), serverResponse);
    }
}
