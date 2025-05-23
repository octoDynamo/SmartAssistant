package main;

public class Main {
    public static void main(String[] args) {
        jade.Boot.main(new String[]{
                "-gui",
                "client:agents.ClientAgent;" +
                        "agentTaches:agents.AgentTaches;" +
                        "agentRecherche:agents.AgentRecherche;" +
                        "agentEvenements:agents.AgentEvenements"
        });
    }
}