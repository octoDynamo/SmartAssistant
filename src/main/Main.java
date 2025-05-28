package main;

import agents.AgentCoordination;
import agents.ClientAgent;
import agents.AgentTaches;
import agents.AgentEvenements;
import agents.AgentRecherche;
import agents.AgentConnexion;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

public class Main {
    public static void main(String[] args) {
        Runtime rt = Runtime.instance();
        Profile p = new ProfileImpl();
        p.setParameter(Profile.MAIN_HOST, "localhost");
        p.setParameter(Profile.MAIN_PORT, "1099");
        p.setParameter(Profile.GUI, "true");

        try {
            AgentContainer mc = rt.createMainContainer(p);

            AgentCoordination coordination = new AgentCoordination();
            AgentConnexion connexion = new AgentConnexion();

            // Save a test user
            connexion.enregistrerUtilisateur("testuser", "testpass");

            // Create and start ClientAgent with coordination
            Object[] clientArgs = new Object[]{coordination};
            AgentController clientAgentController = mc.createNewAgent("client", ClientAgent.class.getName(), clientArgs);
            clientAgentController.start();

            AgentController agentTaches = mc.createNewAgent("agentTaches", AgentTaches.class.getName(), null);
            agentTaches.start();

            AgentController agentRecherche = mc.createNewAgent("agentRecherche", AgentRecherche.class.getName(), null);
            agentRecherche.start();

            AgentController agentEvenements = mc.createNewAgent("agentEvenements", AgentEvenements.class.getName(), null);
            agentEvenements.start();

        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }
}