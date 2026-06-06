package com.example.auditlogservice.entities;

public enum ActionStatus {
    SUCCESS,
    FAILURE,
    WARNING    // ✅ ADDED — sent for non-blocking issues (e.g. security service unreachable)
}
