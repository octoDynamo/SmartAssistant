package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import models.Event;
import javax.swing.*;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class AgentEvenements extends Agent {
    private static final Logger LOGGER = Logger.getLogger(AgentEvenements.class.getName());
    private List<Event> listeEvenements = new ArrayList<>();
    private final File file = new File("events.json");
    private final Set<String> notificationsEnvoyees = new HashSet<>();

    protected void setup() {
        try {
            System.out.println("AgentEvenements démarré.");
            loadEvents();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de AgentEvenements : " + e.getMessage());
            e.printStackTrace();
        }

        // Comportement pour gérer les messages entrants
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent().trim().replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+", " ");
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    try {
                        LOGGER.info("Received command: '" + contenu + "'");

                        if (contenu.startsWith("check_time_slot:")) {
                            long timeMillis = Long.parseLong(contenu.split(":")[1]);
                            Date date = new Date(timeMillis);
                            Event tempEvent = new Event("temp_check", date);
                            boolean conflit = checkConflit(tempEvent);
                            reply.setContent(conflit ? "unavailable" : "available");
                            send(reply);
                            return;
                        }
                        else if (contenu.startsWith("reserve_time_slot:")) {
                            String[] parts = contenu.split(":", 3);
                            String description = parts[1];
                            long timeMillis = Long.parseLong(parts[2]);
                            Date date = new Date(timeMillis);

                            Event blockingEvent = new Event("Tâche: " + description, date);
                            if (checkConflit(blockingEvent)) {
                                reply.setContent("conflit");
                            } else {
                                listeEvenements.add(blockingEvent);
                                saveEvents();
                                reply.setContent("ok");
                            }
                            send(reply);
                            return;
                        }
                        else if (contenu.startsWith("planifie_evenement:")) {
                            String[] parts = contenu.split(":", 3);
                            if (parts.length < 2) {
                                throw new IllegalArgumentException("Description ou date manquante");
                            }
                            String description = parts[1].trim();
                            if (description.isEmpty()) {
                                throw new IllegalArgumentException("Description vide");
                            }
                            String dateTimeStr = parts.length > 2 ? parts[2].trim() : "2025-12-31;15:00";
                            dateTimeStr = dateTimeStr.replaceAll("[\\u00A0\\u200B\\uFEFF]", " ").replaceAll("[-–—]", "-");

                            if (dateTimeStr.contains(" ") && !dateTimeStr.contains(";")) {
                                dateTimeStr = dateTimeStr.replaceFirst(" ", ";");
                                LOGGER.info("Converted space to semicolon: '" + dateTimeStr + "'");
                            }

                            Date date;
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd;HH:mm", Locale.US);
                                sdf.setLenient(false);
                                try {
                                    date = sdf.parse(dateTimeStr);
                                } catch (ParseException e) {
                                    sdf = new SimpleDateFormat("yyyy-MM-dd;H:mm", Locale.US);
                                    sdf.setLenient(false);
                                    date = sdf.parse(dateTimeStr);
                                }
                            } catch (ParseException e) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                                    sdf.setLenient(false);
                                    date = sdf.parse(dateTimeStr.replace(";", " "));
                                } catch (ParseException e2) {
                                    throw new IllegalArgumentException("Format de date invalide. Utilisez yyyy-MM-dd;HH:mm ou yyyy-MM-dd;H:mm");
                                }
                            }
                            Event evenement = new Event(description, date);
                            if (checkConflit(evenement)) {
                                reply.setContent("⚠️ Conflit détecté pour \"" + description + "\". Proposez une autre date.");
                            } else {
                                listeEvenements.add(evenement);
                                saveEvents();
                                reply.setContent("✅ Événement planifié : " + evenement);
                            }
                        } else if (contenu.startsWith("supprime_evenement:")) {
                            String description = contenu.split(":", 2)[1].trim();
                            boolean removed = listeEvenements.removeIf(e -> e.getDescription().equalsIgnoreCase(description));
                            if (removed) {
                                saveEvents();
                                reply.setContent("✅ Événement supprimé : " + description);
                            } else {
                                reply.setContent("❌ Événement non trouvé : " + description);
                            }
                        } else if (contenu.equals("liste_evenements")) {
                            if (listeEvenements.isEmpty()) {
                                reply.setContent("Liste des événements:\nAucun événement planifié.");
                            } else {
                                StringBuilder sb = new StringBuilder("Liste des événements:\n");
                                for (Event e : listeEvenements) {
                                    sb.append("- ").append(e).append("\n");
                                }
                                reply.setContent(sb.toString());
                            }
                        } else {
                            reply.setContent("❌ Commande inconnue. Essayez : planifie_evenement:<événement>:<yyyy-MM-dd;HH:mm>, supprime_evenement:<événement>, liste_evenements");
                        }
                    } catch (IllegalArgumentException e) {
                        reply.setContent("Erreur : " + e.getMessage());
                    } catch (Exception e) {
                        reply.setContent("Erreur : " + e.getMessage());
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

        // Système de notifications optimisé
        addBehaviour(new TickerBehaviour(this, 60000) { // Vérifie toutes les minutes
            @Override
            protected void onTick() {
                Date now = new Date();
                Date notificationTime = new Date(now.getTime() + 300000); // 5 minutes plus tard

                listeEvenements.stream()
                        .filter(e -> e.getDate().after(now) && e.getDate().before(notificationTime))
                        .forEach(e -> {
                            String notificationKey = e.getDescription() + "@" + e.getDate().getTime();
                            if (!notificationsEnvoyees.contains(notificationKey)) {
                                showNotification(e);
                                notificationsEnvoyees.add(notificationKey);
                            }
                        });

                // Nettoyage des anciennes notifications
                notificationsEnvoyees.removeIf(key -> {
                    long eventTime = Long.parseLong(key.split("@")[1]);
                    return eventTime < now.getTime();
                });
            }
        });
    }

    private void showNotification(Event event) {
        String message = String.format(
                "L'événement \"%s\" commence à %s",
                event.getDescription(),
                new SimpleDateFormat("HH:mm").format(event.getDate())
        );

        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(
                        null,
                        message,
                        "🔔 Rappel d'événement",
                        JOptionPane.INFORMATION_MESSAGE
                )
        );
    }

    private void saveEvents() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(file, listeEvenements);
        } catch (Throwable t) {
            LOGGER.severe("Erreur sauvegarde événements : " + t.getMessage());
        }
    }

    private void loadEvents() {
        try {
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                List<Event> loadedEvents = mapper.readValue(file,
                        mapper.getTypeFactory().constructCollectionType(List.class, Event.class));
                listeEvenements.addAll(loadedEvents);
            }
        } catch (Throwable t) {
            LOGGER.severe("Erreur chargement événements : " + t.getMessage());
        }
    }
}













