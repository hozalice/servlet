package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@WebServlet("/Salary-Slip")
public class SalarySlipController extends HttpServlet {
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
            String employeeId = request.getParameter("employer");
            if (employeeId == null || employeeId.trim().isEmpty()) {
                throw new Exception("L'identifiant de l'employé est requis");
            }

            // Récupérer les informations de l'employé
            String employeeUrl = API_BASE_URL + "Employee/" + URLEncoder.encode(employeeId, StandardCharsets.UTF_8);
            JsonObject employeeData = callApi(employeeUrl, sid);

            if (employeeData == null || !employeeData.has("data")) {
                throw new Exception("Employé non trouvé");
            }

            JsonObject employee = employeeData.getAsJsonObject("data");

            // Récupérer les fiches de paie de l'employé
            String salarySlipsUrl = API_BASE_URL + "Salary%20Slip?fields=" +
                    URLEncoder.encode("[\"name\",\"start_date\",\"end_date\",\"net_pay\",\"gross_pay\",\"status\"]",
                            StandardCharsets.UTF_8)
                    +
                    "&filters="
                    + URLEncoder.encode("[[\"employee\",\"=\",\"" + employeeId + "\"]]", StandardCharsets.UTF_8);

            JsonObject salarySlipsData = callApi(salarySlipsUrl, sid);
            // Préparer les données pour la JSP
            request.setAttribute("employee", employee.toString());
            
            // Vérifier si les données de salaire sont valides
            if (salarySlipsData != null && salarySlipsData.has("data")) {
                JsonArray slips = salarySlipsData.getAsJsonArray("data");
                request.setAttribute("salarySlips", slips.toString());
            } else {
                request.setAttribute("salarySlips", "[]");
            }
            
            // Transférer à la JSP
            request.getRequestDispatcher("/fiche_employer.jsp").forward(request, response);

        } catch (Exception e) {
            e.printStackTrace();
            request.setAttribute("error", "Erreur: " + e.getMessage());
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }

    private JsonObject callApi(String urlString, String sid) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sid);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Java-Client");

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

        if (responseCode >= 200 && responseCode < 300) {
            return new Gson().fromJson(response.toString(), JsonObject.class);
        } else {
            System.err.println("Erreur API " + responseCode + ": " + response.toString());
            return null;
        }
    }
}
