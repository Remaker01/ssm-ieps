package com.ieps.service;

import com.ieps.config.IepsCosProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
