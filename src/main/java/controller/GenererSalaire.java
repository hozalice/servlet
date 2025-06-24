package controller;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@WebServlet("/Generate")
public class GenererSalaire extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final HttpClient httpClient;
    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/";
    private final Gson gson;
    private final DateTimeFormatter dateFormatter;

    public GenererSalaire() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
        PrintWriter out = resp.getWriter();
        HttpSession session = req.getSession();
        String sid = (String) session.getAttribute("sid");

        try {
            String employer = req.getParameter("employee");
            String emplyername = req.getParameter("anarana");
            String startStr = req.getParameter("start_date");
            String endStr = req.getParameter("end_date");

            String base;
            if (req.getParameter("salaire") != null && !req.getParameter("salaire").isEmpty()) {
                base = req.getParameter("salaire");
            } else {
                base = String.valueOf(getSalaireplusproche(employer, startStr, req));
            }

            String salarystructure = getsalarystructure(employer, startStr, req);

            boolean allSuccess = true;
            LocalDate current = LocalDate.parse(startStr, dateFormatter);
            LocalDate endDate = LocalDate.parse(endStr, dateFormatter);

            while (!current.isAfter(endDate)) {
                String formattedDate = current.format(dateFormatter);
                boolean result = assignerStructureSalaire(employer, formattedDate, salarystructure, base, sid);
                boolean resultgenererpaie = GenererSlipSalaire(employer,emplyername,salarystructure,current,"1",sid);

                if (result) {
                    out.println("✔ Insertion réussie pour le mois : " + formattedDate);
                } else {
                    out.println("❌ Échec de l'insertion pour le mois : " + formattedDate);
                    allSuccess = false;
                }
                if (resultgenererpaie){
                    out.println("✔ Insertion slary slip réussie pour le mois : " + formattedDate);
                }else {
                    out.println("❌ Échec de l'insertion salary slip pour le mois : " + formattedDate);
                    allSuccess = false;
                }

                current = current.plusMonths(1);
            }

            if (allSuccess) {
                out.println("✅ Toutes les insertions ont réussi.");
            } else {
                out.println("⚠ Certaines insertions ont échoué.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("Erreur : " + e.getMessage());
        }
    }

    private double getSalaireplusproche(String employer, String date, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        String encodedDocType = URLEncoder.encode("Salary Structure Assignment", StandardCharsets.UTF_8).replace("+", "%20");
        String fields = URLEncoder.encode("[\"employee\",\"salary_structure\",\"from_date\",\"base\"]", StandardCharsets.UTF_8).replace("+", "%20");
        String filters = URLEncoder.encode("[[\"employee\",\"=\",\"" + employer + "\"],[\"from_date\",\"<=\",\"" + date + "\"]]", StandardCharsets.UTF_8).replace("+", "%20");

        String url = API_BASE_URL + encodedDocType + "?fields=" + fields + "&filters=" + filters;

        JsonObject data = callApi(url, sid);
        JsonArray slips = data.getAsJsonArray("data");

        double base = 0;
        List<JsonObject> validSlips = new ArrayList<>();

        for (JsonElement slipElement : slips) {
            JsonObject slip = slipElement.getAsJsonObject();
            if (slip.has("from_date") && slip.has("base")) {
                validSlips.add(slip);
            }
        }

        validSlips.sort(Comparator.comparing(slip -> LocalDate.parse(slip.get("from_date").getAsString())));

        if (!validSlips.isEmpty()) {
            JsonObject closest = validSlips.get(validSlips.size() - 1);
            try {
                base = Double.parseDouble(closest.get("base").getAsString());
            } catch (NumberFormatException ignored) {}
        }

        return base;
    }

    private String getsalarystructure(String employer, String date, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        String encodedDocType = URLEncoder.encode("Salary Structure Assignment", StandardCharsets.UTF_8).replace("+", "%20");
        String fields = URLEncoder.encode("[\"employee\",\"salary_structure\"]", StandardCharsets.UTF_8).replace("+", "%20");
        String filters = URLEncoder.encode("[[\"employee\",\"=\",\"" + employer + "\"]]", StandardCharsets.UTF_8).replace("+", "%20");

        String url = API_BASE_URL + encodedDocType + "?fields=" + fields + "&filters=" + filters;

        JsonObject data = callApi(url, sid);
        JsonArray slips = data.getAsJsonArray("data");

        String salarystructure = "";
        for (JsonElement slipElement : slips) {
            JsonObject slip = slipElement.getAsJsonObject();
            if (slip.has("salary_structure")) {
                salarystructure = slip.get("salary_structure").getAsString();
            }
        }
        return salarystructure;
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
            return gson.fromJson(response.toString(), JsonObject.class);
        } else {
            throw new IOException("Erreur API " + responseCode + ": " + response.toString());
        }
    }

    private boolean assignerStructureSalaire(String employeeName, String date, String structure, String base, String sid) {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(employeeName);
        System.out.println(date);
        System.out.println(structure);
        System.out.println(base);
        try {
            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Salary Structure Assignment");
            data.addProperty("employee", employeeName);
            data.addProperty("salary_structure", structure);
            data.addProperty("from_date", date);
            data.addProperty("base", base);

            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "Salary%20Structure%20Assignment"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                System.out.println("Erreur POST: " + responsePost.body());
                return false;
            }

            JsonObject jsonPost = JsonParser.parseString(responsePost.body()).getAsJsonObject();
            String name = jsonPost.getAsJsonObject("data").get("name").getAsString();

            JsonObject submitPayload = new JsonObject();
            submitPayload.addProperty("docstatus", 1);

            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20");

            HttpRequest requestSubmit = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "Salary%20Structure%20Assignment/" + encodedName))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(submitPayload)))
                    .build();

            HttpResponse<String> responseSubmit = httpClient.send(requestSubmit, HttpResponse.BodyHandlers.ofString());

            if (responseSubmit.statusCode() != 200) {
                System.out.println("Erreur PUT: " + responseSubmit.body());
                return false;
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    private boolean GenererSlipSalaire(String employe, String employeeName, String structure, LocalDate datepaie, String status, String sid) throws  Exception {
        System.out.println("----------------------  ---------------------------------------------------");
        System.out.println(employe);
        System.out.println(employeeName);
        System.out.println(structure);
        System.out.println(datepaie);
        System.out.println();

        try {
            // Début et fin du mois avec LocalDate
            LocalDate moisDebut = datepaie.withDayOfMonth(1);
            LocalDate moisFin = datepaie.withDayOfMonth(datepaie.lengthOfMonth());

            // Formater les dates en chaîne (yyyy-MM-dd)
            String startDateStr = moisDebut.toString();
            String endDateStr = moisFin.toString();
            String postingDateStr = LocalDate.now().toString();

            System.out.println("empoyer id : " + employeeName);

            // Vérification si le Salary Slip existe déjà
            String filter = "[[\"employee\",\"=\",\"" + employeeName + "\"]," +
                    "[\"start_date\",\">=\",\"" + startDateStr + "\"]," +
                    "[\"end_date\",\"<=\",\"" + endDateStr + "\"]]";
            String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8).replace("+", "%20");

            String urlCheck = API_BASE_URL + "Salary%20Slip?filters=" + encodedFilter;

            HttpRequest requestGet = HttpRequest.newBuilder()
                    .uri(URI.create(urlCheck))
                    .header("Cookie", "sid=" + sid)
                    .GET()
                    .build();

            HttpResponse<String> responseGet = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());

            if (responseGet.statusCode() != 200) {
                System.out.println("GET Salary Slip failed: " + responseGet.body());
                return false;
            }

            JsonObject jsonGet = JsonParser.parseString(responseGet.body()).getAsJsonObject();
            if (jsonGet.has("data")) {
                JsonArray results = jsonGet.getAsJsonArray("data");
                if (results.size() > 0) {
                    System.out.println("slip de salaire généré avec succès");
                    return true;
                }
            }

            // Création du Salary Slip
            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Salary Slip");
            data.addProperty("employee", employe);
            data.addProperty("employee_name", employeeName);
            data.addProperty("salary_structure", structure);
            data.addProperty("start_date", startDateStr);
            data.addProperty("end_date", endDateStr);
            data.addProperty("posting_date", postingDateStr);
            data.addProperty("docstatus", 1); // Directement soumis

            System.out.println("JSON envoyé: " + gson.toJson(data));
            System.out.println(employeeName);
            System.out.println(employeeName);
            System.out.println(structure);
            System.out.println(startDateStr);
            System.out.println(endDateStr);
            System.out.println(postingDateStr);
            System.out.println("*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*-*");

            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + "Salary%20Slip"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                System.out.println("POST Salary Slip failed: " + responsePost.body());
                return false;
            } else {
                System.out.println("Status code: " + responsePost.statusCode());
                System.out.println("Response body: " + responsePost.body());
            }

            System.out.println("généré avec succès");
            return true;

        } catch (Exception ex) {
            System.out.println("il y a une erreur");
            ex.printStackTrace();
            return false;
        }
    }
}
