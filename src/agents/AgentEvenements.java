package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import models.Event;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AgentEvenements extends Agent {
    private List<Event> listeEvenements = new ArrayList<>();

    protected void setup() {
        System.out.println("AgentEvenements démarré.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent();
                    ACLMessage reply = msg.createReply();
                    try {
                        if (contenu.startsWith("planifie_evenement:")) {
                            String[] parts = contenu.split(":", 3);
                            String description = parts[1];
                            Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(parts[2]);
                            Event evenement = new Event(description, date);
                            if (checkConflit(evenement)) {
                                reply.setContent("Conflit détecté pour " + description + ". Proposez une autre date.");
                            } else {
                                listeEvenements.add(evenement);
                                reply.setContent("Événement planifié: " + evenement);
                            }
                        } else if (contenu.equals("liste_evenements")) {
                            StringBuilder sb = new StringBuilder("Liste des événements:\n");
                            for (Event e : listeEvenements) {
                                sb.append("- ").append(e).append("\n");
                            }
                            reply.setContent(sb.toString());
                        } else {
                            reply.setContent("Commande inconnue pour AgentEvenements");
                        }
                    } catch (Exception e) {
                        reply.setContent("Erreur: " + e.getMessage()); // Send error to ClientAgent
                    }
                    send(reply);
                } else {
                    block();
                }
            }

            private boolean checkConflit(Event newEvent) {
                for (Event e : listeEvenements) {
                    if (Math.abs(e.getDate().getTime() - newEvent.getDate().getTime()) < 3600000) {
                        return true;
                    }
                }
                return false;
            }
        });
    }
}