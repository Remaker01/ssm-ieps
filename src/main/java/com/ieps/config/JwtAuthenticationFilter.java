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
import java.util.ArrayList;
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
    private final List<String> whiteList = new ArrayList<>();
    
    private final PathMatcher pathMatcher = new AntPathMatcher();
    
    public JwtAuthenticationFilter() {
        // 公开页面路由
        whiteList.add("/");
        whiteList.add("/home");
        whiteList.add("/login");
        whiteList.add("/register");
        whiteList.add("/logout");
        whiteList.add("/forget-password");
        whiteList.add("/informs/**");
        whiteList.add("/items");
        whiteList.add("/items/detail");
        whiteList.add("/downloads");
        
        // 兼容旧 .do 路由
        whiteList.add("/goHome.do");
        whiteList.add("/login.do");
        whiteList.add("/register.do");
        whiteList.add("/logout.do");
        whiteList.add("/goLogin.do");
        whiteList.add("/goRegister.do");
        whiteList.add("/goForgetPwd.do");
        whiteList.add("/goShowInformDetail.do");
        whiteList.add("/goShowInfoemDetail.do");
        whiteList.add("/goShowInformMore.do");
        whiteList.add("/goShowItemMore.do");
        whiteList.add("/goShowItemDetail.do");
        whiteList.add("/goShowDownLoadMore.do");
        
        // 验证码相关
        whiteList.add("/getVerifyCode");
        whiteList.add("/getVerifyCode.do");
        whiteList.add("/checkVerifyCode");
        whiteList.add("/checkVerifyCode.do");
        whiteList.add("/checkVerifyNum");
        whiteList.add("/checkVerifyNum.do");
        
        // 公共 AJAX 数据接口（无需登录）
        whiteList.add("/checkUser");
        whiteList.add("/checkUser.do");
        whiteList.add("/getInformDetailById");
        whiteList.add("/getInformDetailById.do");
        whiteList.add("/getItemDetailWithItemNum");
        whiteList.add("/getItemDetailWithItemNum.do");
        whiteList.add("/getUserInfoWithItemNum");
        whiteList.add("/getUserInfoWithItemNum.do");
        whiteList.add("/getFinishedItemList");
        whiteList.add("/getFinishedItemList.do");
        whiteList.add("/getInformListByAdminWithUserNum");
        whiteList.add("/getInformListByAdminWithUserNum.do");
        whiteList.add("/getFileListByAdminWithKind");
        whiteList.add("/getFileListByAdminWithKind.do");
        whiteList.add("/previewFile");
        whiteList.add("/previewFile.do");
        whiteList.add("/downloadFile");
        whiteList.add("/downloadFile.do");
        whiteList.add("/downloadFileWithItemNum");
        whiteList.add("/downloadFileWithItemNum.do");
        
        // 静态资源（按扩展名放行）
        whiteList.add("/static/**");
        whiteList.add("/pages/**");
        whiteList.add("/upload/**");
        whiteList.add("/hub/**");
        whiteList.add("/favicon.ico");
        whiteList.add("/error");
        
        // 通用静态文件扩展名
        whiteList.add("/**/*.html");
        whiteList.add("/**/*.css");
        whiteList.add("/**/*.js");
        whiteList.add("/**/*.png");
        whiteList.add("/**/*.jpg");
        whiteList.add("/**/*.jpeg");
        whiteList.add("/**/*.gif");
        whiteList.add("/**/*.ico");
        whiteList.add("/**/*.woff");
        whiteList.add("/**/*.woff2");
        whiteList.add("/**/*.ttf");
        whiteList.add("/**/*.svg");
        whiteList.add("/**/*.map");
    }
    
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
