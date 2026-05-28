package com.example.backend.config;

import com.example.backend.service.registrar.RegistrarSyncService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RegistrarStartupSyncRunner implements CommandLineRunner {

    private final RegistrarSyncService registrarSyncService;

    @Value("${registrar.sync-on-startup:false}")
    private boolean syncOnStartup;

    public RegistrarStartupSyncRunner(RegistrarSyncService registrarSyncService) {
        this.registrarSyncService = registrarSyncService;
    }

    @Override
    public void run(String... args) {
        if (!syncOnStartup) {
            System.out.println("Registrar startup sync skipped.");
            return;
        }

        try {
            System.out.println("Registrar startup sync started.");
            String result = registrarSyncService.initialSync(null);
            System.out.println("Registrar startup sync finished: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}