package controller;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import model.ConnexionMySQL;
import model.NewTbaleModel;

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
import java.sql.Connection;
import java.sql.Date;
import java.text.DateFormat;
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
            String ecrase = req.getParameter("ecraser");
            String moyenne = req.getParameter("moyenne");
            String salaireParam = req.getParameter("salaire");

            System.out.println("[DEBUG] Paramètres reçus : employer=" + employer + ", anarana=" + emplyername
                    + ", start_date=" + startStr + ", end_date=" + endStr + ", ecraser=" + ecrase + ", moyenne="
                    + moyenne + ", salaire=" + salaireParam);

            // Détermination de la base de salaire à utiliser
            String base;
            if ("1".equals(moyenne)) {
                base = String.valueOf(getAverageBase(req));
                System.out.println("[DEBUG] Moyenne des bases utilisée : " + base);
            } else if (salaireParam != null && !salaireParam.isEmpty()) {
                base = salaireParam;
                System.out.println("[DEBUG] Base de salaire fournie utilisée : " + base);
            } else {
                base = String.valueOf(getSalaireplusproche(employer, startStr, req));
                System.out.println("[DEBUG] Salaire le plus proche utilisé : " + base);
            }
            Connection connection = ConnexionMySQL.connect();
            NewTbaleModel[] lespourcentage = NewTbaleModel.getAll(connection);

            String salarystructure = getsalarystructure(employer, startStr, req);
            System.out.println("[DEBUG] Structure de salaire récupérée : " + salarystructure);

            boolean allSuccess = true;
            LocalDate current = LocalDate.parse(startStr, dateFormatter);
            LocalDate endDate = LocalDate.parse(endStr, dateFormatter);

            while (!current.isAfter(endDate)) {
                String formattedDate = current.format(dateFormatter);
                System.out.println("[DEBUG] Traitement du mois : " + formattedDate);

                // Si ecraser=1, annuler+supprimer les slips existants pour ce mois avant de
                // régénérer
                if ("1".equals(ecrase)) {
                    String startDateStr = current.withDayOfMonth(1).toString();
                    String endDateStr = current.withDayOfMonth(current.lengthOfMonth()).toString();
                    // --- Suppression Salary Slip (déjà présent) ---
                    String filter = "[[\"employee\",\"=\",\"" + employer + "\"],"
                            + "[\"start_date\",\">=\",\"" + startDateStr + "\"],"
                            + "[\"end_date\",\"<=\",\"" + endDateStr + "\"]]";
                    String encodedfields = URLEncoder.encode("[\"name\"]", StandardCharsets.UTF_8).replace("+", "%20");
                    String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8).replace("+", "%20");
                    String urlCheck = API_BASE_URL + "Salary%20Slip?fields=" + encodedfields + "&filters="
                            + encodedFilter;
                    System.out.println("[DEBUG] Suppression des Salary Slip existants pour le mois : " + urlCheck);
                    HttpRequest reqCheck = HttpRequest.newBuilder().uri(URI.create(urlCheck))
                            .header("Cookie", "sid=" + sid).GET().build();
                    HttpResponse<String> resCheck = httpClient.send(reqCheck, HttpResponse.BodyHandlers.ofString());
                    if (resCheck.statusCode() == 200) {
                        JsonArray slips = gson.fromJson(resCheck.body(), JsonObject.class).getAsJsonArray("data");
                        for (JsonElement el : slips) {
                            String slipName = el.getAsJsonObject().get("name").getAsString();
                            System.out.println("[DEBUG] Annulation du Salary Slip : " + slipName);
                            cancelDocument("Salary Slip", slipName, sid);
                            System.out.println("[DEBUG] Suppression du Salary Slip : " + slipName);
                            deleteDocument("Salary Slip", slipName, sid);
                        }
                        // Vérification de la suppression effective (max 3 essais)
                        int essais = 0;
                        boolean encorePresent = true;
                        while (essais < 3 && encorePresent) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                            }
                            HttpResponse<String> resVerif = httpClient.send(reqCheck,
                                    HttpResponse.BodyHandlers.ofString());
                            if (resVerif.statusCode() == 200) {
                                JsonArray slipsVerif = gson.fromJson(resVerif.body(), JsonObject.class)
                                        .getAsJsonArray("data");
                                if (slipsVerif.size() == 0) {
                                    encorePresent = false;
                                    System.out
                                            .println("[DEBUG] Tous les Salary Slip ont bien été supprimés pour le mois "
                                                    + formattedDate);
                                } else {
                                    System.out.println("[DEBUG] Il reste encore " + slipsVerif.size()
                                            + " Salary Slip à supprimer (essai " + (essais + 1) + ")");
                                }
                            }
                            essais++;
                        }
                        if (encorePresent) {
                            System.out.println(
                                    "[DEBUG] Attention : certains Salary Slip n'ont pas pu être supprimés après 3 essais.");
                        }
                    } else {
                        System.out.println("[DEBUG] Erreur lors de la récupération des Salary Slip à supprimer : "
                                + resCheck.body());
                    }

                    // --- Suppression Salary Structure Assignment pour la période ---
                    String filterSSA = "[[\"employee\",\"=\",\"" + employer + "\"],"
                            + "[\"from_date\",\"=\",\"" + formattedDate + "\"]]";
                    String encodedfieldsSSA = URLEncoder.encode("[\"name\"]", StandardCharsets.UTF_8).replace("+",
                            "%20");
                    String encodedFilterSSA = URLEncoder.encode(filterSSA, StandardCharsets.UTF_8).replace("+", "%20");
                    String urlSSA = API_BASE_URL + "Salary%20Structure%20Assignment?fields=" + encodedfieldsSSA
                            + "&filters=" + encodedFilterSSA;
                    System.out.println(
                            "[DEBUG] Suppression des Salary Structure Assignment existants pour le mois : " + urlSSA);
                    HttpRequest reqSSA = HttpRequest.newBuilder().uri(URI.create(urlSSA))
                            .header("Cookie", "sid=" + sid).GET().build();
                    HttpResponse<String> resSSA = httpClient.send(reqSSA, HttpResponse.BodyHandlers.ofString());
                    if (resSSA.statusCode() == 200) {
                        JsonArray ssaList = gson.fromJson(resSSA.body(), JsonObject.class).getAsJsonArray("data");
                        for (JsonElement el : ssaList) {
                            String ssaName = el.getAsJsonObject().get("name").getAsString();
                            System.out.println("[DEBUG] Annulation du Salary Structure Assignment : " + ssaName);
                            cancelDocument("Salary Structure Assignment", ssaName, sid);
                            System.out.println("[DEBUG] Suppression du Salary Structure Assignment : " + ssaName);
                            deleteDocument("Salary Structure Assignment", ssaName, sid);
                        }
                    } else {
                        System.out.println(
                                "[DEBUG] Erreur lors de la récupération des Salary Structure Assignment à supprimer : "
                                        + resSSA.body());
                    }
                }
                double basefinal = 0;

                // On (ré)assigne la structure de salaire et on génère le Salary Slip
                boolean result = assignerStructureSalairecheckbase(employer, formattedDate, salarystructure, base, sid);
                boolean resultgenererpaie = GenererSlipSalaire(employer, emplyername, salarystructure, current, "1",
                        sid);

                if (result) {
                    out.println("✔ Insertion structure réussie pour le mois : " + formattedDate);
                    System.out.println("[DEBUG] Insertion structure réussie pour le mois : " + formattedDate);
                } else {
                    out.println("❌ Échec de l'insertion structure pour le mois : " + formattedDate);
                    System.out.println("[DEBUG] Échec de l'insertion structure pour le mois : " + formattedDate);
                    allSuccess = false;
                }
                if (resultgenererpaie) {
                    out.println("✔ Insertion salary slip réussie pour le mois : " + formattedDate);
                    System.out.println("[DEBUG] Insertion salary slip réussie pour le mois : " + formattedDate);
                } else {
                    out.println("❌ Échec de l'insertion salary slip pour le mois : " + formattedDate);
                    System.out.println("[DEBUG] Échec de l'insertion salary slip pour le mois : " + formattedDate);
                    allSuccess = false;
                }

                current = current.plusMonths(1);
            }

            if (allSuccess) {
                out.println("✅ Toutes les insertions ont réussi.");
                System.out.println("[DEBUG] Toutes les insertions ont réussi.");
            } else {
                out.println("⚠ Certaines insertions ont échoué.");
                System.out.println("[DEBUG] Certaines insertions ont échoué.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            out.println("Erreur : " + e.getMessage());
            System.out.println("[DEBUG] Exception attrapée : " + e.getMessage());
        }
    }

    private double getSalaireplusproche(String employer, String date, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        String encodedDocType = URLEncoder.encode("Salary Structure Assignment", StandardCharsets.UTF_8).replace("+",
                "%20");
        String fields = URLEncoder
                .encode("[\"employee\",\"salary_structure\",\"from_date\",\"base\"]", StandardCharsets.UTF_8)
                .replace("+", "%20");
        String filters = URLEncoder
                .encode("[[\"employee\",\"=\",\"" + employer + "\"],[\"from_date\",\"<=\",\"" + date + "\"]]",
                        StandardCharsets.UTF_8)
                .replace("+", "%20");

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
            } catch (NumberFormatException ignored) {
            }
        }

        return base;
    }

    private double getAverageBase(HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        // On ne filtre plus sur l'employé, on prend tous les Salary Structure
        // Assignment
        String encodedDocType = URLEncoder.encode("Salary Structure Assignment", StandardCharsets.UTF_8).replace("+",
                "%20");
        String fields = URLEncoder
                .encode("[\"employee\",\"salary_structure\",\"from_date\",\"base\"]", StandardCharsets.UTF_8)
                .replace("+", "%20");
        // Pas de filtre sur l'employé
        String url = API_BASE_URL + encodedDocType + "?fields=" + fields;

        JsonObject data = callApi(url, sid);
        JsonArray slips = data.getAsJsonArray("data");

        double base = 0;
        int count = 0;

        for (JsonElement slipElement : slips) {
            JsonObject slip = slipElement.getAsJsonObject();
            if (slip.has("base")) {
                base += slip.get("base").getAsDouble();
                count++;
            }
        }

        if (count == 0) {
            System.out.println("[DEBUG] Aucune base trouvée pour le calcul de la moyenne globale.");
            return 0;
        }
        double moyenne = base / count;
        System.out.println(
                "[DEBUG] Moyenne globale des bases de salaire : " + moyenne + " (" + count + " bases trouvées)");
        return moyenne;
    }

    private String getsalarystructure(String employer, String date, HttpServletRequest request) throws IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        String encodedDocType = URLEncoder.encode("Salary Structure Assignment", StandardCharsets.UTF_8).replace("+",
                "%20");
        String fields = URLEncoder.encode("[\"employee\",\"salary_structure\"]", StandardCharsets.UTF_8).replace("+",
                "%20");
        String filters = URLEncoder.encode("[[\"employee\",\"=\",\"" + employer + "\"]]", StandardCharsets.UTF_8)
                .replace("+", "%20");

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

    private boolean assignerStructureSalaire(String employeeName, String date, String structure, String base,
            String sid) {
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

    private boolean GenererSlipSalaire(String employe, String employeeName, String structure, LocalDate datepaie,
            String status, String sid) throws Exception {
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

    private boolean assignerStructureSalairecheckbase(String employeeName,
                                                      String date,
                                                      String structure,
                                                      String base,
                                                      String sid) throws Exception {
        System.out.println("-------------------------------------------------------------------------");
        System.out.println(employeeName);
        System.out.println(date);
        System.out.println(structure);
        System.out.println(base);

        Connection connection = ConnexionMySQL.connect();
        NewTbaleModel[] pourcentagemois = NewTbaleModel.getAll(connection);
        String basefinal = "";

        // Convertir la date reçue en paramètre en LocalDate
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate dateParam = LocalDate.parse(date, formatter);
        double newbase = 0.0;
        for (int i = 0; i < pourcentagemois.length; i++) {
            LocalDate datePourcentage = pourcentagemois[i].getDaty().toLocalDate();
            double pourcentage = pourcentagemois[i].getPourcentage();
            double basedouble = Double.parseDouble(base);
            String signe = pourcentagemois[i].getSigne();

            if (signe.equals("+")){
                 newbase = basedouble + (basedouble * pourcentage) / 100;
            } else if (signe.equals("-")) {
                 newbase = basedouble - (basedouble * pourcentage) / 100;
            }


            System.out.println("datepourcentage -------> " + datePourcentage +
                    "   datemois---------> " + dateParam +
                    "   new base-------->" + newbase);

            // Comparaison sur le mois et l'année uniquement
            if (datePourcentage.getMonthValue() == dateParam.getMonthValue() &&
                    datePourcentage.getYear() == dateParam.getYear()) {
                basefinal = String.valueOf(newbase);
            }
        }

        try {
            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Salary Structure Assignment");
            data.addProperty("employee", employeeName);
            data.addProperty("salary_structure", structure);
            data.addProperty("from_date", date);
            data.addProperty("base", basefinal);

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

    private void cancelDocument(String docType, String docName, String sid) throws IOException, InterruptedException {
        JsonObject body = new JsonObject();
        body.addProperty("docstatus", 2);
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName);
        System.out.println("cancelDocument URL: " + url);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(body.toString())).build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("cancelDocument status: " + res.statusCode());
    }

    private String encodeURIComponent(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20")
                    .replaceAll("\\%21", "!")
                    .replaceAll("\\%27", "'")
                    .replaceAll("\\%28", "(")
                    .replaceAll("\\%29", ")")
                    .replaceAll("\\%7E", "~");
        } catch (Exception e) {
            return s;
        }
    }

    private void deleteDocument(String docType, String docName, String sid) throws IOException, InterruptedException {
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName);
        System.out.println("deleteDocument URL: " + url);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).DELETE().build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("deleteDocument status: " + res.statusCode());
    }

}