package com.ieps.service;

import com.ieps.config.IepsCosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时本地文件迁移到 COS
 *
 * <p>实现了 {@link CommandLineRunner}，在 Spring Boot 应用启动完成后自动执行。
 * 当配置项 {@code ieps.storage.cos.migration-enabled=true} 时生效。</p>
 *
 * <p><b>与异步下载的关系：</b></p>
 * <ul>
 *   <li>异步下载功能需要文件存储在 COS 上才能正常工作（ZIP 打包时通过 COS API 读取文件）</li>
 *   <li>如果项目的存量文件在本地磁盘，此迁移器会在启动时批量上传到 COS</li>
 *   <li>迁移后文件的 objectKey 更新到 {@code ieps_file_hub} 表的 object_key 字段</li>
 * </ul>
 */
@Component
public class StorageMigrationRunner implements CommandLineRunner {

    @Autowired
    private IepsCosProperties iepsCosProperties;

    @Autowired
    private StorageService storageService;

    @Override
    public void run(String... args) {
        if (iepsCosProperties.isMigrationEnabled()) {
            storageService.migrateLocalFilesToCos();
        }
    }
}
