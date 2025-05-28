package models;

import java.util.ArrayList;
import java.util.List;

public class Utilisateur {
    private String nomUtilisateur;
    private String motDePasse;
    private int idUtilisateur; // Added ID field
    private List<Task> taches = new ArrayList<>();
    private List<Event> evenements = new ArrayList<>();

    public Utilisateur(String nomUtilisateur, String motDePasse) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
        this.idUtilisateur = generateId(); // Example ID generation
    }

    public Utilisateur(String nomUtilisateur, String motDePasse, int idUtilisateur) {
        this.nomUtilisateur = nomUtilisateur;
        this.motDePasse = motDePasse;
        this.idUtilisateur = idUtilisateur;
    }

    private int generateId() {
        // Simple incrementing ID (for demo purposes)
        return (int) (System.currentTimeMillis() % 1000000); // Unique enough for testing
    }

    public String getNomUtilisateur() { return nomUtilisateur; }
    public String getMotDePasse() { return motDePasse; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public List<Task> getTaches() { return taches; }
    public List<Event> getEvenements() { return evenements; }

    public void ajouterTache(Task tache) { taches.add(tache); }
    public void ajouterEvenement(Event evenement) { evenements.add(evenement); }
}