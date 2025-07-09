package model;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class NewTbaleModel {
    private Date daty;
    private double pourcentage;
    private String signe;

    // --- Getters et Setters ---
    public Date getDaty() {
        return daty;
    }

    public void setDaty(Date daty) {
        this.daty = daty;
    }

    public double getPourcentage() {
        return pourcentage;
    }

    public void setPourcentage(double pourcentage) {
        this.pourcentage = pourcentage;
    }

    public String getSigne() {
        return signe;
    }

    public void setSigne(String signe) {
        this.signe = signe;
    }

    // --- Constructeurs ---
    public NewTbaleModel() {}

    public NewTbaleModel(Date daty, double pourcentage, String signe) {
        this.daty = daty;
        this.pourcentage = pourcentage;
        this.signe = signe;
    }

    // --- CREATE : insérer une nouvelle ligne ---
    public boolean insert(Connection conn) throws Exception {
        String sql = "INSERT INTO date_pourcentage (daty, pourcentage, signe) VALUES (?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDate(1, this.daty);
        stmt.setDouble(2, this.pourcentage);
        stmt.setString(3, this.signe);
        return stmt.executeUpdate() > 0;
    }

    // --- READ : lire toutes les lignes ---
    public static NewTbaleModel[] getAll(Connection conn) throws Exception {
        String sql = "SELECT * FROM date_pourcentage ORDER BY daty DESC";
        PreparedStatement stmt = conn.prepareStatement(sql);
        ResultSet rs = stmt.executeQuery();

        ArrayList<NewTbaleModel> list = new ArrayList<>();
        while (rs.next()) {
            Date daty = rs.getDate("daty");
            double pourcentage = rs.getDouble("pourcentage");
            String signe = rs.getString("signe");
            list.add(new NewTbaleModel(daty, pourcentage, signe));
        }

        rs.close();
        stmt.close();

        return list.toArray(new NewTbaleModel[0]);
    }

    // --- READ (par date) ---
    public static NewTbaleModel getByDate(Connection conn, Date date) throws Exception {
        String sql = "SELECT * FROM date_pourcentage WHERE daty = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDate(1, date);
        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            return new NewTbaleModel(
                    rs.getDate("daty"),
                    rs.getDouble("pourcentage"),
                    rs.getString("signe")
            );
        }

        return null;
    }

    // --- UPDATE (modifier un enregistrement existant) ---
    public boolean update(Connection conn) throws Exception {
        String sql = "UPDATE date_pourcentage SET pourcentage = ?, signe = ? WHERE daty = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, this.pourcentage);
        stmt.setString(2, this.signe);
        stmt.setDate(3, this.daty);
        return stmt.executeUpdate() > 0;
    }

    // --- DELETE (supprimer un enregistrement) ---
    public boolean delete(Connection conn) throws Exception {
        String sql = "DELETE FROM date_pourcentage WHERE daty = ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDate(1, this.daty);
        return stmt.executeUpdate() > 0;
    }

    // --- SEARCH (rechercher par intervalle de pourcentage) ---
    public static NewTbaleModel[] searchByPourcentageRange(Connection conn, double min, double max) throws Exception {
        String sql = "SELECT * FROM date_pourcentage WHERE pourcentage BETWEEN ? AND ?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setDouble(1, min);
        stmt.setDouble(2, max);

        ResultSet rs = stmt.executeQuery();
        ArrayList<NewTbaleModel> list = new ArrayList<>();
        while (rs.next()) {
            list.add(new NewTbaleModel(
                    rs.getDate("daty"),
                    rs.getDouble("pourcentage"),
                    rs.getString("signe")
            ));
        }

        return list.toArray(new NewTbaleModel[0]);
    }

    // --- MAIN (exemple de test) ---
    public static void main(String[] args) throws Exception {
        Connection conn = ConnexionMySQL.connect();

        // Exemple d'insertion
        NewTbaleModel model = new NewTbaleModel(Date.valueOf("2024-01-01"), 15.5, "+");
        boolean inserted = model.insert(conn);
        System.out.println("Inséré ? " + inserted);

        // Lecture de tous les enregistrements
        NewTbaleModel[] all = NewTbaleModel.getAll(conn);
        for (NewTbaleModel item : all) {
            System.out.println(item.getDaty() + " - " + item.getPourcentage() + " " + item.getSigne());
        }

        conn.close();
    }
}
