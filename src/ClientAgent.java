import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class ClientAgent extends Agent {
    protected void setup() {
        System.out.println("ClientAgent prêt. Lancement de l'interface graphique...");

        // Lancer l'interface graphique Swing
        new InterfaceGraphique(this);

        // Comportement pour recevoir les réponses
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("Réponse reçue :\n" + msg.getContent());
                    InterfaceGraphique.afficherReponse(msg.getContent()); // afficher dans l'interface
                } else {
                    block();
                }
            }
        });
    }
}



/*public class ClientAgent extends Agent {
    protected void setup() {
        System.out.println("ClientAgent prêt. Tapez une commande :");

        // Lancer l'interface terminal dans un thread
        new Thread(new TerminalInterface(this)).start();

        // Ajouter un comportement pour écouter les réponses
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    System.out.println("Réponse reçue :\n" + msg.getContent());
                } else {
                    block();
                }
            }
        });
    }
}*/



