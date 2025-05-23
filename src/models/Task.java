package models;

import java.util.Date;

public class Task {
    private String description;
    private Date dueDate;
    private String priority; // "High", "Medium", "Low"
    private boolean completed;

    public Task(String description, Date dueDate, String priority) {
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = false;
    }

    // Getters, setters
    public String getDescription() { return description; }
    public Date getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    @Override
    public String toString() {
        return description + " (Due: " + dueDate + ", Priority: " + priority + ")";
    }
}