package agents;

import models.Utilisateur;
import models.Task;
import models.Event;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AgentCoordination {
    private AgentTaches agentTaches;
    private AgentEvenements agentEvenements;
    private AgentRecherche agentRecherche;
    private AgentConnexion agentConnexion;
    private Utilisateur utilisateurActuel;
    private ClientAgent clientAgent; // Field to hold ClientAgent instance

    public AgentCoordination() {
        this.agentTaches = new AgentTaches();
        this.agentEvenements = new AgentEvenements();
        this.agentRecherche = new AgentRecherche();
        this.agentConnexion = new AgentConnexion();
        this.utilisateurActuel = null;
        this.clientAgent = null; // Initialize to null, will be set later
    }

    public void setClientAgent(ClientAgent clientAgent) {
        this.clientAgent = clientAgent; // Set the ClientAgent instance
    }

    public ClientAgent getClientAgent() {
        return clientAgent; // Return the stored instance
    }

    public boolean connecterUtilisateur(String nomUtilisateur, String motDePasse) {
        if (agentConnexion.authentifier(nomUtilisateur, motDePasse)) {
            this.utilisateurActuel = agentConnexion.getUtilisateur(nomUtilisateur);
            return true;
        }
        return false;
    }

    public void definirUtilisateurActuel(Utilisateur utilisateur) {
        this.utilisateurActuel = utilisateur;
    }

    public Utilisateur getUtilisateurActuel() {
        return utilisateurActuel;
    }

    public void ajouterTache(String description, int priorite) {
        if (utilisateurActuel == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        Task tache = new Task(description, priorite);
        agentTaches.ajouterTache(tache);
        utilisateurActuel.ajouterTache(tache);
    }

    public void planifierEvenement(String titre, String dateStr) {
        if (utilisateurActuel == null) {
            throw new IllegalStateException("Utilisateur non connecté");
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd;HH:mm", Locale.US);
            sdf.setLenient(false);
            Date date;
            try {
                date = sdf.parse(dateStr);
            } catch (Exception e) {
                sdf = new SimpleDateFormat("yyyy-MM-dd;H:mm", Locale.US);
                sdf.setLenient(false);
                date = sdf.parse(dateStr);
            }
            Event evenement = new Event(titre, date);
            agentEvenements.planifieEvenement(evenement);
            utilisateurActuel.ajouterEvenement(evenement);
        } catch (Exception e) {
            throw new IllegalArgumentException("Format de date invalide: " + dateStr);
        }
    }

    public List<String> rechercherWeb(String requete) {
        return agentRecherche.rechercherWeb(requete);
    }
}