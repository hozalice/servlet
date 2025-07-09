package model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnexionMySQL {

    // Paramètres de connexion
    private static final String URL = "jdbc:mysql://localhost:3306/_717809c284485191";
    private static final String UTILISATEUR = "root";
    private static final String MOT_DE_PASSE = "hozalice";

    /**
     * Fonction pour se connecter à la base de données MySQL
     * @return une instance de Connection si la connexion réussit, sinon null
     */
    public static Connection connect() {
        try {
            // Charger le pilote JDBC
            Class.forName("org.mariadb.jdbc.Driver");

            // Établir et retourner la connexion
            return DriverManager.getConnection(URL, UTILISATEUR, MOT_DE_PASSE);
        } catch (ClassNotFoundException e) {
            System.err.println("Erreur : Pilote JDBC non trouvé.");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Erreur SQL : " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    // Test de la connexion
    public static void main(String[] args) {
        Connection connexion = connect();
        if (connexion != null) {
            System.out.println("Connexion réussie à la base de données MySQL !");
            try {
                connexion.close();
            } catch (SQLException e) {
                System.err.println("Erreur lors de la fermeture de la connexion.");
                e.printStackTrace();
            }
        } else {
            System.out.println("Échec de la connexion à la base de données MySQL.");
        }
    }
}
