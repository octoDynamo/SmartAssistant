package models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.text.SimpleDateFormat;

public class Event {
    private String titre;
    private Date date;

    public Event() {
    }

    @JsonCreator
    public Event(@JsonProperty("titre") String titre, @JsonProperty("date") Date date) {
        this.titre = titre;
        this.date = date;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
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
        return titre + " (Date: " + sdf.format(date) + ")";
    }
}