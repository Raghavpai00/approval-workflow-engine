package com.techpulse.approval_workflow_engine.globle_exception_handling;


import org.springframework.http.HttpStatus;

public class WorkflowException extends RuntimeException {

    private final HttpStatus status;

    public WorkflowException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    // Shortcuts so the service reads clearly
    public static WorkflowException notFound(String message) {
        return new WorkflowException(HttpStatus.NOT_FOUND, message);
    }

    public static WorkflowException conflict(String message) {
        return new WorkflowException(HttpStatus.CONFLICT, message);
    }

    public static WorkflowException forbidden(String message) {
        return new WorkflowException(HttpStatus.FORBIDDEN, message);
    }

    public static WorkflowException badRequest(String message) {
        return new WorkflowException(HttpStatus.BAD_REQUEST, message);
    }
}
