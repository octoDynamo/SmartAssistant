package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import gui.InterfaceGraphique;

public class AgentRecherche extends Agent {
    protected void setup() {
        System.out.println("AgentRecherche démarré.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent();
                    if (contenu.startsWith("recherche_meteo:")) {
                        String ville = contenu.split(":", 2)[1];
                        String resultat = fetchWeather(ville); // À implémenter
                        ACLMessage reply = msg.createReply();
                        reply.setContent(resultat);
                        send(reply);
                    } else if (contenu.startsWith("recherche_recette:")) {
                        String recette = contenu.split(":", 2)[1];
                        String resultat = fetchRecipe(recette); // À implémenter
                        ACLMessage reply = msg.createReply();
                        reply.setContent(resultat);
                        send(reply);
                    }
                } else {
                    block();
                }
            }
        });
    }

    private String fetchWeather(String ville) {
        // Simuler une API météo (remplacer par une vraie API comme OpenWeatherMap)
        return "Météo à " + ville + ": 20°C, ensoleillé";
    }

    private String fetchRecipe(String recette) {
        // Simuler une recherche web (remplacer par Jsoup ou une API)
        return "Recette pour " + recette + ": Ingrédients - farine, sucre, œufs...";
    }
}