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

    private static final String OPENWEATHER_API_KEY = "41b0205404f23ded8d76ca6f648c9ee4"; // Remplace par ta clé OpenWeatherMap

    protected void setup() {
        System.out.println("AgentRecherche démarré.");

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent();
                    String resultat = null;

                    if (contenu.startsWith("recherche_meteo:")) {
                        String ville = contenu.split(":", 2)[1];
                        resultat = fetchWeather(ville);
                    } else if (contenu.startsWith("recherche_recette:")) {
                        String nomRecette = contenu.split(":", 2)[1];
                        resultat = fetchRecipe(nomRecette);
                    } else if (contenu.startsWith("recherche_web:")) {
                        String requete = contenu.split(":", 2)[1];
                        resultat = fetchWebSearch(requete);
                    }

                    if (resultat != null) {
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

    // -------- API MÉTÉO --------
    private String fetchWeather(String ville) {
        try {
            String urlStr = "http://api.openweathermap.org/data/2.5/weather?q=" + ville +
                    "&appid=" + OPENWEATHER_API_KEY + "&units=metric&lang=fr";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseSB = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseSB.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(responseSB.toString());
            String description = json.getJSONArray("weather").getJSONObject(0).getString("description");
            double temp = json.getJSONObject("main").getDouble("temp");

            return "Météo à " + ville + " : " + temp + "°C, " + description;
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la récupération de la météo.";
        }
    }

    // -------- API RECETTE --------
    private String fetchRecipe(String nomRecette) {
        try {
            String urlStr = "https://www.themealdb.com/api/json/v1/1/search.php?s=" + nomRecette.replace(" ", "%20");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseSB = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseSB.append(line);
            }
            in.close();

            JSONObject jsonResponse = new JSONObject(responseSB.toString());
            JSONArray meals = jsonResponse.optJSONArray("meals");

            if (meals == null) {
                return "Aucune recette trouvée pour '" + nomRecette + "'";
            }

            JSONObject meal = meals.getJSONObject(0);
            String mealName = meal.getString("strMeal");
            String instructions = meal.getString("strInstructions");

            return "Recette : " + mealName + "\nInstructions : " + instructions;
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la recherche de la recette.";
        }
    }

    // -------- API RECHERCHE WEB --------
    private String fetchWebSearch(String requete) {
        try {
            String urlStr = "https://api.duckduckgo.com/?q=" + requete.replace(" ", "+") + "&format=json";
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder responseSB = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                responseSB.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(responseSB.toString());
            String result = json.optString("AbstractText");

            if (result == null || result.isEmpty()) {
                return "Aucun résultat trouvé pour : " + requete;
            }
            return "Résultat : " + result;
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la recherche web.";
        }
    }
}
