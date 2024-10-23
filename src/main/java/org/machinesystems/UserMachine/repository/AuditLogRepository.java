package org.machinesystems.UserMachine.repository;

import org.machinesystems.UserMachine.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}