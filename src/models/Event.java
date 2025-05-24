package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Event {
    private String description;
    private Date date;

    // No-arg constructor for Jackson deserialization
    public Event() {
    }

    @JsonCreator
    public Event(@JsonProperty("description") String description, @JsonProperty("date") Date date) {
        this.description = description;
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return description + " (Date: " + sdf.format(date) + ")";
    }
}