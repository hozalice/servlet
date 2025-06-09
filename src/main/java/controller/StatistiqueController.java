package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import com.google.gson.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

@WebServlet("/statistiques")
public class StatistiqueController extends HttpServlet {
    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        if (sid == null) {
            response.sendRedirect("index.jsp");
            return;
        }

        String anneeParam = request.getParameter("annee");
        Integer annee = (anneeParam != null && !anneeParam.isEmpty()) ? Integer.parseInt(anneeParam) : null;

        try {
            // Champs à récupérer
            String fields = URLEncoder.encode("[\"name\",\"employee\",\"employee_name\",\"start_date\",\"net_pay\",\"earnings\"]", StandardCharsets.UTF_8);

            // URL de base
            String salaryUrl = API_BASE_URL + "Salary%20Slip?fields=" + fields;

            // Ajout du filtre par année si présent
            if (annee != null) {
                String filterJson = String.format("[[\"Salary Slip\",\"start_date\",\">=\",\"%d-01-01\"],[\"Salary Slip\",\"start_date\",\"<=\",\"%d-12-31\"]]", annee, annee);
                salaryUrl += "&filters=" + URLEncoder.encode(filterJson, StandardCharsets.UTF_8);
            }

            // Appel API
            JsonObject salaryData = callApi(salaryUrl, sid);
            JsonArray salarySlips = salaryData.has("data") ? salaryData.getAsJsonArray("data") : new JsonArray();

            // Initialiser résumé mensuel de 1 à 12 avec total=0 et détails vides
            Map<Integer, JsonObject> resumeMensuel = new LinkedHashMap<>();
            for (int mois = 1; mois <= 12; mois++) {
                JsonObject moisData = new JsonObject();
                moisData.addProperty("mois", mois);
                moisData.addProperty("total", 0.0);
                moisData.add("details", new JsonObject());
                resumeMensuel.put(mois, moisData);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            // Parcours des fiches
            for (JsonElement elem : salarySlips) {
                JsonObject slip = elem.getAsJsonObject();
                if (!slip.has("start_date")) continue;

                String dateStr = slip.get("start_date").getAsString();
                LocalDate date = LocalDate.parse(dateStr, formatter);
                int mois = date.getMonthValue();

                JsonObject moisObj = resumeMensuel.get(mois);
                double totalActuel = moisObj.get("total").getAsDouble();
                double netPay = slip.has("net_pay") ? slip.get("net_pay").getAsDouble() : 0.0;
                moisObj.addProperty("total", totalActuel + netPay);

                JsonObject details = moisObj.getAsJsonObject("details");
                if (slip.has("earnings")) {
                    JsonArray earnings = slip.getAsJsonArray("earnings");
                    for (JsonElement e : earnings) {
                        JsonObject earning = e.getAsJsonObject();
                        String type = earning.has("salary_component") ? earning.get("salary_component").getAsString() : "Autre";
                        double amount = earning.has("amount") ? earning.get("amount").getAsDouble() : 0.0;

                        double actuel = details.has(type) ? details.get(type).getAsDouble() : 0.0;
                        details.addProperty(type, actuel + amount);
                    }
                }
            }

            request.setAttribute("annee", (annee != null) ? annee : "Toutes");
            request.setAttribute("resumeMensuel", new Gson().toJson(resumeMensuel));

            request.getRequestDispatcher("/statistiques.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de la récupération des statistiques : " + e.getMessage());
            request.getRequestDispatcher("/statistiques.jsp").forward(request, response);
        }
    }

    private JsonObject callApi(String urlString, String sid) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sid);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Java-Client");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int responseCode = conn.getResponseCode();
        StringBuilder response = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
        }

        if (responseCode >= 200 && responseCode < 300) {
            return new Gson().fromJson(response.toString(), JsonObject.class);
        } else {
            throw new IOException("Erreur API : " + responseCode + " - " + response.toString());
        }
    }
}
