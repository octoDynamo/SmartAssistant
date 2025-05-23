package agents;

import gui.InterfaceGraphique;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent {
    private InterfaceGraphique gui; // Store GUI instance

    protected void setup() {
        System.out.println("ClientAgent prêt. Lancement de l'interface graphique...");
        gui = new InterfaceGraphique(this); // Initialize GUI
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    gui.afficherReponse(msg.getContent()); // Call non-static method
                } else {
                    block();
                }
            }
        });
    }

    public void envoyerCommande(String commande) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        if (commande.startsWith("meteo ")) {
            msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
            msg.setContent("recherche_meteo:" + commande.substring(6));
        } else if (commande.startsWith("recette ")) {
            msg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
            msg.setContent("recherche_recette:" + commande.substring(8));
        } else if (commande.startsWith("planifie ")) {
            msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
            String[] parts = commande.substring(9).split(":");
            msg.setContent("planifie_evenement:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31 15:00"));
        } else if (commande.equals("liste_evenements")) {
            msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
            msg.setContent("liste_evenements");
        } else {
            msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));
            String[] parts = commande.startsWith("ajoute ") ? commande.substring(7).split(":") : new String[]{commande};
            String content = commande;
            if (commande.startsWith("ajoute ")) {
                content = "ajoute_tache:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31") + ":" + (parts.length > 2 ? parts[2] : "Medium");
            } else if (commande.startsWith("supprime ")) {
                content = "supprime_tache:" + commande.substring(9);
            } else if (commande.equals("liste")) {
                content = "liste_taches";
            }
            msg.setContent(content);
        }
        send(msg);
    }
}