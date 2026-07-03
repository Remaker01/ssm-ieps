/**
 * IEPS JWT 鉴权辅助脚本
 * 全局 AJAX 配置：自动携带 Token、处理 401 未授权
 */

// Token 存储 Key
const IEPS_TOKEN_KEY = 'ieps_token';

// 公开页面路径（发生 401 时不应跳转登录页）
const PUBLIC_PATHS = [
    '/login', '/register', '/forget-password', '/logout',
    '/', '/home', '/index',
    '/items', '/items/detail', '/informs', '/downloads',
    '/goLogin', '/goRegister', '/goForgetPwd', '/goHome',
    '/goShowInformDetail', '/goShowInformMore', '/goShowItemMore', '/goShowItemDetail', '/goShowDownLoadMore'
];

function isPublicPath(pathname) {
    return PUBLIC_PATHS.some(function (p) {
        return pathname === p || pathname.indexOf(p + '/') === 0 || pathname.indexOf(p + '?') === 0;
    });
}

// ======================== Token 管理 ========================

function getToken() {
    return localStorage.getItem(IEPS_TOKEN_KEY);
}

function setToken(token) {
    localStorage.setItem(IEPS_TOKEN_KEY, token);
}

function removeToken() {
    localStorage.removeItem(IEPS_TOKEN_KEY);
}

function hasToken() {
    return !!getToken();
}

/**
 * 从 JWT Token 中解析 payload（仅用于获取前端需要的 userNum/roleId）
 * 注意：不验证签名，仅做 base64 解码
 */
function parseTokenPayload(token) {
    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;
        const payload = parts[1];
        // Base64 URL decode
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decoded);
    } catch (e) {
        console.error('Token 解析失败', e);
        return null;
    }
}

/**
 * 获取当前登录用户编号（从前端存储的 token 中解析）
 */
function getCurrentUserNum() {
    const token = getToken();
    if (!token) return null;
    const payload = parseTokenPayload(token);
    return payload ? payload.userNum : null;
}

/**
 * 获取当前登录用户角色 ID
 */
function getCurrentRoleId() {
    const token = getToken();
    if (!token) return null;
    const payload = parseTokenPayload(token);
    return payload ? payload.roleId : null;
}

/**
 * 检查 Token 是否即将过期（剩余时间少于指定分钟数）
 */
function isTokenExpiring(minutes) {
    const token = getToken();
    if (!token) return true;
    const payload = parseTokenPayload(token);
    if (!payload || !payload.exp) return true;
    const now = Math.floor(Date.now() / 1000);
    return (payload.exp - now) < (minutes || 5) * 60;
}

/**
 * 获取 JWT Auth 请求参数（所有页面的统一入口）
 * 替代旧的 $.Request("userNum") 方式
 */
function getAuthParams() {
    var payload = parseTokenPayload(getToken());
    return {
        userNum: payload ? payload.userNum : '',
        roleId: payload ? payload.roleId : ''
    };
}

// ======================== 页面鉴权 ========================

/**
 * 获取顶层窗口的 location，用于统一页面跳转
 * 在 iframe 中会跳转顶层窗口（而非 iframe 自身）
 */
function topLocation() {
    return window.top.location;
}

/**
 * 页面鉴权检查：若无 Token 则跳转登录页
 * 在受保护页面的 <head> 中调用
 */
function requireAuth() {
    if (!hasToken()) {
        topLocation().href = '/login';
        return false;
    }
    return true;
}

/**
 * 登录态检查：如果 Token 有效则跳转到首页
 * 在登录页/注册页/首页等公开页面调用
 */
function redirectIfAuthenticated() {
    if (!hasToken()) return;
    // 仅当 token 未过期时才跳转，避免登陆页闪烁
    if (isTokenExpiring(1)) return;
    var payload = parseTokenPayload(getToken());
    if (payload && payload.userNum && payload.roleId) {
        topLocation().href = '/index';
    }
}

// ======================== 全局 AJAX 配置 ========================

/**
 * 配置指定 jQuery 实例的 AJAX 全局设置（注入 Authorization Token）
 */
function setupAjaxAuth(jqInstance) {
    if (!jqInstance || !jqInstance.ajaxSetup) return;
    jqInstance.ajaxSetup({
        beforeSend: function (xhr) {
            const token = getToken();
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        },
        statusCode: {
            401: function () {
                removeToken();
                if (!isPublicPath(window.top.location.pathname)) {
                    window.top.location.href = '/login';
                }
            }
        }
    });
}

// 1) 立即配置全局 jQuery（window.$）
setupAjaxAuth(window.$);

// 2) 如果 Layui 已加载，也配置其内部 jQuery（Layui table 等模块使用 layui.$）
if (window.layui && layui.$) {
    setupAjaxAuth(layui.$);
}
