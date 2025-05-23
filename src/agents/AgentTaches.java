package agents;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import models.Task;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AgentTaches extends Agent {
    private List<Task> listeTaches = new ArrayList<>();

    protected void setup() {
        System.out.println("AgentTaches démarré.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent();
                    ACLMessage reply = msg.createReply();
                    try {
                        if (contenu.startsWith("ajoute_tache:")) {
                            String[] parts = contenu.split(":", 3);
                            String description = parts[1];
                            Date dueDate = new SimpleDateFormat("yyyy-MM-dd").parse(parts[2]);
                            String priority = parts.length > 3 ? parts[3] : "Medium";
                            Task task = new Task(description, dueDate, priority);
                            listeTaches.add(task);
                            reply.setContent("Tâche ajoutée: " + task);
                        } else if (contenu.startsWith("supprime_tache:")) {
                            String description = contenu.split(":", 2)[1];
                            boolean supprimée = listeTaches.removeIf(t -> t.getDescription().equals(description));
                            reply.setContent(supprimée ? "Tâche supprimée: " + description : "Tâche non trouvée: " + description);
                        } else if (contenu.equals("liste_taches")) {
                            StringBuilder sb = new StringBuilder("Liste des tâches:\n");
                            for (Task t : listeTaches) {
                                sb.append("- ").append(t).append("\n");
                            }
                            reply.setContent(sb.toString());
                        } else {
                            reply.setContent("Commande inconnue pour AgentTaches");
                        }
                    } catch (Exception e) {
                        reply.setContent("Erreur: " + e.getMessage());
                    }
                    send(reply);
                } else {
                    block();
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 60000) {
            @Override
            protected void onTick() {
                Date now = new Date();
                for (Task task : listeTaches) {
                    if (!task.isCompleted() && task.getDueDate().before(now)) {
                        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                        msg.addReceiver(new AID("client", AID.ISLOCALNAME));
                        msg.setContent("Rappel: Tâche \"" + task.getDescription() + "\" est due !");
                        send(msg);
                    }
                }
            }
        });
    }
}