package utils;

import models.Utilisateur;
import java.io.*;

public class GestionnaireStockage {
    private static final String STORAGE_PATH = "data/users/";

    public GestionnaireStockage() {
        File dir = new File(STORAGE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public void sauvegarderUtilisateur(Utilisateur utilisateur) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(STORAGE_PATH + utilisateur.getNomUtilisateur() + ".txt"))) {
            writer.write(utilisateur.getNomUtilisateur() + "," + utilisateur.getMotDePasse() + "," + utilisateur.getIdUtilisateur());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Utilisateur chargerUtilisateur(String nomUtilisateur) {
        try (BufferedReader reader = new BufferedReader(new FileReader(STORAGE_PATH + nomUtilisateur + ".txt"))) {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split(",");
                return new Utilisateur(parts[0], parts[1], Integer.parseInt(parts[2]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}