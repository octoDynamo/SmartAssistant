import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;


import java.util.ArrayList;
import java.util.List;




public class AgentTaches extends Agent {
    private List<String> listeTaches = new ArrayList<>();

    protected void setup() {
        System.out.println("AgentTaches démarré.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent();

                    if (contenu.startsWith("ajoute_tache:")) {
                        String tache = contenu.split(":", 2)[1];
                        listeTaches.add(tache);
                        System.out.println("Tâche ajoutée: " + tache);

                    } else if (contenu.startsWith("supprime_tache:")) {
                        String tacheASupprimer = contenu.split(":", 2)[1];
                        boolean supprimée = listeTaches.remove(tacheASupprimer);
                        if (supprimée) {
                            System.out.println("Tâche supprimée: " + tacheASupprimer);
                        } else {
                            System.out.println("Tâche non trouvée: " + tacheASupprimer);
                        }

                    } else if (contenu.equals("liste_taches")) {
                        StringBuilder sb = new StringBuilder("Liste des tâches:\n");
                        for (String t : listeTaches) {
                            sb.append("- ").append(t).append("\n");
                        }
                        ACLMessage reply = msg.createReply();
                        reply.setContent(sb.toString());
                        send(reply);

                    }
                } else {
                    block();
                }
            }
        });
    }
}
