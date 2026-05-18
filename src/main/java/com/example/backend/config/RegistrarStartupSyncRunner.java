package com.example.backend.config;

import com.example.backend.service.registrar.RegistrarSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RegistrarStartupSyncRunner implements CommandLineRunner {

    private final RegistrarSyncService registrarSyncService;

    public RegistrarStartupSyncRunner(RegistrarSyncService registrarSyncService) {
        this.registrarSyncService = registrarSyncService;
    }

    @Override
    public void run(String... args) {

        try {
            String result = registrarSyncService.initialSync(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}