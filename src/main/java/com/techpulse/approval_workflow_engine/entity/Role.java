package com.techpulse.approval_workflow_engine.entity;

public enum Role {
    REQUESTER(false),
    APPROVER(false),
    ADMIN(true);

    private final boolean canOverride;

    Role(boolean canOverride) {
        this.canOverride = canOverride;
    }

    public boolean canOverride() {
        return canOverride;
    }
}