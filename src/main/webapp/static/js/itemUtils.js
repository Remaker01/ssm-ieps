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

// ======== 在线预览 / 下载文件 ========
function chooseFileMethod(fileName) {
    function open_page(url, param) {
        var form = '<form action="' + url + '"  target="_blank"  id="windowOpen" style="display:none">';
        for (var key in param) {
            form += '<input name="' + key + '" value="' + param[key] + '"/>';
        }
        form += '</form>';
        $('body').append(form);
        $('#windowOpen').submit();
        $('#windowOpen').remove();
    }

    layer.confirm('你是需要预览文件还是下载文件呢？', {
        btn: ['预览', '下载']
    }, function (index) {
        let url = '';
        const param = {a: 1};

        $.ajaxSettings.async = false;
        $.get("/previewFile.do", {fileName: fileName}, function (result) {
            if (result.status != 0) {
                return layer.msg(result.msg, {icon: 2});
            }
            layer.msg(result.msg, {icon: 1});
            url = result.data;
        });
        $.ajaxSettings.async = true;
        layer.close(index);
        open_page(url, param);
    }, function (index) {
        window.location.href = "/downloadFile.do?fileName=" + fileName;
        layer.close(index);
    });
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
