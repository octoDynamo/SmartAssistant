package models;

import java.util.Date;

public class Task {
    private String description;
    private Date dueDate;
    private String priority; // "High", "Medium", "Low"
    private boolean completed;

    public Task(String description, int priority) {
        this.description = description;
        this.dueDate = null; // Set to null or handle separately if needed
        this.priority = convertPriority(priority);
        this.completed = false;
    }

    // Helper method to convert int priority to String
    private String convertPriority(int priority) {
        switch (priority) {
            case 1: return "High";
            case 2: return "Medium";
            case 3: return "Low";
            default: return "Medium"; // Default value
        }
    }

    // Existing constructor for compatibility with other parts (e.g., JSON deserialization)
    public Task(String description, Date dueDate, String priority) {
        this.description = description;
        this.dueDate = dueDate;
        this.priority = priority;
        this.completed = false;
    }

    // Getters and setters
    public String getDescription() { return description; }
    public Date getDueDate() { return dueDate; }
    public String getPriority() { return priority; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void setDueDate(Date dueDate) { this.dueDate = dueDate; }

    @Override
    public String toString() {
        return description + " (Due: " + (dueDate != null ? dueDate : "N/A") + ", Priority: " + priority + ")";
    }


}