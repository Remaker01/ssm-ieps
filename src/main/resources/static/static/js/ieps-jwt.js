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
 * 修复 $(document).ajaxError 全局事件与 Layui 的 jQuery 实例（layui.$）不兼容
 */
function refreshAccessToken() {
    var refreshToken = getRefreshToken();
    var promise = $.Deferred();

    if (!refreshToken) {
        promise.reject(new Error('无 Refresh Token'));
        return promise.promise();  // 返回 promise 对象，支持 .done/.fail/.then
    }

    $.ajax({
        url: '/refresh',
        method: 'POST',
        data: { refreshToken: refreshToken },
        dataType: 'json',
        success: function (res) {
            if (res && res.status === 0 && res.data && res.data.accessToken) {
                setToken(res.data.accessToken);
                promise.resolve(res.data.accessToken);
            } else {
                promise.reject(new Error('刷新失败：' + (res ? res.msg : '未知错误')));
            }
        },
        error: function () {
            promise.reject(new Error('刷新请求网络异常'));
        }
    });

    return promise.promise();
}

/**
 * 用 Refresh Token 换取新的 Access Token（同步版本）
 * 返回新的 Access Token，失败返回 null
 * 用于同步请求上下文中的 token 刷新
 */
function refreshAccessTokenSync() {
    var refreshToken = getRefreshToken();
    if (!refreshToken) {
        return null;
    }

    var newAccessToken = null;
    $.ajax({
        url: '/refresh',
        method: 'POST',
        data: { refreshToken: refreshToken },
        dataType: 'json',
        async: false,
        success: function (res) {
            if (res && res.status === 0 && res.data && res.data.accessToken) {
                setToken(res.data.accessToken);
                newAccessToken = res.data.accessToken;
            }
        },
        error: function () {
            newAccessToken = null;
        }
    });

    return newAccessToken;
}

/**
 * 处理 401 响应：尝试刷新 Token，成功则重试所有排队请求
 * @param {Object}  jqXHR       - jQuery XHR 对象
 * @param {Object}  settings    - 原始 AJAX 配置（用于重试）
 * @param {Object}  jqInstance  - 发起请求的 jQuery 实例（用于重试时保持兼容）
 */
function handleUnauthorized(jqXHR, settings, jqInstance) {
    var $jq = jqInstance || window.$;

    // 如果是同步请求，直接清空 token 并返回错误
    // 因为异步刷新无法在同步请求上下文中完成
    if (settings.async === false) {
        clearAllTokens();
        if (!isPublicPath(window.top.location.pathname)) {
            // 在同步上下文中，不立即跳转（避免阻塞），返回后让调用方处理
            if (settings.url && settings.url.indexOf('/refresh') === -1) {
                console.warn('同步请求遇到 401，Token 可能已过期，请重新登录');
            }
        }
        return;
    }

    if (_refreshing) {
        _refreshQueue.push(function () { $jq.ajax(settings); });
        return;
    }

    _refreshing = true;

    refreshAccessToken()
        .then(function (newToken) {
            // 刷新成功
            _refreshing = false;
            // 重试当前请求
            $jq.ajax(settings);
            // 重试队列中等待的其他请求
            var queue = _refreshQueue.slice();
            _refreshQueue = [];
            queue.forEach(function (cb) { if (cb) cb(); });
        })
        .fail(function () {
            // 刷新失败
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
        if (getRefreshToken()) {
            handleUnauthorized();
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

    jqInstance.ajaxSetup({
        beforeSend: function (xhr) {
            var token = getToken();
            if (token) {
                xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            }
        },
        statusCode: {
            401: function (xhr, _ajaxOptions, thrownError) {
                // 跳过刷新接口自身，避免递归
                if (_ajaxOptions && (_ajaxOptions.url === '/refresh' || _ajaxOptions.url === '/refresh.do')) {
                    return;
                }
                handleUnauthorized(xhr, this, jqInstance);
            }
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

window.refreshAccessTokenSync = refreshAccessTokenSync;
window.isPublicPath = isPublicPath;
