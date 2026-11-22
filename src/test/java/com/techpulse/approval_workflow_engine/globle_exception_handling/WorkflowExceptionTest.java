
package com.techpulse.approval_workflow_engine.globle_exception_handling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class WorkflowExceptionTest {

    @Test
    void notFound_shouldReturnNotFoundException() {
        WorkflowException exception =
                WorkflowException.notFound("Request not found");

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Request not found", exception.getMessage());
    }

    @Test
    void conflict_shouldReturnConflictException() {
        WorkflowException exception =
                WorkflowException.conflict("Request already processed");

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Request already processed", exception.getMessage());
    }

    @Test
    void forbidden_shouldReturnForbiddenException() {
        WorkflowException exception =
                WorkflowException.forbidden("Access denied");

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertEquals("Access denied", exception.getMessage());
    }

    @Test
    void badRequest_shouldReturnBadRequestException() {
        WorkflowException exception =
                WorkflowException.badRequest("Invalid request");

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("Invalid request", exception.getMessage());
    }
}
