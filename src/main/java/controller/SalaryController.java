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
import java.util.HashMap;
import java.util.Map;

import com.google.gson.*;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/salaries")
public class SalaryController extends HttpServlet {
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

        // Récupérer les filtres mois et année depuis les paramètres GET
        String monthParam = request.getParameter("month"); // ex: "3" pour mars
        String yearParam = request.getParameter("year");   // ex: "2025"

        try {
            // Récupérer tous les employés
            String employeeUrl = API_BASE_URL + "Employee?fields=" +
                    URLEncoder.encode("[\"name\",\"employee_name\",\"employee_number\"]", StandardCharsets.UTF_8);
            JsonObject employeesData = callApi(employeeUrl, sid);

            JsonArray employees = employeesData.has("data") ? employeesData.getAsJsonArray("data") : new JsonArray();
            JsonArray allSalarySlips = new JsonArray();

            // Pour chaque employé, récupérer les fiches de paie
            for (JsonElement emp : employees) {
                JsonObject employee = emp.getAsJsonObject();
                String empId = employee.get("name").getAsString();

                // Construire URL des fiches de paie filtrées
                String salarySlipsUrl = API_BASE_URL + "Salary%20Slip?fields=" +
                        URLEncoder.encode(
                                "[\"name\",\"employee\",\"employee_name\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"status\"]",
                                StandardCharsets.UTF_8)
                        + "&filters=[[\"employee\",\"=\",\"" + empId + "\"]]";

                JsonObject salarySlipsData = callApi(salarySlipsUrl, sid);

                if (salarySlipsData != null && salarySlipsData.has("data")) {
                    JsonArray employeeSlips = salarySlipsData.getAsJsonArray("data");

                    for (JsonElement slip : employeeSlips) {
                        JsonObject slipObj = slip.getAsJsonObject();

                        // Filtrer par mois et année si demandé
                        if (passesDateFilter(slipObj, monthParam, yearParam)) {
                            String slipName = slipObj.has("name") ? slipObj.get("name").getAsString() : "";

                            if (!slipName.isEmpty()) {
                                try {
                                    String encodedSlipName = slipName
                                            .replace(" ", "%20")
                                            .replace("/", "%2F");
                                    String slipDetailsUrl = API_BASE_URL + "Salary%20Slip/" + encodedSlipName;

                                    JsonObject slipDetails = callApi(slipDetailsUrl, sid);
                                    if (slipDetails != null && slipDetails.has("data")) {
                                        JsonObject slipData = slipDetails.getAsJsonObject("data");
                                        if (!slipData.has("earnings")) {
                                            slipData.add("earnings", new JsonArray());
                                        }
                                        if (!slipData.has("deductions")) {
                                            slipData.add("deductions", new JsonArray());
                                        }
                                        allSalarySlips.add(slipData);
                                    } else {
                                        allSalarySlips.add(slipObj);
                                    }
                                } catch (Exception e) {
                                    System.err.println("Erreur récupération détails fiche paie " + slipName + ": " + e.getMessage());
                                    allSalarySlips.add(slipObj);
                                }
                            } else {
                                allSalarySlips.add(slipObj);
                            }
                        }
                    }
                }
            }

            // Grouper les fiches par employé
            JsonObject groupedSalaries = new JsonObject();
            for (JsonElement emp : employees) {
                JsonObject employee = emp.getAsJsonObject();
                String empId = employee.get("name").getAsString();

                JsonArray empSalaries = new JsonArray();
                for (JsonElement slip : allSalarySlips) {
                    JsonObject salary = slip.getAsJsonObject();
                    if (salary.has("employee") && salary.get("employee").getAsString().equals(empId)) {
                        empSalaries.add(salary);
                    }
                }

                if (empSalaries.size() > 0) {
                    groupedSalaries.add(empId, empSalaries);
                }
            }

            // Calculer les totaux
            JsonObject totalsJson = calculateTotals(allSalarySlips);
            System.out.println(totalsJson);
            // Mettre les attributs pour la JSP
            request.setAttribute("employees", employees.toString());
            request.setAttribute("groupedSalaries", groupedSalaries.toString());
            request.setAttribute("allSalarySlips", allSalarySlips.toString());
            request.setAttribute("totalsJson", totalsJson.toString());

            request.getRequestDispatcher("/salaries.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de la récupération des données: " + e.getMessage());
            request.getRequestDispatcher("/salaries.jsp").forward(request, response);
        }
    }

    /**
     * Vérifie si une fiche de paie passe le filtre de mois et année (sur start_date).
     */
    private boolean passesDateFilter(JsonObject slip, String monthParam, String yearParam) {
        if (!slip.has("start_date")) {
            return false; // Sans date on exclut
        }
        String startDateStr = slip.get("start_date").getAsString();

        try {
            LocalDate startDate = LocalDate.parse(startDateStr, DateTimeFormatter.ISO_DATE);

            if (yearParam != null && !yearParam.isEmpty()) {
                int yearFilter = Integer.parseInt(yearParam);
                if (startDate.getYear() != yearFilter) {
                    return false;
                }
            }

            if (monthParam != null && !monthParam.isEmpty()) {
                int monthFilter = Integer.parseInt(monthParam);
                if (startDate.getMonthValue() != monthFilter) {
                    return false;
                }
            }
            return true; // Passe tous les filtres
        } catch (Exception e) {
            // En cas de problème de parsing, exclure
            return false;
        }
    }

    /**
     * Calcule les totaux des montants pour earnings et deductions.
     */
    private JsonObject calculateTotals(JsonArray salarySlips) {
        Map<String, Double> earningsTotals = new HashMap<>();
        Map<String, Double> deductionsTotals = new HashMap<>();

        for (JsonElement slipElem : salarySlips) {
            JsonObject slip = slipElem.getAsJsonObject();

            if (slip.has("earnings") && slip.get("earnings").isJsonArray()) {
                JsonArray earnings = slip.getAsJsonArray("earnings");
                for (JsonElement earningElem : earnings) {
                    JsonObject earning = earningElem.getAsJsonObject();
                    String component = earning.has("salary_component") ? earning.get("salary_component").getAsString() : "unknown";
                    double amount = earning.has("amount") ? earning.get("amount").getAsDouble() : 0.0;
                    earningsTotals.put(component, earningsTotals.getOrDefault(component, 0.0) + amount);
                }
            }

            if (slip.has("deductions") && slip.get("deductions").isJsonArray()) {
                JsonArray deductions = slip.getAsJsonArray("deductions");
                for (JsonElement deductionElem : deductions) {
                    JsonObject deduction = deductionElem.getAsJsonObject();
                    String component = deduction.has("salary_component") ? deduction.get("salary_component").getAsString() : "unknown";
                    double amount = deduction.has("amount") ? deduction.get("amount").getAsDouble() : 0.0;
                    deductionsTotals.put(component, deductionsTotals.getOrDefault(component, 0.0) + amount);

                }
            }
        }

        JsonObject result = new JsonObject();

        JsonObject earningsJson = new JsonObject();
        for (Map.Entry<String, Double> entry : earningsTotals.entrySet()) {
            earningsJson.addProperty(entry.getKey(), entry.getValue());
        }

        JsonObject deductionsJson = new JsonObject();
        for (Map.Entry<String, Double> entry : deductionsTotals.entrySet()) {
            deductionsJson.addProperty(entry.getKey(), entry.getValue());
        }
        //System.out.println("GAIN =  " + earningsJson);
        //System.out.println("DEDUCTION  = " + deductionsJson );
        result.add("earningsTotals", earningsJson);
        result.add("deductionsTotals", deductionsJson);

        return result;
    }

    private JsonObject callApi(String urlString, String sid) throws IOException {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", "sid=" + sid);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Java-Client");
            conn.setConnectTimeout(10000); // 10 secondes
            conn.setReadTimeout(10000); // 10 secondes

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 200 && responseCode < 300 ? conn.getInputStream() : conn.getErrorStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            JsonObject jsonResponse = null;
            if (!response.toString().trim().isEmpty()) {
                try {
                    jsonResponse = new Gson().fromJson(response.toString(), JsonObject.class);
                } catch (JsonSyntaxException e) {
                    throw new IOException(
                            "Erreur parsing JSON: " + e.getMessage() + " - Response: " + response.toString());
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                if (jsonResponse == null) {
                    throw new IOException("Empty response from API: " + urlString);
                }
                return jsonResponse;
            } else {
                throw new IOException("Erreur HTTP " + responseCode + ": " + response.toString());
            }
        } catch (Exception e) {
            throw new IOException("Erreur appel API: " + e.getMessage(), e);
        }
    }
}
