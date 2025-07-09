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

@WebServlet("/Update")
public class UpdateController extends HttpServlet {
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
            req.getRequestDispatcher("/UpdateSalaire.jsp").forward(req, resp);
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
            String percentageStr = req.getParameter("percentage");
            System.out.println("Paramètres reçus: componentName=" + componentName + ", condition=" + condition
                    + ", amount=" + amountStr + ", percentage=" + percentageStr);

            if (componentName == null || condition == null || amountStr == null || percentageStr == null
                    || componentName.isBlank() || condition.isBlank() || amountStr.isBlank()
                    || percentageStr.isBlank()) {
                throw new IllegalArgumentException("Paramètres manquants ou invalides");
            }

            double amount = Double.parseDouble(amountStr);
            double changePercent = Double.parseDouble(percentageStr) / 100.0;
            String action = (changePercent < 0) ? "decraise" : "araise";
            changePercent = Math.abs(changePercent);

            List<JsonObject> salarySlips = fetchAllSalarySlips(sid);
            System.out.println("Nombre de bulletins récupérés : " + salarySlips.size());

            int updatedCount = 0;

            for (JsonObject slip : salarySlips) {
                String slipName = slip.get("name").getAsString();
                System.out.println("Traitement bulletin salaire : " + slipName);

                JsonObject fullSlip = getDocument("Salary Slip", slipName, sid);
                if (fullSlip == null) {
                    System.err.println("Bulletin salaire introuvable: " + slipName);
                    continue;
                }

                if (!checkSalarySlipMatchesCondition(fullSlip, componentName, condition, amount)) {
                    System.out.println("Bulletin non concerné par la condition, skip : " + slipName);
                    continue;
                }

                // Annulation et suppression
                // On annule seulement le bulletin pour conserver l'historique
                cancelDocument("Salary Slip", slipName, sid);

                String employee = fullSlip.has("employee") && !fullSlip.get("employee").isJsonNull()
                        ? fullSlip.get("employee").getAsString()
                        : null;
                String postingDate = fullSlip.has("posting_date") && !fullSlip.get("posting_date").isJsonNull()
                        ? fullSlip.get("posting_date").getAsString()
                        : null;

                if (employee == null || postingDate == null) {
                    System.err.println(
                            "Données manquantes dans bulletin salaire: employee ou posting_date null pour " + slipName);
                    continue;
                }

                JsonObject ssa = fetchSalaryStructureAssignmentForEmployee(employee, postingDate, sid);
                if (ssa == null) {
                    System.err.println("Données SSA incomplètes, création annulée pour bulletin: " + slipName);
                    continue;
                }

                String oldSsaName = ssa.get("name").getAsString();
                System.out.println("--------------------------------------------------------------------------------------->"+oldSsaName);

                // Nous ANNULLERONS l'ancienne SSA APRÈS avoir créé et soumis la nouvelle afin
                // qu'il y ait toujours
                // au moins une SSA active pendant la création du Salary Slip

                double oldBase = ssa.has("base") && !ssa.get("base").isJsonNull() ? ssa.get("base").getAsDouble() : 0;
                // Si base n'existe pas, on prend le salaire brut du bulletin comme base de
                // référence
                if (oldBase == 0 && fullSlip.has("gross_pay") && !fullSlip.get("gross_pay").isJsonNull()) {
                    oldBase = fullSlip.get("gross_pay").getAsDouble();
                }
                double newBase = ("araise".equals(action)) ? oldBase + (oldBase * changePercent)
                        : Math.max(0, oldBase - (oldBase * changePercent));

                // Vérifications et compléments pour éviter les valeurs nulles
                String ssaEmployee = (ssa.has("employee") && !ssa.get("employee").isJsonNull()
                        && !ssa.get("employee").getAsString().isBlank())
                                ? ssa.get("employee").getAsString()
                                : employee; // fallback à l'employé du bulletin

                String ssaSalaryStructure = (ssa.has("salary_structure") && !ssa.get("salary_structure").isJsonNull()
                        && !ssa.get("salary_structure").getAsString().isBlank())
                                ? ssa.get("salary_structure").getAsString()
                                : ((fullSlip.has("salary_structure") && !fullSlip.get("salary_structure").isJsonNull())
                                        ? fullSlip.get("salary_structure").getAsString()
                                        : null);

                String ssaCompany = (ssa.has("company") && !ssa.get("company").isJsonNull()
                        && !ssa.get("company").getAsString().isBlank())
                                ? ssa.get("company").getAsString()
                                : ((fullSlip.has("company") && !fullSlip.get("company").isJsonNull())
                                        ? fullSlip.get("company").getAsString()
                                        : null);

                String slipStartDate = (fullSlip.has("start_date") && !fullSlip.get("start_date").isJsonNull())
                        ? fullSlip.get("start_date").getAsString()
                        : postingDate;
                // Calculer le 1er du mois du slip
                String ssaFromDate = slipStartDate.substring(0, 8) + "01";

                // Si une donnée indispensable manque encore, journaliser puis passer au suivant
                if (ssaEmployee == null || ssaSalaryStructure == null || ssaCompany == null || ssaFromDate == null) {
                    System.err.println("Données SSA essentielles toujours manquantes pour l'employé: " + employee
                            + ", arrêt création SSA.");
                    continue;
                }
                    
                // 1. Créer et soumettre la nouvelle SSA AVANT de supprimer l'ancienne
                String newSsaName = createSalaryStructureAssignment(ssaEmployee, ssaSalaryStructure, ssaCompany,
                        ssaFromDate, newBase, sid);

                if (newSsaName == null) {
                    // Gestion du cas DuplicateAssignment : suppression de la SSA existante puis
                    // nouvelle tentative
                    System.err.println("Erreur création nouvelle SSA pour " + employee +"---------------------------"+ newSsaName
                            + ". Tentative de suppression de la SSA existante...");
                    JsonObject existingSSA = querySalaryStructureAssignment(ssaEmployee, ssaFromDate, sid);
                    if (existingSSA != null && existingSSA.has("name")) {
                        String existingName = existingSSA.get("name").getAsString();
                        System.out.println("Annulation de la SSA existante : " + existingName);
                        cancelDocument("Salary Structure Assignment", existingName, sid);
                        System.out.println("Suppression de la SSA existante : " + existingName);
                        deleteDocument("Salary Structure Assignment", existingName, sid);
                        // Attendre la disparition effective de la SSA (max 3 essais)
                        int essais = 0;
                        boolean encorePresent = true;
                        while (essais < 3 && encorePresent) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException ie) {
                            }
                            JsonObject verifSSA = querySalaryStructureAssignment(ssaEmployee, ssaFromDate, sid);
                            if (verifSSA == null) {
                                encorePresent = false;
                                System.out.println("[DEBUG] SSA supprimée pour " + employee);
                            } else {
                                System.out.println("[DEBUG] SSA encore présente pour " + employee + " (essai "
                                        + (essais + 1) + ")");
                            }
                            essais++;
                        }
                        if (encorePresent) {
                            System.err.println(
                                    "[DEBUG] Impossible de supprimer la SSA pour " + employee + " après 3 essais.");
                            continue;
                        }
                        // Nouvelle tentative de création AVEC LE MÊME NOM
                        newSsaName = createSalaryStructureAssignment(ssaEmployee, ssaSalaryStructure, ssaCompany,
                                ssaFromDate, newBase, sid);
                        String newSlipName = createSalarySlip(employee,
                                ssaSalaryStructure,
                                postingDate,
                                slipStartDate,
                                fullSlip.has("end_date") && !fullSlip.get("end_date").isJsonNull()
                                        ? fullSlip.get("end_date").getAsString()
                                        : postingDate,
                                newSsaName,
                                sid);
                        System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<"+newSlipName);
                        submitDocument("Salary Slip",newSlipName,sid);

                        if (newSlipName == null) {
                            System.err.println("Erreur création nouveau bulletin salaire pour " + employee);
                            continue;
                        }
                        else if (newSlipName != null){
                            System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + newSlipName + " employer :  "+employee);
                            continue;
                        }
                        if (newSsaName == null) {
                            System.err.println("Nouvel échec de création de la SSA pour " + employee);
                            continue;
                        }
                    } else {
                        System.err.println("Impossible de trouver la SSA existante à supprimer pour " + employee);
                        continue;
                    }
                }
                // Soumettre la SSA pour qu'elle soit considérée valide lors de la création du
                // Salary Slip
                //submitDocument("Salary Structure Assignment", newSsaName, sid);
                // Vérifier que la SSA est bien active (max 3 essais)
                int essaisActive = 0;
                boolean active = false;
                while (essaisActive < 3 && !active) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ie) {
                    }
                    JsonObject verifSSA = querySalaryStructureAssignment(ssaEmployee, ssaFromDate, sid);
                    if (verifSSA != null && verifSSA.has("docstatus") && verifSSA.get("docstatus").getAsInt() == 1) {
                        active = true;
                        System.out.println("[DEBUG] Nouvelle SSA active pour " + employee);
                    } else {
                        System.out.println("[DEBUG] Nouvelle SSA pas encore active pour " + employee + " (essai "
                                + (essaisActive + 1) + ")");
                    }
                    essaisActive++;
                }
                if (!active) {
                    System.err.println("[DEBUG] Nouvelle SSA non active pour " + employee + " après 3 essais.");
                    continue;
                }

                // SOUMETTRE TOUTES LES SSA BROUILLON POUR CET EMPLOYÉ ET CETTE PÉRIODE AVANT DE
                // CRÉER LE SLIP
                //List<JsonObject> allSSA = fetchAllSSAForEmployeeAndDate(ssaEmployee, ssaFromDate, sid);
                //for (JsonObject ssaObj : allSSA) {
                    //if (ssaObj.has("docstatus") && ssaObj.get("docstatus").getAsInt() == 0) {
                       // String ssaName = ssaObj.get("name").getAsString();
                        //System.out.println("[DEBUG] SSA en brouillon trouvée, soumission : " + ssaName);
                       // submitDocument("Salary Structure Assignment", ssaName, sid);
                    //}
                //}
                cancelDocument("Salary Structure Assignment", oldSsaName, sid);
                // 2. Créer et soumettre le Salary Slip APRÈS la soumission de toutes les SSA
                String newSlipName = createSalarySlip(employee,
                        ssaSalaryStructure,
                        postingDate,
                        slipStartDate,
                        fullSlip.has("end_date") && !fullSlip.get("end_date").isJsonNull()
                                ? fullSlip.get("end_date").getAsString()
                                : postingDate,
                        newSsaName,
                        sid);

                if (newSlipName == null) {
                    System.err.println("Erreur création nouveau bulletin salaire pour " + employee);
                    continue;
                }
                else if (newSlipName != null){
                    System.err.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>" + newSlipName + " employer :  "+employee);
                    continue;
                }
                submitDocument("Salary Slip", newSlipName, sid);

                // 3. Annuler/supprimer l'ancienne SSA seulement après la création du nouveau
                // Salary Slip
                cancelDocument("Salary Structure Assignment", oldSsaName, sid);

                updatedCount++;
                System.out.println("Mise à jour réussie pour bulletin: " + slipName);
            }

            // Forward vers la JSP avec message de succès
            req.setAttribute("successMessage", "Mise à jour terminée. " + updatedCount + " bulletins mis à jour.");
            JsonArray salaryComponents = fetchAllSalaryComponents(sid);
            req.setAttribute("salarie_component", salaryComponents);
            req.getRequestDispatcher("/UpdateSalaire.jsp").forward(req, resp);

        } catch (Exception e) {
            LOGGER.severe("Erreur POST : " + e.getMessage());
            req.setAttribute("errorMessage", e.getMessage());
            try {
                JsonArray salaryComponents = fetchAllSalaryComponents(sid);
                req.setAttribute("salarie_component", salaryComponents);
                req.getRequestDispatcher("/UpdateSalaire.jsp").forward(req, resp);
            } catch (Exception ex) {
                throw new IOException("Erreur lors du forward vers UpdateSalaire.jsp : " + ex.getMessage());
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

    private List<JsonObject> fetchAllSalarySlips(String sid) throws IOException, InterruptedException {
        String fields = URLEncoder.encode("[\"name\"]", StandardCharsets.UTF_8);
        String filters = URLEncoder.encode("[[\"docstatus\",\"=\",1]]", StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Slip?fields=" + fields + "&filters=" + filters;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .header("Accept", "application/json")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("fetchAllSalarySlips status : " + response.statusCode());
        JsonArray array = gson.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");

        List<JsonObject> slips = new ArrayList<>();
        for (JsonElement e : array)
            slips.add(e.getAsJsonObject());
        return slips;
    }

    private JsonObject getDocument(String docType, String docName, String sid)
            throws IOException, InterruptedException {
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName);
        System.out.println("getDocument URL : " + url);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Accept", "application/json").GET().build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("getDocument status : " + res.statusCode());
        if (res.statusCode() != 200) {
            System.err.println(
                    "Échec récupération document : " + docType + "/" + docName + " - Status: " + res.statusCode());
            return null;
        }
        return gson.fromJson(res.body(), JsonObject.class).getAsJsonObject("data");
    }

    private boolean checkSalarySlipMatchesCondition(JsonObject slip, String componentName, String condition,
            double amount) {
        JsonArray earnings = slip.getAsJsonArray("earnings");
        JsonArray deductions = slip.getAsJsonArray("deductions");

        boolean earningsMatch = checkComponentsCondition(earnings, componentName, condition, amount);
        boolean deductionsMatch = checkComponentsCondition(deductions, componentName, condition, amount);

        System.out.println("checkSalarySlipMatchesCondition - Earnings match: " + earningsMatch + ", Deductions match: "
                + deductionsMatch);
        return earningsMatch || deductionsMatch;
    }

    private boolean checkComponentsCondition(JsonArray comps, String componentName, String condition, double amount) {
        if (comps == null)
            return false;
        for (JsonElement e : comps) {
            JsonObject comp = e.getAsJsonObject();
            if (!comp.has("salary_component") || comp.get("salary_component").isJsonNull())
                continue;
            String name = comp.get("salary_component").getAsString();
            if (!name.equalsIgnoreCase(componentName))
                continue;
            if (!comp.has("amount") || comp.get("amount").isJsonNull())
                continue;
            double val = comp.get("amount").getAsDouble();

            boolean result = switch (condition) {
                case ">" -> val > amount;
                case "<" -> val < amount;
                case "=" -> val == amount;
                default -> false;
            };
            System.out.println("checkComponentsCondition : Component = " + name + ", Amount = " + val + ", Condition = "
                    + condition + ", Result = " + result);
            return result;
        }
        return false;
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

    private void deleteDocument(String docType, String docName, String sid) throws IOException, InterruptedException {
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName);
        System.out.println("deleteDocument URL: " + url);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).DELETE().build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("deleteDocument status: " + res.statusCode());
    }

    private String createSalaryStructureAssignment(String employee, String salaryStructure, String company,
            String fromDate, double base, String sid) throws IOException, InterruptedException {
        System.out.println("createSalaryStructureAssignment URL : " + API_BASE_URL + "Salary%20Structure%20Assignment");
        System.out.println("Payload : {\"employee\":\"" + employee + "\",\"salary_structure\":\"" + salaryStructure
                + "\",\"company\":\"" + company + "\",\"from_date\":\"" + fromDate + "\",\"base\":" + base + "}");

        JsonObject obj = new JsonObject();
        obj.addProperty("employee", employee);
        obj.addProperty("salary_structure", salaryStructure);
        obj.addProperty("company", company);
        obj.addProperty("from_date", fromDate);
        obj.addProperty("base", base);
        obj.addProperty("docstatus","1" );

        String url = API_BASE_URL + "Salary%20Structure%20Assignment";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(obj.toString())).build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("createSalaryStructureAssignment status : " + res.statusCode());
        System.out.println("createSalaryStructureAssignment response : " + res.body());

        if (res.statusCode() != 200) {
            return null;
        }
        JsonObject responseData = gson.fromJson(res.body(), JsonObject.class).getAsJsonObject("data");
        return responseData != null && responseData.has("name") ? responseData.get("name").getAsString() : null;
    }

    private String createSalarySlip(String employee, String salaryStructure, String postingDate, String startDate,
            String endDate, String ssa, String sid) throws IOException, InterruptedException {
        JsonObject obj = new JsonObject();
        obj.addProperty("employee", employee);
        obj.addProperty("salary_structure", salaryStructure);
        obj.addProperty("posting_date", postingDate);
        obj.addProperty("start_date", startDate);
        obj.addProperty("end_date", endDate);
        obj.addProperty("salary_structure_assignment", ssa);
        obj.addProperty("docstatus","1" );

        String url = API_BASE_URL + "Salary%20Slip";

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(obj.toString())).build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("createSalarySlip status : " + res.statusCode());
        System.out.println("createSalarySlip response : " + res.body());

        if (res.statusCode() != 200) {
            return null;
        }
        JsonObject responseData = gson.fromJson(res.body(), JsonObject.class).getAsJsonObject("data");
        return responseData != null && responseData.has("name") ? responseData.get("name").getAsString() : null;
    }

    private JsonObject fetchSalaryStructureAssignmentForEmployee(String emp, String postDate, String sid)
            throws IOException, InterruptedException {
        System.out.println("Recherche SSA pour employé: " + emp + " jusqu'à la date: " + postDate);

        // Première tentative : avec filtre date
        JsonObject ssa = querySalaryStructureAssignment(emp, postDate, sid);
        if (ssa != null) {
            System.out.println("SSA trouvée (avec filtre date) : " + ssa);
            return ssa; // même si incomplète, on laissera le fallback dans doPost gérer les champs
                        // manquants
        }

        // Deuxième tentative : sans filtre date
        System.out.println("Aucune SSA trouvée avec filtre date. Recherche sans filtre date...");
        ssa = querySalaryStructureAssignmentWithoutDate(emp, sid);
        if (ssa != null) {
            System.out.println("SSA trouvée (sans filtre date) : " + ssa);
            return ssa;
        }

        System.err.println("Aucune SSA trouvée pour employé " + emp);
        return null;
    }

    private JsonObject querySalaryStructureAssignment(String emp, String postDate, String sid)
            throws IOException, InterruptedException {
        String filters = URLEncoder.encode(
                "[[\"employee\",\"=\",\"" + emp + "\"],[\"from_date\",\"=\",\"" + postDate + "\"]]",
                StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Structure%20Assignment?filters=" + filters
                + "&limit_page_length=1&order_by=from_date%20desc";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Accept", "application/json").GET().build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Requête SSA avec filtre date: " + url + " | Status: " + res.statusCode());

        if (res.statusCode() != 200)
            return null;

        JsonObject json = gson.fromJson(res.body(), JsonObject.class);
        JsonArray arr = json.getAsJsonArray("data");
        if (arr != null && arr.size() > 0) {
            return arr.get(0).getAsJsonObject();
        }
        return null;
    }

    private JsonObject querySalaryStructureAssignmentWithoutDate(String emp, String sid)
            throws IOException, InterruptedException {
        String filters = URLEncoder.encode("[[\"employee\",\"=\",\"" + emp + "\"]]", StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Structure%20Assignment?filters=" + filters
                + "&limit_page_length=1&order_by=from_date%20desc";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid).header("Accept", "application/json").GET().build();

        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("Requête SSA sans filtre date: " + url + " | Status: " + res.statusCode());

        if (res.statusCode() != 200)
            return null;

        JsonObject json = gson.fromJson(res.body(), JsonObject.class);
        JsonArray arr = json.getAsJsonArray("data");
        if (arr != null && arr.size() > 0) {
            return arr.get(0).getAsJsonObject();
        }
        return null;
    }

    private boolean checkRequiredSsaFields(JsonObject ssa) {
        String[] requiredFields = { "name", "employee", "salary_structure", "company", "from_date" };
        for (String field : requiredFields) {
            if (!ssa.has(field) || ssa.get(field).isJsonNull() || ssa.get(field).getAsString().isBlank()) {
                System.err.println("Champ SSA manquant ou vide : " + field);
                return false;
            }
        }
        return true;
    }

    private void submitDocument(String docType, String docName, String sid) throws IOException, InterruptedException {
        String url = API_BASE_URL + encodeURIComponent(docType) + "/" + encodeURIComponent(docName) + "/submit";
        System.out.println("submitDocument URL : " + url);
        // Attendre que le document existe vraiment côté serveur (max 3 essais)
        int essais = 0;
        boolean exists = false;
        while (essais < 3 && !exists) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
            }
            JsonObject doc = getDocument(docType, docName, sid);
            if (doc != null) {
                exists = true;
            } else {
                System.out
                        .println("[DEBUG] Document pas encore disponible pour soumission (essai " + (essais + 1) + ")");
            }
            essais++;
        }
        if (!exists) {
            System.err.println(
                    "[DEBUG] Document " + docType + "/" + docName + " non disponible pour soumission après 3 essais.");
            return;
        }
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        System.out.println("submitDocument status : " + res.statusCode());
        if (res.statusCode() != 200) {
            System.err.println(
                    "[DEBUG] Échec de la soumission du document " + docType + "/" + docName + " : " + res.body());
        } else {
            // Vérifier que le docstatus est bien passé à 1
            JsonObject doc = getDocument(docType, docName, sid);
            if (doc != null && doc.has("docstatus") && doc.get("docstatus").getAsInt() == 1) {
                System.out.println("[DEBUG] Document " + docType + "/" + docName + " soumis avec succès.");
            } else {
                System.err
                        .println("[DEBUG] Document " + docType + "/" + docName + " n'est pas soumis (docstatus != 1)");
            }
        }
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

    // Récupère toutes les SSA pour un employé et une date donnée
    private List<JsonObject> fetchAllSSAForEmployeeAndDate(String employee, String fromDate, String sid)
            throws IOException, InterruptedException {
        String filters = URLEncoder.encode(
                "[[\"employee\",\"=\",\"" + employee + "\"],[\"from_date\",\"=\",\"" + fromDate + "\"]]",
                StandardCharsets.UTF_8);
        String url = API_BASE_URL + "Salary%20Structure%20Assignment?filters=" + filters;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .header("Accept", "application/json")
                .GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            return new ArrayList<>();
        JsonArray array = gson.fromJson(response.body(), JsonObject.class).getAsJsonArray("data");
        List<JsonObject> result = new ArrayList<>();
        for (JsonElement e : array)
            result.add(e.getAsJsonObject());
        return result;
    }
}
