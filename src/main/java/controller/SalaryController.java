package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        try {
            // Récupérer tous les employés
            String employeeUrl = API_BASE_URL + "Employee?fields=" +
                    URLEncoder.encode("[\"name\",\"employee_name\",\"employee_number\"]", StandardCharsets.UTF_8);
            JsonObject employeesData = callApi(employeeUrl, sid);

            // Récupérer les données des employés
            JsonArray employees = employeesData.has("data") ? employeesData.getAsJsonArray("data") : new JsonArray();
            JsonArray allSalarySlips = new JsonArray();

            // Pour chaque employé, récupérer les fiches de paie détaillées
            for (JsonElement emp : employees) {
                JsonObject employee = emp.getAsJsonObject();
                String empId = employee.get("name").getAsString();

                // Récupérer les fiches de paie pour cet employé
                String salarySlipsUrl = API_BASE_URL + "Salary%20Slip?fields=" +
                        URLEncoder.encode(
                                "[\"name\",\"employee\",\"employee_name\",\"start_date\",\"end_date\",\"gross_pay\",\"net_pay\",\"status\"]",
                                StandardCharsets.UTF_8)
                        +
                        "&filters=[[\"employee\",\"=\",\"" + empId + "\"]]";

                JsonObject salarySlipsData = callApi(salarySlipsUrl, sid);
                if (salarySlipsData != null && salarySlipsData.has("data")) {
                    JsonArray employeeSlips = salarySlipsData.getAsJsonArray("data");

                    // Pour chaque fiche de paie, récupérer les détails complets
                    for (JsonElement slip : employeeSlips) {
                        JsonObject slipObj = slip.getAsJsonObject();
                        String slipName = slipObj.has("name") ? slipObj.get("name").getAsString() : "";

                        if (!slipName.isEmpty()) {
                            try {
                                // Récupérer les détails complets de la fiche de paie
                                // Remplacer les espaces par %20 et les / par %2F
                                String encodedSlipName = slipName
                                        .replace(" ", "%20") // Remplacer les espaces d'abord
                                        .replace("/", "%2F"); // Puis les slashes
                                String slipDetailsUrl = API_BASE_URL + "Salary%20Slip/" + encodedSlipName;

                                JsonObject slipDetails = callApi(slipDetailsUrl, sid);
                                if (slipDetails != null && slipDetails.has("data")) {
                                    JsonObject slipData = slipDetails.getAsJsonObject("data");
                                    // S'assurer que les tableaux existent
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
                                // En cas d'erreur, ajouter les données de base
                                System.err.println("Erreur lors de la récupération des détails pour " + slipName + ": "
                                        + e.getMessage());
                                allSalarySlips.add(slipObj);
                            }
                        } else {
                            allSalarySlips.add(slipObj);
                        }
                    }
                }
            }

            // Grouper les fiches de paie par employé
            JsonArray salarySlips = allSalarySlips;

            // Grouper les fiches de paie par employé
            JsonObject groupedSalaries = new JsonObject();
            for (JsonElement emp : employees) {
                JsonObject employee = emp.getAsJsonObject();
                String empId = employee.get("name").getAsString();

                JsonArray empSalaries = new JsonArray();
                for (JsonElement slip : salarySlips) {
                    JsonObject salary = slip.getAsJsonObject();
                    if (salary.has("employee") && salary.get("employee").getAsString().equals(empId)) {
                        empSalaries.add(salary);
                    }
                }

                if (empSalaries.size() > 0) {
                    groupedSalaries.add(empId, empSalaries);
                }
            }

            // Ajouter les données à la requête
            request.setAttribute("employees", employees.toString());
            request.setAttribute("groupedSalaries", groupedSalaries.toString());
            request.setAttribute("allSalarySlips", salarySlips.toString());

            // Transférer à la JSP
            request.getRequestDispatcher("/salaries.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur lors de la récupération des données: " + e.getMessage());
            request.getRequestDispatcher("/salaries.jsp").forward(request, response);
        }
    }

    private JsonObject callApi(String urlString, String sid) throws IOException {
        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", "sid=" + sid);
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Java-Client");
            conn.setConnectTimeout(10000); // 10 secondes de timeout
            conn.setReadTimeout(10000); // 10 secondes de timeout

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
                            "Erreur de parsing JSON: " + e.getMessage() + " - Réponse: " + response.toString());
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                if (jsonResponse == null) {
                    throw new IOException("Réponse vide de l'API pour l'URL: " + urlString);
                }
                return jsonResponse;
            } else {
                String errorMsg = "Erreur API: " + responseCode + " - " + urlString;
                if (jsonResponse != null) {
                    errorMsg += " - " + jsonResponse.toString();
                } else {
                    errorMsg += " - Réponse: " + response.toString();
                }
                throw new IOException(errorMsg);
            }
        } catch (IOException e) {
            throw new IOException("Erreur lors de l'appel à l'API: " + urlString + " - " + e.getMessage(), e);
        }
    }
}