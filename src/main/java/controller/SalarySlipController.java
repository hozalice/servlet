package controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/Salary-Slip")
public class SalarySlipController extends HttpServlet {

    private static final String API_BASE_URL = "http://erpnext.localhost:8000/api/resource/Salary%20Slip";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try {
            String sid = (String) req.getSession().getAttribute("sid");
            if (sid == null) {
                resp.sendRedirect("index.jsp");
                return;
            }

            // Encodage correct du paramètre fields
            String fieldsParam = "[\"name\",\"employee_name\",\"posting_date\",\"start_date\",\"end_date\",\"status\",\"net_pay\",\"gross_pay\"]";
            String encodedFields = URLEncoder.encode(fieldsParam, StandardCharsets.UTF_8.toString());

            // Construction de l'URL complète
            URL url = new URL(API_BASE_URL + "?fields=" + encodedFields);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Cookie", "sid=" + sid);

            int responseCode = conn.getResponseCode();
            StringBuilder response = new StringBuilder();

            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 200 && responseCode < 300 ?
                            conn.getInputStream() : conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            if (responseCode >= 200 && responseCode < 300) {
                JsonReader reader = new JsonReader(new java.io.StringReader(response.toString()));
                reader.setLenient(true);

                JsonParser parser = new JsonParser();
                JsonElement jsonElement = parser.parse(reader);

                JsonObject fiche_employers = jsonElement.getAsJsonObject();

                // Passer les données JSON à la JSP
                req.setAttribute("fiche_employerData", fiche_employers.toString());
                req.getRequestDispatcher("fiche_employer.jsp").forward(req, resp);
            } else {
                String errorMessage = "Error: " + responseCode;
                try {
                    JsonReader reader = new JsonReader(new java.io.StringReader(response.toString()));
                    reader.setLenient(true);
                    JsonParser parser = new JsonParser();
                    JsonElement jsonElement = parser.parse(reader);

                    if (jsonElement.isJsonObject()) {
                        JsonObject jsonObj = jsonElement.getAsJsonObject();
                        if (jsonObj.has("data")) {
                            errorMessage = "Employee data: " + jsonObj.get("data").toString();
                        } else if (jsonObj.has("message")) {
                            errorMessage = jsonObj.get("message").getAsString();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                resp.setStatus(responseCode);
                resp.getWriter().write(errorMessage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Server error: " + e.getMessage());
        }
    }
}
