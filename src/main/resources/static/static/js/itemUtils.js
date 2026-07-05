/**
 * 项目页面通用工具函数
 * 供 allItem/applyItem/endItem/inspectItem 等页面共享
 */

// ======== 获取URL参数 ========
function getUrlParam(name) {
    const reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    const r = window.parent.location.search.substr(1).match(reg);
    if (r != null) return encodeURI(r[2]);
    return null;
}

// ======== 生成流水号日期部分 ========
function getFormatDate() {
    const nowDate = new Date();
    const year = nowDate.getFullYear();
    const month = nowDate.getMonth() + 1 < 10 ? "0" + (nowDate.getMonth() + 1) : nowDate.getMonth() + 1;
    return year + "" + month;
}

// ======== 生成UUID（长度 + 进制） ========
function uuid(len, radix) {
    const chars = '0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz'.split('');
    const uuid = []; let i;
    radix = radix || chars.length;
    if (len) {
        for (i = 0; i < len; i++) uuid[i] = chars[0 | Math.random() * radix];
    } else {
        let r;
        uuid[8] = uuid[13] = uuid[18] = uuid[23] = '-';
        uuid[14] = '4';
        for (i = 0; i < 36; i++) {
            if (!uuid[i]) {
                r = 0 | Math.random() * 16;
                uuid[i] = chars[(i == 19) ? (r & 0x3) | 0x8 : r];
            }
        }
    }
    return uuid.join('');
}

// ======== 项目申请单 - 添加成员 ========
function addItemMemory() {
    const tbody = $("#tbody");
    const tr = $("<tr></tr>");
    const tdName = $("<td></td>");
    const tdNameInput = $("<input type='text' name='leaderMemoryName' class='layui-input' placeholder='请输入成员姓名'/>");
    tdName.append(tdNameInput);
    const tdNum = $("<td></td>");
    const tdNumInput = $("<input type='text' name='leaderMemoryNum' class='layui-input' placeholder='请输入成员学号'/>");
    tdNum.append(tdNumInput);
    const tdAction = $("<td></td>");
    const tdActionModify = $("<a></a>");
    const tdActionDel = $("<a></a>");
    tdActionModify.addClass("modify");
    tdActionModify.text(" 修改 ");
    tdActionDel.addClass("remove");
    tdActionDel.text(" 删除 ");
    tbody.append(tr.append(tdName).append(tdNum)
        .append(tdAction.append(tdActionModify).append(tdActionDel)));
}

// ======== 成员行 - 修改（编辑/保存切换） ========
function modifyInfo() {
    $(this).parent().siblings("td").each(function () {
        const is_text = $(this).find("input:text");
        if (!is_text.length) {
            $(this).html("<input type='text'  class='layui-input' value=' " + $(this).text() + " '/>");
        } else {
            $(this).html(is_text.val());
        }
    })
}

// ======== 成员行 - 删除 ========
function removeInfo() {
    $(this).parent().parent().remove();
}

// ======== 文件下载 ========
function chooseFileMethod(fileName) {
    window.location.href = "/downloadFile?fileName=" + encodeURIComponent(fileName);
}

// ======== 获取项目成员表格数据 ========
function getTableContent() {
    const set = [];
    $('#tbody').each(function () {
        const table = [];
        $(this).find('tr').each(function () {
            const row = [];
            $(this).find('th,td:lt(2)').each(function () {
                row.push($(this).text().trim());
            });
            table.push(row);
        });
        set.push(table);
    });
    return set;
}

// ======== POST 方式提交下载 ========
function submitDownload(url, params) {
    var form = $('<form method="post" style="display:none"></form>').attr('action', url);
    Object.keys(params || {}).forEach(function (key) {
        var value = params[key];
        if (Array.isArray(value)) {
            value.forEach(function (item) {
                form.append($('<input type="hidden"/>').attr('name', key).val(item));
            });
            return;
        }
        form.append($('<input type="hidden"/>').attr('name', key).val(value));
    });
    $('body').append(form);
    form.trigger('submit');
    form.remove();
}

// ======== 后端中转文件上传队列 ========
function createBackendUploadQueue(upload, options) {
    var fileQueue = {};
    var order = [];
    var listView = options.listView ? $(options.listView) : null;
    var totalUploads = 0;
    var completedUploads = 0;
    var failed = false;
    var submitDeferred = null;
    var uploadListIns;

    function rejectSubmit(message) {
        if (submitDeferred && submitDeferred.state() === "pending") {
            submitDeferred.reject(message);
        }
    }

    function resolveSubmit() {
        if (submitDeferred && submitDeferred.state() === "pending") {
            submitDeferred.resolve();
        }
    }

    function setRowStatus(index, html) {
        if (!listView || !listView.length) {
            return;
        }
        listView.find('tr#upload-' + index + ' .upload-status').html(html);
    }

    function appendRow(index, file, filesRef) {
        if (!listView || !listView.length) {
            return;
        }

        var tr = $([
            '<tr id="upload-' + index + '">',
            '<td>' + file.name + '</td>',
            '<td>' + (file.size / 1024).toFixed(1) + 'kb</td>',
            '<td class="upload-status">等待上传</td>',
            '<td><button class="layui-btn layui-btn-xs layui-btn-danger demo-delete">删除</button></td>',
            '</tr>'
        ].join(''));

        tr.find('.demo-delete').on('click', function () {
            delete fileQueue[index];
            delete filesRef[index];
            order = order.filter(function (item) {
                return item !== index;
            });
            tr.remove();
            if (uploadListIns && uploadListIns.config && uploadListIns.config.elem) {
                uploadListIns.config.elem.next()[0].value = '';
            }
        });

        listView.append(tr);
    }

    uploadListIns = upload.render({
        elem: options.elem,
        url: options.url || '/batchUploadFile',
        accept: options.accept || 'file',
        multiple: options.multiple !== false,
        auto: false,
        number: options.number || 8,
        field: options.field || 'files',
        bindAction: options.bindAction,
        before: function () {
            this.data = options.buildData ? (options.buildData() || {}) : {};
        },
        choose: function (obj) {
            var files = this.files = obj.pushFile();
            Object.keys(files).forEach(function (index) {
                if (fileQueue[index]) {
                    return;
                }
                fileQueue[index] = files[index];
                order.push(index);
                appendRow(index, files[index], files);
            });
        },
        done: function (res, index) {
            if (res.status === 0) {
                setRowStatus(index, '<span style="color: #5FB878;">上传成功</span>');
                delete this.files[index];
                delete fileQueue[index];
                completedUploads++;
                if (!failed && completedUploads >= totalUploads) {
                    resolveSubmit();
                }
                return;
            }

            failed = true;
            setRowStatus(index, '<span style="color: #FF5722;">上传失败</span>');
            rejectSubmit(res.msg || "文件上传失败，请稍后重试！");
        },
        error: function (index) {
            failed = true;
            setRowStatus(index, '<span style="color: #FF5722;">上传失败</span>');
            rejectSubmit("文件上传失败，请稍后重试！");
        }
    });

    return {
        hasFiles: function () {
            return order.some(function (index) {
                return !!fileQueue[index];
            });
        },
        submit: function () {
            submitDeferred = $.Deferred();
            failed = false;
            completedUploads = 0;
            totalUploads = order.filter(function (index) {
                return !!fileQueue[index];
            }).length;

            if (totalUploads === 0) {
                submitDeferred.resolve();
                return submitDeferred.promise();
            }

            $(options.bindAction).trigger('click');
            return submitDeferred.promise();
        }
    };
}
