package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import gui.InterfaceGraphique;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgentTaches extends Agent {

    private TaskManager taskManager;
    private InterfaceGraphique gui;

    @Override
    protected void setup() {
        System.out.println("AgentTaches lancé...");
        taskManager = new TaskManager();

        addBehaviour(new TickerBehaviour(this, 1000) {
            @Override
            protected void onTick() {
                ACLMessage msg = receive();
                if (msg != null) {
                    String content = msg.getContent().trim();

                    if (content.toLowerCase().contains("vider") || content.toLowerCase().contains("supprimer tout")) {
                        taskManager.clearTasks();
                        String reponse = "✅ Toutes les tâches ont été supprimées.";
                        sendReply(msg, reponse);
                        if (gui != null) gui.afficherReponse(reponse);

                    } else if (content.contains("liste")) {
                        List<Task> tasks = taskManager.getSortedTasks();
                        StringBuilder sb = new StringBuilder("Liste des tâches :\n");
                        for (Task t : tasks) {
                            String prioStr = switch (t.getPriority()) {
                                case 1 -> "Haute";
                                case 3 -> "Basse";
                                default -> "Moyenne";
                            };
                            sb.append("- ")
                                    .append(t.getDescription())
                                    .append(" (Due: ")
                                    .append(t.getDateTime())
                                    .append(", Priority: ")
                                    .append(prioStr)
                                    .append(")\n");
                        }
                        sendReply(msg, sb.toString());
                        if (gui != null) gui.afficherReponse(sb.toString());
                    } else if (content.toLowerCase().startsWith("ajoute ")) {
                        try {
                            TaskParser.ParsedTask parsed = TaskParser.parseNaturalCommand(content);
                            LocalTime time = parsed.time != null ? parsed.time : askUserForTime();
                            if (time == null) {
                                sendReply(msg, "⏰ Heure manquante. Tâche non ajoutée.");
                                return;
                            }

                            LocalDateTime dateTime = LocalDateTime.of(parsed.date, time);

                            // Vérifier la disponibilité du créneau
                            if (isTimeSlotAvailable(dateTime)) {
                                Task task = new Task(parsed.description, dateTime, parsed.priority);
                                taskManager.addTask(task);

                                // Réserver le créneau
                                reserveTimeSlot(task);

                                String reponse = "✅ Tâche ajoutée :\n" + task;
                                sendReply(msg, reponse);
                                if (gui != null) gui.afficherReponse(reponse);
                            } else {
                                String reponse = "❌ Créneau déjà réservé : " + dateTime + "\nImpossible d'ajouter la tâche.";
                                sendReply(msg, reponse);
                                if (gui != null) gui.afficherReponse(reponse);
                            }
                        } catch (Exception e) {
                            String reponse = "❌ Erreur lors de l'ajout de la tâche : " + e.getMessage();
                            sendReply(msg, reponse);
                            if (gui != null) gui.afficherReponse(reponse);
                        }
                    }
                }
            }
        });

        addBehaviour(new TickerBehaviour(this, 10_000) {
            @Override
            protected void onTick() {
                List<Task> dueSoon = taskManager.getTasksDueSoon();
                LocalDateTime now = LocalDateTime.now();
                for (Task task : dueSoon) {
                    if (task.getDateTime().isBefore(now.plusSeconds(10))) {
                        showPopup(task.getDescription());
                    }
                }
            }
        });
    }

    private boolean isTimeSlotAvailable(LocalDateTime dateTime) {
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());

        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
        msg.setContent("check_time_slot:" + date.getTime());
        send(msg);

        ACLMessage reply = blockingReceive(1000);
        if (reply != null && reply.getPerformative() == ACLMessage.INFORM) {
            return !reply.getContent().contains("unavailable");
        }
        return true;
    }

    private void reserveTimeSlot(Task task) {
        Date date = Date.from(task.getDateTime().atZone(ZoneId.systemDefault()).toInstant());

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentEvenements", AID.ISLOCALNAME));
        msg.setContent("reserve_time_slot:" + task.getDescription() + ":" + date.getTime());
        send(msg);
    }

    public void setGUI(InterfaceGraphique gui) {
        this.gui = gui;
    }

    private void sendReply(ACLMessage request, String content) {
        ACLMessage reply = request.createReply();
        reply.setPerformative(ACLMessage.INFORM);
        reply.setContent(content);
        send(reply);
    }

    private void showPopup(String message) {
        SwingUtilities.invokeLater(() ->
                JOptionPane.showMessageDialog(null, message, "🔔 Rappel de tâche", JOptionPane.INFORMATION_MESSAGE));
    }

    private LocalTime askUserForTime() {
        List<String> hours = new ArrayList<>();
        for (int h = 0; h < 24; h++) {
            for (int m = 0; m < 60; m += 5) {
                hours.add(String.format("%02d:%02d", h, m));
            }
        }

        String selectedTime = (String) JOptionPane.showInputDialog(
                null,
                "🕒 Heure non détectée. Sélectionnez une heure :",
                "Sélection de l'heure",
                JOptionPane.QUESTION_MESSAGE,
                null,
                hours.toArray(),
                hours.get(0)
        );

        if (selectedTime != null && selectedTime.matches("\\d{2}:\\d{2}")) {
            String[] parts = selectedTime.split(":");
            return LocalTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return null;
    }

    public static class Task {
        private String description;
        private LocalDateTime dateTime;
        private int priority;

        public Task() {}
        public Task(String description, LocalDateTime dateTime, int priority) {
            this.description = description;
            this.dateTime = dateTime;
            this.priority = priority;
        }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public LocalDateTime getDateTime() { return dateTime; }
        public void setDateTime(LocalDateTime dateTime) { this.dateTime = dateTime; }

        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }

        @Override
        public String toString() {
            String prioStr = switch (priority) {
                case 1 -> "Haute";
                case 3 -> "Basse";
                default -> "Moyenne";
            };
            return "- " + description + " (Due: " + dateTime + ", Priority: " + prioStr + ")";
        }
    }

    public static class TaskManager {
        private final List<Task> tasks = new ArrayList<>();
        private final File file = new File("tasks.json");

        public TaskManager() {
            loadTasks();
        }

        public void addTask(Task task) {
            tasks.add(task);
            saveTasks();
        }

        public void clearTasks() {
            tasks.clear();
            saveTasks();
        }

        public List<Task> getSortedTasks() {
            return tasks.stream()
                    .sorted(Comparator.comparing(Task::getDateTime).thenComparing(Task::getPriority))
                    .collect(Collectors.toList());
        }

        public List<Task> getTasksDueSoon() {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime soon = now.plusMinutes(5);
            return tasks.stream()
                    .filter(t -> t.getDateTime() != null && !t.getDateTime().isBefore(now) && !t.getDateTime().isAfter(soon))
                    .collect(Collectors.toList());
        }

        private void saveTasks() {
            try {
                ObjectMapper mapper = new ObjectMapper();
                mapper.writeValue(file, tasks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void loadTasks() {
            try {
                if (file.exists()) {
                    ObjectMapper mapper = new ObjectMapper();
                    CollectionType type = mapper.getTypeFactory().constructCollectionType(List.class, Task.class);
                    tasks.addAll(mapper.readValue(file, type));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class TaskParser {
        public static ParsedTask parseNaturalCommand(String text) {
            // Supprime "ajoute" et ses variantes au début
            String cleaned = text.replaceFirst("(?i)^ajoute( une)? (tâche|tache)?( pour)?", "").trim();

            // Extrait la priorité et nettoie
            int priority = extractPriority(cleaned);
            cleaned = cleanPriorityKeywords(cleaned);

            // Extrait l'heure (doit être fait avant cleanDescription)
            LocalTime time = extractTime(cleaned);

            // Nettoie la description (doit être fait après extractTime)
            String description = cleanDescription(cleaned);

            // Date par défaut : aujourd'hui
            LocalDate date = extractDate(cleaned);
            if (date == null) {
                date = LocalDate.now();
            }

            return new ParsedTask(description, date, time, priority);
        }

        public static LocalTime extractTime(String text) {
            Pattern p = Pattern.compile("\\b(\\d{1,2})(?:[h:])(\\d{2})\\b|\\b(\\d{1,2})[hH]\\b");
            Matcher m = p.matcher(text);
            if (m.find()) {
                if (m.group(3) != null) {
                    return LocalTime.of(Integer.parseInt(m.group(3)), 0);
                }
                return LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            }
            return null;
        }

        public static LocalDate extractDate(String text) {
            Pattern p = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b|\\b(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})\\b");
            Matcher m = p.matcher(text);
            if (m.find()) {
                if (m.group(4) != null) {
                    return LocalDate.of(Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Integer.parseInt(m.group(6)));
                }
                return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            }
            return null;
        }

        public static String cleanDescription(String text) {
            // Supprime les heures (comme "18:00")
            String cleaned = text.replaceAll("\\b\\d{1,2}[h:]\\d{2}\\b", "");

            // Supprime les dates
            cleaned = cleaned.replaceAll("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b", "");
            cleaned = cleaned.replaceAll("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b", "");

            // Supprime les mots-clés résiduels
            cleaned = cleaned.replaceAll("(?i)\\b(ajoute|tâche|tache|pour|demain|aujourd'hui)\\b", "");

            return cleaned.trim();
        }

        private static String cleanPriorityKeywords(String text) {
            return text.replaceAll("(?i)\\b(urgent(e)?|important(e)?|pas important(e)?|peu important|sans importance)\\b", "").trim();
        }

        public static int extractPriority(String text) {
            String lowered = text.toLowerCase()
                    .replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a");

            if (lowered.matches(".*\\b(pas important|peu important|sans importance)\\b.*")) {
                return 3;
            }
            if (lowered.matches(".*\\b(urgent|très important|priorité haute)\\b.*")) {
                return 1;
            }
            if (lowered.matches(".*\\b(important|moyen)\\b.*")) {
                return 2;
            }
            return 2; // Priorité moyenne par défaut
        }

        public static class ParsedTask {
            public final String description;
            public final LocalDate date;
            public final LocalTime time;
            public final int priority;

            public ParsedTask(String description, LocalDate date, LocalTime time, int priority) {
                this.description = description;
                this.date = date;
                this.time = time;
                this.priority = priority;
            }
        }
    }
}