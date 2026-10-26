package ru.mirea.appliancerepair.model;

public enum Priority {
    LOW("Низкий"), MEDIUM("Обычный"), HIGH("Высокий"), URGENT("Срочный");

    private final String title;
    Priority(String title) { this.title = title; }
    @Override public String toString() { return title; }
}