/*package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import models.Event;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class AgentEvenements extends Agent {
    private static final Logger LOGGER = Logger.getLogger(AgentEvenements.class.getName());
    private List<Event> listeEvenements = new ArrayList<>();
    private final File file = new File("events.json");

    protected void setup() {
        try {
            System.out.println("AgentEvenements démarré.");
            loadEvents();
        } catch (Exception e) {
            LOGGER.severe("Erreur lors de l'initialisation de AgentEvenements : " + e.getMessage());
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String contenu = msg.getContent().trim().replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+", " ");
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);

                    try {
                        LOGGER.info("Received command: '" + contenu + "'");

                        // Nouveaux messages pour la gestion des créneaux
                        if (contenu.startsWith("check_time_slot:")) {
                            long timeMillis = Long.parseLong(contenu.split(":")[1]);
                            Date date = new Date(timeMillis);
                            Event tempEvent = new Event("temp_check", date);
                            boolean conflit = checkConflit(tempEvent);
                            reply.setContent(conflit ? "unavailable" : "available");
                            send(reply);
                            return;
                        }
                        else if (contenu.startsWith("reserve_time_slot:")) {
                            String[] parts = contenu.split(":", 3);
                            String description = parts[1];
                            long timeMillis = Long.parseLong(parts[2]);
                            Date date = new Date(timeMillis);

                            Event blockingEvent = new Event("Tâche: " + description, date);
                            if (checkConflit(blockingEvent)) {
                                reply.setContent("conflit");
                            } else {
                                listeEvenements.add(blockingEvent);
                                saveEvents();
                                reply.setContent("ok");
                            }
                            send(reply);
                            return;
                        }
                        else if (contenu.startsWith("planifie_evenement:")) {
                            String[] parts = contenu.split(":", 3);
                            if (parts.length < 2) {
                                throw new IllegalArgumentException("Description ou date manquante");
                            }
                            String description = parts[1].trim();
                            if (description.isEmpty()) {
                                throw new IllegalArgumentException("Description vide");
                            }
                            String dateTimeStr = parts.length > 2 ? parts[2].trim() : "2025-12-31;15:00";
                            dateTimeStr = dateTimeStr.replaceAll("[\\u00A0\\u200B\\uFEFF]", " ").replaceAll("[-–—]", "-");

                            if (dateTimeStr.contains(" ") && !dateTimeStr.contains(";")) {
                                dateTimeStr = dateTimeStr.replaceFirst(" ", ";");
                                LOGGER.info("Converted space to semicolon: '" + dateTimeStr + "'");
                            }

                            LOGGER.info("Attempting to parse dateStr: '" + dateTimeStr + "'");
                            LOGGER.info("dateStr bytes: " + Arrays.toString(dateTimeStr.getBytes()));

                            Date date;
                            try {
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd;HH:mm", Locale.US);
                                sdf.setLenient(false);
                                try {
                                    date = sdf.parse(dateTimeStr);
                                } catch (ParseException e) {
                                    sdf = new SimpleDateFormat("yyyy-MM-dd;H:mm", Locale.US);
                                    sdf.setLenient(false);
                                    date = sdf.parse(dateTimeStr);
                                }
                            } catch (ParseException e) {
                                try {
                                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);
                                    sdf.setLenient(false);
                                    date = sdf.parse(dateTimeStr.replace(";", " "));
                                } catch (ParseException e2) {
                                    throw new IllegalArgumentException("Format de date invalide. Utilisez yyyy-MM-dd;HH:mm ou yyyy-MM-dd;H:mm (ex. 2025-06-01;15:00 ou 2025-06-01;9:00)");
                                }
                            }
                            Event evenement = new Event(description, date);
                            if (checkConflit(evenement)) {
                                reply.setContent("⚠️ Conflit détecté pour \"" + description + "\". Proposez une autre date.");
                            } else {
                                listeEvenements.add(evenement);
                                saveEvents();
                                reply.setContent("✅ Événement planifié : " + evenement);
                            }
                        } else if (contenu.startsWith("supprime_evenement:")) {
                            String description = contenu.split(":", 2)[1].trim();
                            boolean removed = listeEvenements.removeIf(e -> e.getDescription().equalsIgnoreCase(description));
                            if (removed) {
                                saveEvents();
                                reply.setContent("✅ Événement supprimé : " + description);
                            } else {
                                reply.setContent("❌ Événement non trouvé : " + description);
                            }
                        } else if (contenu.equals("liste_evenements")) {
                            if (listeEvenements.isEmpty()) {
                                reply.setContent("Liste des événements:\nAucun événement planifié.");
                            } else {
                                StringBuilder sb = new StringBuilder("Liste des événements:\n");
                                for (Event e : listeEvenements) {
                                    sb.append("- ").append(e).append("\n");
                                }
                                reply.setContent(sb.toString());
                            }
                            LOGGER.info("Sent event list: " + reply.getContent());
                        } else {
                            reply.setContent("❌ Commande inconnue. Essayez : planifie_evenement:<événement>:<yyyy-MM-dd;HH:mm>, supprime_evenement:<événement>, liste_evenements");
                        }
                    } catch (IllegalArgumentException e) {
                        LOGGER.warning("Erreur lors du traitement de la commande : " + contenu + " - " + e.getMessage());
                        reply.setContent("Erreur : " + e.getMessage());
                    } catch (Exception e) {
                        LOGGER.warning("Erreur inattendue lors du traitement de la commande : " + contenu + " - " + e.getMessage());
                        e.printStackTrace();
                        reply.setContent("Erreur : " + e.getMessage());
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

    private void saveEvents() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.writeValue(file, listeEvenements);
            LOGGER.info("Événements sauvegardés dans events.json");
        } catch (Throwable t) {
            LOGGER.severe("Erreur sauvegarde événements : " + t.getMessage());
            t.printStackTrace();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("client", AID.ISLOCALNAME));
            msg.setContent("Erreur sauvegarde événements : " + t.getMessage());
            send(msg);
        }
    }

    private void loadEvents() {
        try {
            if (file.exists()) {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                List<Event> loadedEvents = mapper.readValue(file, mapper.getTypeFactory().constructCollectionType(List.class, Event.class));
                listeEvenements.addAll(loadedEvents);
                LOGGER.info("Loaded " + loadedEvents.size() + " events from events.json");
            } else {
                LOGGER.info("Aucun fichier events.json trouvé, démarrage avec une liste vide.");
            }
        } catch (Throwable t) {
            LOGGER.severe("Erreur chargement événements : " + t.getMessage());
            t.printStackTrace();
            ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
            msg.addReceiver(new AID("client", AID.ISLOCALNAME));
            msg.setContent("Erreur chargement événements : " + t.getMessage());
            send(msg);
        }
    }
}
*/








