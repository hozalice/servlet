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
import java.time.LocalDate;
import java.time.YearMonth;

    @WebServlet("/Salary-Slip")
    public class SalarySlipController extends HttpServlet {

        private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/Salary%20Slip";
        private static final String API_FICHE_EMP = "http://172.25.36.0:8000/api/resource/Employee";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            resp.setContentType("text/html;charset=UTF-8");
            PrintWriter out = resp.getWriter();

            try {
                String sid = (String) req.getSession().getAttribute("sid");
                if (sid == null) {
                    resp.sendRedirect("index.jsp");
                    return;
                }

                String employer = req.getParameter("employer");
                String mois = req.getParameter("mois");
                String annee = req.getParameter("annee");

                String fieldsParam = "[\"name\",\"employee\",\"employee_name\",\"posting_date\",\"start_date\",\"end_date\",\"status\",\"net_pay\",\"gross_pay\"]";
                String filtre;
                if ((mois == null || mois.isEmpty()) || (annee == null || annee.isEmpty())) {
                    filtre = "[[\"employee\",\"=\",\"" + employer + "\"]]";
                } else {
                    int month = Integer.parseInt(mois);
                    int year = Integer.parseInt(annee);
                    LocalDate start = LocalDate.of(year, month, 1);
                    LocalDate end = YearMonth.of(year, month).atEndOfMonth();

                    filtre = "[" +
                            "[\"employee\",\"=\",\"" + employer + "\"]," +
                            "[\"start_date\",\">=\",\"" + start + "\"]," +
                            "[\"start_date\",\"<=\",\"" + end + "\"]" +
                            "]";
                }

                String encodedFields = URLEncoder.encode(fieldsParam, StandardCharsets.UTF_8);
                String encodedFilters = URLEncoder.encode(filtre, StandardCharsets.UTF_8);

                // API pour Salary Slip
                String apiUrl = API_BASE_URL + "?fields=" + encodedFields + "&filters=" + encodedFilters;

                // API pour récupérer les infos de l'employé
                String apiemployer = API_FICHE_EMP + "/" + employer;

                // Connexion pour Salary Slip
                JsonObject fiche_employers = getJsonFromApi(apiUrl, sid);
                // Connexion pour Employer
                JsonObject employerData = getJsonFromApi(apiemployer, sid);

                // Extraire la biographie
                String biographie = "";
                if (employerData != null && employerData.has("data")) {
                    JsonObject dataObject = employerData.getAsJsonObject("data");
                    if (dataObject.has("biography") && !dataObject.get("biography").isJsonNull()) {
                        biographie = dataObject.get("biography").getAsString();
                    }
                }

                req.setAttribute("biographie", biographie);
                req.setAttribute("fiche_employerData", fiche_employers.toString());
                req.getRequestDispatcher("fiche_employer.jsp").forward(req, resp);

            } catch (Exception e) {
                e.printStackTrace();
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write("Erreur serveur : " + e.getMessage());
            }
        }

    // Méthode utilitaire pour appeler une API et retourner un JsonObject
    private JsonObject getJsonFromApi(String urlString, String sid) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sid);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("User-Agent", "Java-Client");

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
            return jsonElement.getAsJsonObject();
        } else {
            return null;
        }
    }
}
