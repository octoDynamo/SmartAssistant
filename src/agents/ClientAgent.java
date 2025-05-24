package agents;


import gui.InterfaceGraphique;
import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent {
    private InterfaceGraphique gui;

    protected void setup() {
        System.out.println("ClientAgent prêt. Lancement de l'interface graphique...");
        ACLMessage testMsg = new ACLMessage(ACLMessage.REQUEST);
        testMsg.addReceiver(new AID("agentRecherche", AID.ISLOCALNAME));
        testMsg.setContent("test_connexion");
        send(testMsg);
        System.out.println("Message de test envoyé à agentRecherche.");

        gui = new InterfaceGraphique(this);

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    gui.afficherReponse(msg.getContent());
                } else {
                    block();
                }
            }
        });
    }


    public void envoyerCommande(String commande) {
        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        AID destinataire = null;
        String content = "";

        if (commande.startsWith("meteo ")) {
            destinataire = new AID("agentRecherche", AID.ISLOCALNAME);
            content = "recherche_meteo:" + commande.substring(6);
        } else if (commande.startsWith("recette ")) {
            destinataire = new AID("agentRecherche", AID.ISLOCALNAME);
            content = "recherche_recette:" + commande.substring(8);
        } else if (commande.startsWith("planifie ")) {
            destinataire = new AID("agentEvenements", AID.ISLOCALNAME);
            String[] parts = commande.substring(9).split(":");
            content = "planifie_evenement:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31 15:00");
        } else if (commande.equals("liste_evenements")) {
            destinataire = new AID("agentEvenements", AID.ISLOCALNAME);
            content = "liste_evenements";
        } else {
            destinataire = new AID("agentTaches", AID.ISLOCALNAME);
            if (commande.startsWith("ajoute ")) {
                String[] parts = commande.substring(7).split(":");
                content = "ajoute_tache:" + parts[0] + ":" + (parts.length > 1 ? parts[1] : "2025-12-31") + ":" + (parts.length > 2 ? parts[2] : "Medium");
            } else if (commande.startsWith("supprime ")) {
                content = "supprime_tache:" + commande.substring(9);
            } else if (commande.equals("liste")) {
                content = "liste_taches";
            } else {
                content = commande;
            }
        }

        // Envoi du message
        if (destinataire != null) {
            msg.addReceiver(destinataire);
            msg.setContent(content);
            send(msg);
        } else {
            gui.afficherReponse("Commande non reconnue.");
        }
    }
}   