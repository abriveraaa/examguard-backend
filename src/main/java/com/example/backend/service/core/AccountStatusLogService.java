package com.example.backend.service.core;

import com.example.backend.entity.core.AccountStatusLog;
import com.example.backend.entity.core.UserAccess;
import com.example.backend.repository.core.AccountStatusLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountStatusLogService {

    private final AccountStatusLogRepository repository;

    public AccountStatusLogService(AccountStatusLogRepository repository) {
        this.repository = repository;
    }

    public void log(UserAccess user,
                    String action,
                    String reason,
                    String performedBy,
                    String previousStatus,
                    String newStatus) {

        AccountStatusLog log = new AccountStatusLog();

        log.setSchoolId(user.getSchoolId());
        log.setRole(user.getRole());
        log.setAction(action);
        log.setReason(reason);
        log.setPerformedBy(performedBy);
        log.setPreviousStatus(previousStatus);
        log.setNewStatus(newStatus);

        repository.save(log);
    }
}