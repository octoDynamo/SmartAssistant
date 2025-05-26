package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONObject;

public class AgentRecherche extends Agent {

    private static final String OPENWEATHER_API_KEY = "41b0205404f23ded8d76ca6f648c9ee4";

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
                    } else if (contenu.startsWith("recherche_wiki:")) {
                        String terme = contenu.split(":", 2)[1];
                        resultat = fetchWiki(terme);
                    } else if (contenu.startsWith("recherche_web:")) {
                        String requete = contenu.split(":", 2)[1];
                        resultat = fetchWebSearch(requete);
                    } else if (contenu.equals("recherche_news")) {
                        resultat = getNews();
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

    // -------- API WIKIPEDIA --------
    private String fetchWiki(String terme) {
        try {
            String urlStr = "https://fr.wikipedia.org/w/api.php?action=query&prop=extracts&exintro=true&explaintext=true&format=json&titles=" + terme.replace(" ", "%20");
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
            JSONObject pages = json.getJSONObject("query").getJSONObject("pages");

            String cle = pages.keys().next(); // clé dynamique
            JSONObject page = pages.getJSONObject(cle);

            if (page.has("extract")) {
                String extrait = page.getString("extract");
                if (extrait.isEmpty()) {
                    return "Aucun extrait trouvé pour : " + terme;
                }
                return "Extrait Wikipedia :\n" + extrait;
            } else {
                return "Aucun résultat trouvé sur Wikipedia pour : " + terme;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur lors de la recherche Wikipedia.";
        }
    }
    // -------- API NEWS --------
    private String getNews() {
        try {
            String apiKey = "pub_7ca1ae106dda4f66b19bd9bcc379251f";
            String urlStr = "https://newsdata.io/api/1/news?apikey=" + apiKey + "&language=fr&country=fr";
            URL url = new URL(urlStr);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            JSONObject json = new JSONObject(response.toString());

            if (!json.has("results")) {
                return "Aucun résultat renvoyé par l'API (peut-être aucune actualité ou clé incorrecte).";
            }

            JSONArray articles = json.getJSONArray("results");

            StringBuilder result = new StringBuilder();
            for (int i = 0; i < Math.min(3, articles.length()); i++) {
                JSONObject article = articles.getJSONObject(i);
                String title = article.optString("title");
                String link = article.optString("link");
                result.append((i + 1)).append(". ").append(title).append("\n").append(link).append("\n\n");
            }

            return result.toString().isEmpty() ? "Aucune actualité trouvée aujourd'hui." : result.toString();

        } catch (Exception e) {
            e.printStackTrace();
            return "Erreur : " + e.getMessage(); // Affiche l'erreur précise
        }
    }

}
