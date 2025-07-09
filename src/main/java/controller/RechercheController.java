package controller;

import com.google.gson.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@WebServlet("/recherche")
public class RechercheController extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private static final Logger LOGGER = Logger.getLogger(UpdateController.class.getName());

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sid = (String) req.getSession().getAttribute("sid");
        try {
            JsonArray salaryComponents = fetchAllSalaryComponents(sid);
            System.out.println("GET salary components count = " + salaryComponents.size());
            req.setAttribute("salarie_component", salaryComponents);
            req.getRequestDispatcher("/page_recherche.jsp").forward(req, resp);
        } catch (Exception e) {
            LOGGER.severe("Erreur récupération composants : " + e.getMessage());
            sendJsonError(resp, "Erreur récupération composants : " + e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sid = (String) req.getSession().getAttribute("sid");
        try {
            String componentName = req.getParameter("salary_component");
            String condition = req.getParameter("condition");
            String amountStr = req.getParameter("amount");
            if (componentName == null || condition == null || amountStr == null
                    || componentName.isBlank() || condition.isBlank() || amountStr.isBlank()) {
                throw new IllegalArgumentException("Paramètres manquants ou invalides");
            }
            double amount = Double.parseDouble(amountStr);
            // Appel direct de la fonction qui filtre côté API
            JsonArray matchedSlips = getSalarySlipsByComponentCondition(componentName, condition, amount, sid);
            req.setAttribute("matchedSlips", matchedSlips);
            req.getRequestDispatcher("/salarylist.jsp").forward(req, resp);
        } catch (Exception e) {
            LOGGER.severe("Erreur POST : " + e.getMessage());
            req.setAttribute("errorMessage", e.getMessage());
            try {
                req.getRequestDispatcher("/salarylist.jsp").forward(req, resp);
            } catch (Exception ex) {
                throw new IOException("Erreur lors du forward vers salarylist.jsp : " + ex.getMessage());
            }
        }
    }

    private JsonArray fetchAllSalaryComponents(String sid) throws IOException, InterruptedException {
        String fields = URLEncoder.encode("[\"name\",\"salary_component\",\"type\"]", StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Component?fields=" + fields;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .header("Accept", "application/json")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("fetchAllSalaryComponents status : " + response.statusCode());
        if (response.statusCode() != 200)
            throw new IOException("Erreur récupération Salary Components");
        return gson.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");
    }

    /**
     * Récupère tous les Salary Slip validés qui contiennent le composant de salaire
     * donné
     * respectant la condition (> < =) sur le montant, filtrage côté Java.
     */
    private JsonArray getSalarySlipsByComponentCondition(String salaryComponent, String condition, double amount,
            String sid) throws IOException, InterruptedException {
        // 1. Récupérer tous les Salary Slip validés
        String fields = URLEncoder.encode("[\"name\"]", StandardCharsets.UTF_8);
        String filters = URLEncoder.encode("[[\"docstatus\",\"=\",1]]", StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Slip?fields=" + fields + "&filters=" + filters;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            System.out.println("[DEBUG] Erreur API Salary Slip : " + response.statusCode() + " - " + response.body());
            throw new IOException("Erreur récupération Salary Slips filtrés");
        }
        JsonArray array = gson.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");
        JsonArray result = new JsonArray();
        for (JsonElement e : array) {
            JsonObject slip = e.getAsJsonObject();
            String slipName = slip.get("name").getAsString();
            JsonObject fullSlip = getDocument("Salary Slip", slipName, sid);
            if (fullSlip == null)
                continue;
            if (salarySlipHasComponentCondition(fullSlip, salaryComponent, condition, amount)) {
                result.add(fullSlip);
            }
        }
        return result;
    }

    // Fonctions utilitaires pour le filtrage côté Java
    private boolean salarySlipHasComponentCondition(JsonObject slip, String salaryComponent, String condition,
            double amount) {
        JsonArray earnings = slip.getAsJsonArray("earnings");
        JsonArray deductions = slip.getAsJsonArray("deductions");
        return componentArrayMatches(earnings, salaryComponent, condition, amount)
                || componentArrayMatches(deductions, salaryComponent, condition, amount);
    }

    private boolean componentArrayMatches(JsonArray comps, String salaryComponent, String condition, double amount) {
        if (comps == null)
            return false;
        for (JsonElement e : comps) {
            JsonObject comp = e.getAsJsonObject();
            if (!comp.has("salary_component") || comp.get("salary_component").isJsonNull())
                continue;
            String name = comp.get("salary_component").getAsString();
            if (!name.equalsIgnoreCase(salaryComponent))
                continue;
            if (!comp.has("amount") || comp.get("amount").isJsonNull())
                continue;
            double val = comp.get("amount").getAsDouble();
            switch (condition) {
                case ">":
                    if (val > amount)
                        return true;
                    break;
                case "<":
                    if (val < amount)
                        return true;
                    break;
                case "=":
                    if (val == amount)
                        return true;
                    break;
            }
        }
        return false;
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

    private void sendJsonError(HttpServletResponse resp, String message) throws IOException {
        try (PrintWriter out = resp.getWriter()) {
            out.print("{\"success\": false, \"error\":\"" + message + "\"}");
        }
    }

    // Fonction utilitaire pour récupérer un document ERPNext par type et nom
    private JsonObject getDocument(String docType, String docName, String sid)
            throws IOException, InterruptedException {
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName);
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Accept", "application/json").GET().build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() != 200) {
            System.err.println(
                    "Échec récupération document : " + docType + "/" + docName + " - Status: " + res.statusCode());
            return null;
        }
        return gson.fromJson(res.body(), JsonObject.class).getAsJsonObject("data");
    }
}
