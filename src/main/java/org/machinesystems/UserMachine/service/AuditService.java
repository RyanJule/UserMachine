package org.machinesystems.UserMachine.service;

import org.machinesystems.UserMachine.model.AuditLog;
import org.machinesystems.UserMachine.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    public void logAuditEvent(String level, String loggerName, String message, String threadName, String exception) {
        AuditLog auditLog = new AuditLog();
        auditLog.setTimestamp(new Date());
        auditLog.setLevel(level);
        auditLog.setLogger(loggerName);
        auditLog.setMessage(message);
        auditLog.setThread(threadName);
        auditLog.setException(exception);

        auditLogRepository.save(auditLog);
    }
}
