(function (window) {
    if (window.ieps) {
        return;
    }

    function getJQuery() {
        return window.jQuery || window.$;
    }

    function getLayer() {
        if (window.layer && typeof window.layer.msg === "function") {
            return window.layer;
        }
        if (window.layui && window.layui.layer && typeof window.layui.layer.msg === "function") {
            return window.layui.layer;
        }
        return null;
    }

    function showMessage(message, icon) {
        var layer = getLayer();
        if (layer) {
            layer.msg(message, icon ? {icon: icon} : {});
            return;
        }
        if (window.console && typeof window.console.warn === "function") {
            window.console.warn(message);
        }
    }

    function normalizeUrl(url) {
        if (!url) {
            return url;
        }
        return url.replace(/\.do(?=($|\?))/, "");
    }

    function request(method, url, data, options) {
        var $ = getJQuery();
        if (!$ || !$.ajax) {
            throw new Error("jQuery 未加载，无法发起 IEPS 请求");
        }

        var ajaxOptions = $.extend(true, {
            url: normalizeUrl(url),
            type: method,
            data: data || {}
        }, options || {});

        var jqxhr = $.ajax(ajaxOptions);
        if (!ajaxOptions.silentError) {
            jqxhr.fail(function (xhr) {
                if (xhr && xhr.responseJSON && xhr.responseJSON.msg) {
                    showMessage(xhr.responseJSON.msg, 2);
                    return;
                }
                showMessage("请求失败，请稍后重试！", 2);
            });
        }
        return jqxhr;
    }

    function requestSync(method, url, data, options) {
        var $ = getJQuery();
        if (!$ || !$.extend) {
            throw new Error("jQuery 未加载，无法发起 IEPS 请求");
        }
        var response = null;
        request(method, url, data, $.extend(true, {
            async: false,
            success: function (result) {
                response = result;
            }
        }, options || {}));
        return response;
    }

    function handleResponse(result, options) {
        var settings = options || {};
        if (!result) {
            if (!settings.silentError) {
                showMessage("请求失败，请稍后重试！", 2);
            }
            return null;
        }
        if (result.status !== 0) {
            if (!settings.silentError) {
                showMessage(result.msg || "操作失败，请稍后重试！", 2);
            }
            return null;
        }
        return result.data;
    }

    function getCurrentUser() {
        var payload = typeof window.parseTokenPayload === "function" ? window.parseTokenPayload(window.getToken && window.getToken()) : null;
        return {
            token: window.getToken ? window.getToken() : null,
            userNum: payload && payload.userNum ? payload.userNum : "",
            roleId: payload && payload.roleId != null ? payload.roleId : "",
            payload: payload
        };
    }

    function getAuthParams() {
        var currentUser = getCurrentUser();
        return {
            userNum: currentUser.userNum || "",
            roleId: currentUser.roleId || ""
        };
    }

    function loadCurrentUserInfo(options) {
        return request("GET", "/getUserInfo", {}, options);
    }

    function loadItemOptions(options) {
        return request("GET", "/meta/item-options", {}, options).done(function (result) {
            var data = handleResponse(result, options);
            if (data && typeof window.updateItemOptionMaps === "function") {
                window.updateItemOptionMaps(data);
            }
        });
    }

    function loadItemOptionsSync(options) {
        var result = requestSync("GET", "/meta/item-options", {}, options);
        var data = handleResponse(result, options);
        if (data && typeof window.updateItemOptionMaps === "function") {
            window.updateItemOptionMaps(data);
        }
        return data;
    }

    window.ieps = {
        auth: {
            requireAuth: function () {
                return window.requireAuth ? window.requireAuth() : true;
            },
            currentUser: getCurrentUser,
            params: getAuthParams,
            loadCurrentUserInfo: loadCurrentUserInfo,
            logout: function () {
                if (window.removeToken) {
                    window.removeToken();
                }
            }
        },
        http: {
            request: request,
            requestSync: requestSync,
            get: function (url, data, options) {
                return request("GET", url, data, options);
            },
            post: function (url, data, options) {
                return request("POST", url, data, options);
            },
            getSync: function (url, data, options) {
                return requestSync("GET", url, data, options);
            },
            postSync: function (url, data, options) {
                return requestSync("POST", url, data, options);
            },
            handleResponse: handleResponse,
            normalizeUrl: normalizeUrl
        },
        meta: {
            loadItemOptions: loadItemOptions,
            loadItemOptionsSync: loadItemOptionsSync
        }
    };
}(window));
