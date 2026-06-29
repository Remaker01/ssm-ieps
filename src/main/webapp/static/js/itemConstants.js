/**
 * 项目级别/类型/状态 常量映射表
 * 统一管理数值与中文文字的转换，供 allItem/applyItem/endItem 等页面共享
 */

// ======== 工具函数 ========

/**
 * 根据正向映射表生成反向映射表（文字 -> 数值）
 * @param {Object} map  正向映射表 {数值: 文字}
 * @returns {Object}     反向映射表 {文字: 数值}
 */
function buildReverseMap(map) {
    var rev = {};
    for (var k in map) {
        rev[map[k]] = Number(k);
    }
    return rev;
}

/**
 * 生成 select 选项 HTML（laytpl 模板中用）
 * @param {Object} map      正向映射表
 * @param {number} selected 当前选中值
 * @returns {string}        选项 HTML 片段
 */
function renderOptions(map, selected) {
    var html = '';
    for (var key in map) {
        var sel = Number(key) === Number(selected) ? ' selected="selected"' : '';
        html += '<option value="' + key + '"' + sel + '>' + map[key] + '</option>\n';
    }
    return html;
}

// ======== 项目级别 ========
// 1: 无；2：校级；3：省区级；4：国家级
var ITEM_LEVEL_MAP     = {1: "无", 2: "校级", 3: "省区级", 4: "国家级"};
var ITEM_LEVEL_REVERSE = buildReverseMap(ITEM_LEVEL_MAP);

// ======== 项目类型 ========
// 1：创新训练；2：创业训练；3：创业实践
var ITEM_TYPE_MAP     = {1: "创新训练", 2: "创业训练", 3: "创业实践"};
var ITEM_TYPE_REVERSE = buildReverseMap(ITEM_TYPE_MAP);

// ======== 项目状态 ========
// 1：申请中；2：立项评审；3：已立项；4：立项失败；5：中期检查；6：待结题；7：结题评审；8：结题成功；9：结题失败
var ITEM_STATUS_MAP     = {1: "申请中", 2: "立项评审", 3: "已立项", 4: "立项失败", 5: "中期检查", 6: "待结题", 7: "结题评审", 8: "结题成功", 9: "结题失败"};
var ITEM_STATUS_REVERSE = buildReverseMap(ITEM_STATUS_MAP);
