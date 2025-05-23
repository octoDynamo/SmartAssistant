import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class InterfaceGraphique extends JFrame {
    private static JTextArea zoneReponse;
    private JTextField champCommande;
    private Agent agent;

    public InterfaceGraphique(Agent agent) {
        this.agent = agent;
        setTitle("SmartAssistant - Interface Graphique");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        champCommande = new JTextField();
        zoneReponse = new JTextArea();
        zoneReponse.setEditable(false);

        JButton boutonEnvoyer = new JButton("Envoyer");
        boutonEnvoyer.addActionListener(this::envoyerCommande);

        JPanel panneauHaut = new JPanel(new BorderLayout());
        panneauHaut.add(new JLabel("Commande : "), BorderLayout.WEST);
        panneauHaut.add(champCommande, BorderLayout.CENTER);
        panneauHaut.add(boutonEnvoyer, BorderLayout.EAST);

        add(panneauHaut, BorderLayout.NORTH);
        add(new JScrollPane(zoneReponse), BorderLayout.CENTER);

        setVisible(true);
    }

    private void envoyerCommande(ActionEvent e) {
        String commande = champCommande.getText().trim();
        if (commande.isEmpty()) return;

        ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
        msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));

        if (commande.startsWith("ajoute ")) {
            msg.setContent("ajoute_tache:" + commande.substring(7));
        } else if (commande.startsWith("supprime ")) {
            msg.setContent("supprime_tache:" + commande.substring(9));
        } else if (commande.equals("liste")) {
            msg.setContent("liste_taches");
        } else {
            afficherReponse("Commande inconnue. Essayez : ajoute <tâche>, supprime <tâche>, liste");
            return;
        }

        agent.send(msg);
        champCommande.setText("");
    }

    public static void afficherReponse(String message) {
        SwingUtilities.invokeLater(() -> {
            zoneReponse.append(message + "\n");
        });
    }
}









/*public class TerminalInterface implements Runnable {
    private Agent agent;

    public TerminalInterface(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void run() {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("Commande: ");
            String ligne = sc.nextLine();

            ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
            msg.addReceiver(new AID("agentTaches", AID.ISLOCALNAME));

            if (ligne.startsWith("ajoute ")) {
                msg.setContent("ajoute_tache:" + ligne.substring(7));
            } else if (ligne.startsWith("supprime ")) {
                msg.setContent("supprime_tache:" + ligne.substring(9));
            } else if (ligne.startsWith("liste")) {
                msg.setContent("liste_taches");
            } else {
                System.out.println("Commande inconnue.");
                continue;
            }

            agent.send(msg);
        }
    }
}*/
