package com.ieps.enums;

/**
 * 异步下载任务状态枚举
 *
 * <p><b>状态流转：</b></p>
 * <pre>
 *   ┌─────────────────────────────────────────────────────┐
 *   │  1. PENDING（等待执行）                               │
 *   │     ↓                                                 │
 *   │  2. RUNNING（正在打包）                                │
 *   │     ├──→ 3. SUCCESS（打包完成，可下载）→ (超时) → EXPIRED │
 *   │     └──→ 4. FAILED（打包失败）                         │
 *   └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>各状态在前端的处理建议：</p>
 * <ul>
 *   <li>{@link #PENDING}、{@link #RUNNING} — 前端持续轮询，展示进度动画</li>
 *   <li>{@link #SUCCESS} — 显示下载按钮，用户可下载 ZIP 包</li>
 *   <li>{@link #FAILED} — 显示错误信息，提示用户重试</li>
 *   <li>{@link #EXPIRED} — 提示链接已过期，重新创建任务</li>
 * </ul>
 */
public enum DownloadTaskStatus {

    /** 等待执行：任务已创建，尚未开始打包 */
    PENDING("pending"),
    /** 正在打包：异步线程正在从 COS 读取文件并生成 ZIP */
    RUNNING("running"),
    /** 打包成功：ZIP 已上传到 COS，前端可获取下载链接 */
    SUCCESS("success"),
    /** 打包失败：打包过程中发生异常，可查看 errorMessage 获取详情 */
    FAILED("failed"),
    /** 已过期：ZIP 包已从 COS 删除，无法继续下载 */
    EXPIRED("expired");

    private final String value;

    DownloadTaskStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
