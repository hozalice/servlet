package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.google.gson.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@WebServlet("/graphe")
public class GrapheController extends HttpServlet {

    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/Salary%20Slip";

    // Fonction utilitaire pour faire un appel GET avec le cookie de session
    private String callApiWithSessionCookie(String urlStr, String sessionId) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sessionId);

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    // Calculer les totaux par mois et par composant pour gains et déductions
    private JsonObject calculateTotalsParMois(JsonArray salarySlips) {
        Map<String, Map<String, Double>> earningsByMonth = new HashMap<>();
        Map<String, Map<String, Double>> deductionsByMonth = new HashMap<>();

        for (JsonElement slipElem : salarySlips) {
            JsonObject slip = slipElem.getAsJsonObject();

            // Extraire la date (start_date) au format YYYY-MM-DD
            String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : null;
            if (startDate == null) continue;

            // Extraire l'année et le mois "YYYY-MM"
            String month = startDate.length() >= 7 ? startDate.substring(0, 7) : startDate;

            // Gains
            if (slip.has("earnings") && slip.get("earnings").isJsonArray()) {
                JsonArray earnings = slip.getAsJsonArray("earnings");
                Map<String, Double> earningMap = earningsByMonth.getOrDefault(month, new HashMap<>());

                for (JsonElement earningElem : earnings) {
                    JsonObject earning = earningElem.getAsJsonObject();
                    String component = earning.has("salary_component") ? earning.get("salary_component").getAsString() : "unknown";
                    double amount = earning.has("amount") ? earning.get("amount").getAsDouble() : 0.0;
                    earningMap.put(component, earningMap.getOrDefault(component, 0.0) + amount);
                }
                earningsByMonth.put(month, earningMap);
            }

            // Déductions
            if (slip.has("deductions") && slip.get("deductions").isJsonArray()) {
                JsonArray deductions = slip.getAsJsonArray("deductions");
                Map<String, Double> deductionMap = deductionsByMonth.getOrDefault(month, new HashMap<>());

                for (JsonElement deductionElem : deductions) {
                    JsonObject deduction = deductionElem.getAsJsonObject();
                    String component = deduction.has("salary_component") ? deduction.get("salary_component").getAsString() : "unknown";
                    double amount = deduction.has("amount") ? deduction.get("amount").getAsDouble() : 0.0;
                    deductionMap.put(component, deductionMap.getOrDefault(component, 0.0) + amount);
                }
                deductionsByMonth.put(month, deductionMap);
            }
        }

        // Convertir les maps en JsonObject
        JsonObject result = new JsonObject();

        JsonObject earningsJson = new JsonObject();
        for (String month : earningsByMonth.keySet()) {
            JsonObject monthJson = new JsonObject();
            for (Map.Entry<String, Double> entry : earningsByMonth.get(month).entrySet()) {
                monthJson.addProperty(entry.getKey(), entry.getValue());
            }
            earningsJson.add(month, monthJson);
        }

        JsonObject deductionsJson = new JsonObject();
        for (String month : deductionsByMonth.keySet()) {
            JsonObject monthJson = new JsonObject();
            for (Map.Entry<String, Double> entry : deductionsByMonth.get(month).entrySet()) {
                monthJson.addProperty(entry.getKey(), entry.getValue());
            }
            deductionsJson.add(month, monthJson);
        }

        result.add("earningsByMonth", earningsJson);
        result.add("deductionsByMonth", deductionsJson);

        return result;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            String anneeStr = req.getParameter("annee");
            Integer annee = null;

            if (anneeStr != null && !anneeStr.trim().isEmpty()) {
                annee = Integer.parseInt(anneeStr.trim());
            }

            HttpSession session = req.getSession();
            String sid = (String) session.getAttribute("sid");

            if (sid == null) {
                resp.sendRedirect("index.jsp");
                return;
            }

            String listUrl;
            if (annee != null) {
                String startDate = annee + "-01-01";
                String endDate = annee + "-12-31";
                String filtersJson = "[[\"start_date\", \">=\", \"" + startDate + "\"], [\"start_date\", \"<=\", \"" + endDate + "\"]]";
                String filters = URLEncoder.encode(filtersJson, StandardCharsets.UTF_8);
                listUrl = API_BASE_URL + "?limit_page_length=0&fields=[\"name\"]&filters=" + filters;
            } else {
                listUrl = API_BASE_URL + "?limit_page_length=0&fields=[\"name\"]";
            }

            String listResponse = callApiWithSessionCookie(listUrl, sid);
            JsonObject listJson = JsonParser.parseString(listResponse).getAsJsonObject();
            JsonArray dataArray = listJson.getAsJsonArray("data");

            JsonArray resultDetails = new JsonArray();

            for (JsonElement elem : dataArray) {
                String name = elem.getAsJsonObject().get("name").getAsString();
                String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replace("+", "%20");

                String detailUrl = API_BASE_URL + "/" + encodedName;
                String detailResponse = callApiWithSessionCookie(detailUrl, sid);
                JsonObject detailJson = JsonParser.parseString(detailResponse).getAsJsonObject().getAsJsonObject("data");

                resultDetails.add(detailJson);
            }

            // Calculer les totaux par mois
            JsonObject totalResult = calculateTotalsParMois(resultDetails);

            // Passer les données à la JSP
            req.setAttribute("allsalaryslip", new Gson().toJson(resultDetails));
            req.setAttribute("totals", totalResult.toString());
            System.out.println(totalResult.toString());
            req.getRequestDispatcher("/graphe-salaire.jsp").forward(req, resp);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des fiches de paie.");
            } catch (Exception ignored) {}
        }
    }
}
