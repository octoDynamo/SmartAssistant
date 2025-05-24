package agents;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import gui.InterfaceGraphique;
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
                    String content = msg.getContent().trim().toLowerCase();

                    if (content.contains("vider") || content.contains("supprimer tout")) {
                        taskManager.clearTasks();
                        String reponse = "✅ Toutes les tâches ont été supprimées.";
                        sendReply(msg, reponse);
                        gui.afficherReponse(reponse);

                    } else if (content.contains("liste")) {
                        List<Task> tasks = taskManager.getSortedTasks();
                        StringBuilder sb = new StringBuilder("📋 Liste des tâches :\n");
                        for (Task t : tasks) sb.append(t).append("\n");
                        sendReply(msg, sb.toString());
                        gui.afficherReponse(sb.toString());

                    } else {
                        // Analyse intelligente du message
                        LocalDate date = TaskParser.extractDate(content);
                        LocalTime time = TaskParser.extractTime(content);
                        int priority = TaskParser.extractPriority(content);
                        String desc = TaskParser.cleanDescription(content);

                        if (date == null) date = LocalDate.now();

                        if (time == null) {
                            // 🎯 Si l'heure n’est pas détectée, demander via une boîte de dialogue
                            time = askUserForTime();
                            if (time == null) {
                                String reponse = "⏰ Heure manquante ou annulée. Tâche non ajoutée.";
                                sendReply(msg, reponse);
                                gui.afficherReponse(reponse);
                                return;
                            }
                        }

                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        Task task = new Task(desc, dateTime, priority);
                        taskManager.addTask(task);

                        String reponse = "✅ Tâche ajoutée :\n" + task;
                        sendReply(msg, reponse);
                        gui.afficherReponse(reponse);
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
            for (int m = 0; m < 60; m++) {
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

    // --- Classe Task ---
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
            return "📌 " + description + "\n🕒 " + dateTime + " | 🔺 Priorité: " + prioStr;
        }
    }

    // --- Classe TaskManager ---
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

    // --- Classe TaskParser ---
    public static class TaskParser {

        public static LocalTime extractTime(String text) {
            Pattern p = Pattern.compile("\\b(\\d{1,2})[hH:](\\d{2})\\b");
            Matcher m = p.matcher(text);
            if (m.find()) return LocalTime.of(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            return null;
        }

        public static LocalDate extractDate(String text) {
            Pattern p = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b");
            Matcher m = p.matcher(text);
            if (m.find()) return LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));
            return null;
        }

        public static String cleanDescription(String text) {
            String cleaned = text.replaceAll("\\b(\\d{1,2})[hH:](\\d{2})\\b", "");
            cleaned = cleaned.replaceAll("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b", "");
            cleaned = cleaned.replaceAll("(?i)ajoute(r)? une tâche pour", "");
            cleaned = cleaned.replaceAll("(?i)urgent(e)?|pas important(e)?|peu important|sans importance|important", "");
            return cleaned.trim();
        }

        public static int extractPriority(String text) {
            String lowered = text.toLowerCase()
                    .replace("é", "e").replace("è", "e").replace("ê", "e").replace("à", "a");

            if (lowered.contains("pas important") || lowered.contains("pas importante")
                    || lowered.contains("peu important") || lowered.contains("sans importance")) return 3;
            if (lowered.contains("urgent") || lowered.contains("urgente")
                    || lowered.contains("immediat") || lowered.contains("important")) return 1;
            return 2;
        }
    }
}
