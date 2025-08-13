package com.accessvault.model.enums;

public enum AuditActionType {
    REGISTER,
    LOGIN,
    ADD_SECRET,
    VIEW_SECRETS,
    DELETE_SECRET,
    DELETE_BLOCKED,
    DELETE_NON_EXISTENT,
    AUTH_SUCCESS,
    UNAUTHORIZED_ACCESS,
    DELETE_SECRET_FAILED,
    EXPORT_LOGS
}
