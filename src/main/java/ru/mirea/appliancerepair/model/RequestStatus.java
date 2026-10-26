package ru.mirea.appliancerepair.model;

public enum RequestStatus {
    NEW("Новая"),
    DIAGNOSING("Диагностика"),
    REPAIRING("В ремонте"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена");

    private final String title;

    RequestStatus(String title) { this.title = title; }

    @Override public String toString() { return title; }
}
