package org.machinesystems.UserMachine.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.machinesystems.UserMachine.model.AuditLog;
import org.machinesystems.UserMachine.repository.AuditLogRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

public class AuditServiceTest {

    @InjectMocks
    private AuditService auditService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @BeforeEach
    void setUp() {
        // Initialize mocks
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLogAuditEvent() {
        // Given
        String level = "INFO";
        String loggerName = "TestLogger";
        String message = "This is a test message";
        String threadName = "main";
        String exception = "None";

        // When
        auditService.logAuditEvent(level, loggerName, message, threadName, exception);

        // Then
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(auditLogCaptor.capture());

        AuditLog capturedLog = auditLogCaptor.getValue();
        assertEquals(level, capturedLog.getLevel());
        assertEquals(loggerName, capturedLog.getLogger());
        assertEquals(message, capturedLog.getMessage());
        assertEquals(threadName, capturedLog.getThread());
        assertEquals(exception, capturedLog.getException());
        // Optional: Check if timestamp is not null
        assertNotNull(capturedLog.getTimestamp());
    }
}

