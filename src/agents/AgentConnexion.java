package agents;

import models.Utilisateur;
import utils.GestionnaireStockage;
import java.util.logging.Logger;

public class AgentConnexion {
    private static final Logger LOGGER = Logger.getLogger(AgentConnexion.class.getName());
    private GestionnaireStockage stockage = new GestionnaireStockage();

    public boolean authentifier(String nomUtilisateur, String motDePasse) {
        LOGGER.info("Tentative d'authentification pour : " + nomUtilisateur);
        Utilisateur user = stockage.chargerUtilisateur(nomUtilisateur);
        if (user == null) {
            LOGGER.warning("Utilisateur non trouvé : " + nomUtilisateur);
            return false;
        }
        if (user.getMotDePasse().equals(motDePasse)) {
            LOGGER.info("Authentification réussie pour : " + nomUtilisateur);
            return true;
        }
        LOGGER.warning("Mot de passe incorrect pour : " + nomUtilisateur);
        return false;
    }

    public Utilisateur getUtilisateur(String nomUtilisateur) {
        return stockage.chargerUtilisateur(nomUtilisateur);
    }

    public void enregistrerUtilisateur(String nomUtilisateur, String motDePasse) {
        Utilisateur user = new Utilisateur(nomUtilisateur, motDePasse);
        stockage.sauvegarderUtilisateur(user);
        LOGGER.info("Utilisateur enregistré : " + nomUtilisateur);
    }
}