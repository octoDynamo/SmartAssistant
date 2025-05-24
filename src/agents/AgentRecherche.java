package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

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
                        String resultat = fetchWeather(ville);
                        ACLMessage reply = msg.createReply();
                        reply.setContent(resultat);
                        send(reply);
                    } else if (contenu.startsWith("recherche_recette:")) {
                        String nomRecette = contenu.split(":", 2)[1];
                        String resultat = fetchRecipe(nomRecette);
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
        // À remplacer avec OpenWeatherMap plus tard
        return "Météo à " + ville + ": 20°C, ensoleillé";
    }

    private String fetchRecipe(String nomRecette) {
        try {
            String urlStr = "https://www.themealdb.com/api/json/v1/1/search.php?s=" + nomRecette.replace(" ", "%20");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // Lire la réponse
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseSB = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseSB.append(line);
            }
            in.close();

            // Parse JSON
            JSONObject jsonResponse = new JSONObject(responseSB.toString());
            JSONArray meals = jsonResponse.optJSONArray("meals");

            if (meals == null) {
                return "Aucune recette trouvée pour '" + nomRecette + "'";
            }

            JSONObject meal = meals.getJSONObject(0);
            String mealName = meal.getString("strMeal");
            String instructions = meal.getString("strInstructions");

            return "Recette: " + mealName + "\nInstructions: " + instructions;
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la recherche de la recette.";
        }
    }
}
