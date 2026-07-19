/**
 * IEPS JWT 鉴权辅助脚本
 * 全局 AJAX 配置：自动携带 Access Token、401 自动刷新、双 Token 管理
 */

// Token 存储 Key
const IEPS_TOKEN_KEY = 'ieps_token';
const IEPS_REFRESH_TOKEN_KEY = 'ieps_refresh_token';

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

// Access Token
function getToken() {
    return localStorage.getItem(IEPS_TOKEN_KEY);
}

function setToken(token) {
    localStorage.setItem(IEPS_TOKEN_KEY, token);
}

function removeToken() {
    localStorage.removeItem(IEPS_TOKEN_KEY);
}

// Refresh Token
function getRefreshToken() {
    return localStorage.getItem(IEPS_REFRESH_TOKEN_KEY);
}

function setRefreshToken(token) {
    localStorage.setItem(IEPS_REFRESH_TOKEN_KEY, token);
}

function removeRefreshToken() {
    localStorage.removeItem(IEPS_REFRESH_TOKEN_KEY);
}

function hasToken() {
    return !!getToken();
}

/**
 * 清除所有 Token（退出或刷新彻底失败时调用）
 */
function clearAllTokens() {
    removeToken();
    removeRefreshToken();
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
        const decoded = atob(payload.replace(/-/g, '+').replace(/_/g, '/'));
        return JSON.parse(decoded);
    } catch (e) {
        console.error('Token 解析失败', e);
        return null;
    }
}

/**
 * 获取当前登录用户编号（从前端存储的 access token 中解析）
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
 */
function getAuthParams() {
    var payload = parseTokenPayload(getToken());
    return {
        userNum: payload ? payload.userNum : '',
        roleId: payload ? payload.roleId : ''
    };
}

// ======================== Token 刷新 ========================

// 刷新互斥锁：防止并发 401 触发多次刷新
var _refreshing = false;
// 请求重试队列：在刷新期间排队等待的请求回调
var _refreshQueue = [];

/**
 * 用 Refresh Token 换取新的 Access Token
 * 返回 Promise，刷新成功时 resolve，彻底失败时 reject
 */
function refreshAccessToken() {
    var refreshToken = getRefreshToken();
    if (!refreshToken) {
        return Promise.reject(new Error('无 Refresh Token'));
    }

    return new Promise(function (resolve, reject) {
        $.ajax({
            url: '/refresh',
            method: 'POST',
            data: { refreshToken: refreshToken },
            dataType: 'json',
            success: function (res) {
                if (res && res.status === 0 && res.data && res.data.accessToken) {
                    setToken(res.data.accessToken);
                    resolve(res.data.accessToken);
                } else {
                    reject(new Error('刷新失败：' + (res ? res.msg : '未知错误')));
                }
            },
            error: function () {
                reject(new Error('刷新请求网络异常'));
            }
        });
    });
}

/**
 * 处理 401 响应：尝试刷新 Token，成功则重试所有排队请求
 * @param {Function} retryOriginal - 重试原始请求的回调
 */
function handleUnauthorized(retryOriginal) {
    if (_refreshing) {
        // 已有刷新在进行中，将当前请求加入队列等待
        _refreshQueue.push(retryOriginal);
        return;
    }

    _refreshing = true;

    refreshAccessToken().then(function (newToken) {
        // 刷新成功，重试当前请求
        _refreshing = false;
        if (retryOriginal) retryOriginal();
        // 重试队列中等待的其他请求
        var queue = _refreshQueue.slice();
        _refreshQueue = [];
        queue.forEach(function (cb) { if (cb) cb(); });
    }).catch(function () {
        // 刷新彻底失败：清除 token，跳转登录页
        _refreshing = false;
        _refreshQueue = [];
        clearAllTokens();
        if (!isPublicPath(window.top.location.pathname)) {
            window.top.location.href = '/login';
        }
    });
}

// ======================== 页面鉴权 ========================

function topLocation() {
    return window.top.location;
}

function requireAuth() {
    if (!hasToken()) {
        // 有 Refresh Token 时尝试恢复会话
        if (getRefreshToken()) {
            handleUnauthorized(function () {
                topLocation().href = topLocation().href;
            });
            return false;
        }
        topLocation().href = '/login';
        return false;
    }
    return true;
}

function redirectIfAuthenticated() {
    if (!hasToken()) return;
    if (isTokenExpiring(1)) return;
    var payload = parseTokenPayload(getToken());
    if (payload && payload.userNum && payload.roleId) {
        topLocation().href = '/index';
    }
}

// ======================== 全局 AJAX 配置 ========================

/**
 * 配置指定 jQuery 实例的 AJAX 全局设置
 * - 自动注入 Authorization: Bearer <access_token>
 * - 401 时自动尝试刷新 Token
 */
function setupAjaxAuth(jqInstance) {
    if (!jqInstance || !jqInstance.ajaxSetup) return;

    // 保存原始的 statusCode 设置
    jqInstance.ajaxSetup({
        beforeSend: function (xhr) {
            var token = getToken();
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        }
    });

    // 使用 ajaxPrefilter 拦截 401 响应以实现自动刷新
    // 避免 statusCode 可能导致无限递归
    $(document).ajaxError(function (event, jqXHR, settings, error) {
        if (jqXHR.status === 401) {
            // 跳过刷新接口自身的 401
            if (settings.url === '/refresh' || settings.url === '/refresh.do') {
                return;
            }

            // 阻止默认错误处理
            event.stopPropagation();

            // 保存原始请求的配置，用于刷新后重试
            var originalSettings = settings;

            handleUnauthorized(function () {
                // 刷新成功后用新 Token 重试原始请求
                var newToken = getToken();
                if (newToken) {
                    $.ajax(originalSettings);
                }
            });
        }
    });
}

// ======================== 初始化 ========================

// 1) 立即配置全局 jQuery（window.$）
setupAjaxAuth(window.$);

// 2) Layui 内部 jQuery——代理 layui.use() 自动配置
if (window.layui) {
    var _origLayuiUse = layui.use;
    layui.use = function () {
        if (layui.$ && layui.$.ajaxSetup) {
            setupAjaxAuth(layui.$);
        }
        return _origLayuiUse.apply(this, arguments);
    };
}

// 3) 暴露刷新函数（供登录页在登录成功后调用）
window.setLoginTokens = function (accessToken, refreshToken) {
    setToken(accessToken);
    setRefreshToken(refreshToken);
};

window.clearLoginTokens = function () {
    clearAllTokens();
};
