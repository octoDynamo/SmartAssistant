package models;

import java.util.Date;

public class Event {
    private String description;
    private Date date;

    public Event(String description, Date date) {
        this.description = description;
        this.date = date;
    }

    public String getDescription() { return description; }
    public Date getDate() { return date; }

    @Override
    public String toString() {
        return description + " (Date: " + date + ")";
    }
}