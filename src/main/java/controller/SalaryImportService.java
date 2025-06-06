package controller;

import com.google.gson.*;
import java.io.*;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class SalaryImportService {
    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy");
    private final Map<String, Boolean> componentCache = new HashMap<>();

    // DTOs
    public static class EmployeeDto {
        private String ref;
        private String nom;
        private String prenom;
        private String genre;
        private Date dateEmbauche;
        private Date dateNaissance;
        private String company;

        public String getRef() {
            return ref;
        }

        public void setRef(String ref) {
            this.ref = ref;
        }

        public String getNom() {
            return nom;
        }

        public void setNom(String nom) {
            this.nom = nom;
        }

        public String getPrenom() {
            return prenom;
        }

        public void setPrenom(String prenom) {
            this.prenom = prenom;
        }

        public String getGenre() {
            return genre;
        }

        public void setGenre(String genre) {
            this.genre = genre;
        }

        public Date getDateEmbauche() {
            return dateEmbauche;
        }

        public void setDateEmbauche(Date dateEmbauche) {
            this.dateEmbauche = dateEmbauche;
        }

        public Date getDateNaissance() {
            return dateNaissance;
        }

        public void setDateNaissance(Date dateNaissance) {
            this.dateNaissance = dateNaissance;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }
    }

    public static class SalaryComponentDto {
        private String salaryStructure;
        private String name;
        private String abbr;
        private String type;
        private String formule;
        private String company;

        public String getSalaryStructure() {
            return salaryStructure;
        }

        public void setSalaryStructure(String salaryStructure) {
            this.salaryStructure = salaryStructure;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAbbr() {
            return abbr;
        }

        public void setAbbr(String abbr) {
            this.abbr = abbr;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getFormule() {
            return formule;
        }

        public void setFormule(String formule) {
            this.formule = formule;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }
    }

    public static class SalarySlipDto {
        private String employee;
        private String salaryStructure;
        private String month;
        private int baseSalary;

        public String getEmployee() {
            return employee;
        }

        public void setEmployee(String employee) {
            this.employee = employee;
        }

        public String getSalaryStructure() {
            return salaryStructure;
        }

        public void setSalaryStructure(String salaryStructure) {
            this.salaryStructure = salaryStructure;
        }

        public String getMonth() {
            return month;
        }

        public void setMonth(String month) {
            this.month = month;
        }

        public int getBaseSalary() {
            return baseSalary;
        }

        public void setBaseSalary(int baseSalary) {
            this.baseSalary = baseSalary;
        }
    }

    public static class ImportResult {
        public final boolean success;
        public final String message;
        public final List<String> errors;

        public ImportResult(boolean success, String message, List<String> errors) {
            this.success = success;
            this.message = message;
            this.errors = errors != null ? errors : new ArrayList<>();
        }
    }

    // Méthode principale d'importation
    public ImportResult processImport(InputStream employeesCsv, InputStream componentsCsv,
            InputStream salarySlipsCsv, String sessionId) {
        List<String> errors = new ArrayList<>();

        try {
            // 1. Lire et traiter les employés
            List<EmployeeDto> employees = readEmployees(employeesCsv, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la lecture des employés", errors);
            }

            // 2. Lire et traiter les composants de salaire
            Map<String, List<SalaryComponentDto>> componentsByStructure = readSalaryComponents(componentsCsv, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la lecture des composants", errors);
            }

            // 3. Lire et traiter les fiches de paie
            List<SalarySlipDto> salarySlips = readSalarySlips(salarySlipsCsv, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la lecture des fiches de paie", errors);
            }

            // 4. Créer les composants de salaire
            createSalaryComponents(componentsByStructure, sessionId, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la création des composants", errors);
            }

            // 5. Créer les structures de salaire
            createSalaryStructures(componentsByStructure, sessionId, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la création des structures", errors);
            }

            // 6. Créer les employés
            createEmployees(employees, sessionId, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la création des employés", errors);
            }

            // 7. Créer les fiches de paie
            createSalarySlips(salarySlips, sessionId, errors);
            if (!errors.isEmpty()) {
                return new ImportResult(false, "Erreur lors de la création des fiches de paie", errors);
            }

            return new ImportResult(true, "Importation terminée avec succès", errors);

        } catch (Exception e) {
            errors.add("Erreur inattendue: " + e.getMessage());
            return new ImportResult(false, "Échec de l'importation", errors);
        }
    }

    // Méthodes de lecture des fichiers CSV
    private List<EmployeeDto> readEmployees(InputStream is, List<String> errors) throws IOException {
        List<EmployeeDto> employees = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 7) {
                        EmployeeDto emp = new EmployeeDto();
                        emp.setRef(parts[0].trim());
                        emp.setNom(parts[1].trim());
                        emp.setPrenom(parts[2].trim());
                        emp.setGenre(parts[3].trim());
                        emp.setDateEmbauche(DATE_FORMAT.parse(parts[4].trim()));
                        emp.setDateNaissance(DATE_FORMAT.parse(parts[5].trim()));
                        emp.setCompany(parts[6].trim());
                        employees.add(emp);
                    }
                } catch (Exception e) {
                    errors.add(String.format("Erreur ligne %d: %s", lineNumber, e.getMessage()));
                }
            }
        }
        return employees;
    }

    private Map<String, List<SalaryComponentDto>> readSalaryComponents(InputStream is, List<String> errors)
            throws IOException {
        Map<String, List<SalaryComponentDto>> componentsByStructure = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 6) {
                        SalaryComponentDto comp = new SalaryComponentDto();
                        comp.setSalaryStructure(parts[0].trim());
                        comp.setName(parts[1].trim());
                        comp.setAbbr(parts[2].trim());
                        comp.setType(parts[3].trim());
                        comp.setFormule(parts[4].trim());
                        comp.setCompany(parts[5].trim());

                        componentsByStructure
                                .computeIfAbsent(comp.getSalaryStructure(), k -> new ArrayList<>())
                                .add(comp);
                    }
                } catch (Exception e) {
                    errors.add(String.format("Erreur ligne %d: %s", lineNumber, e.getMessage()));
                }
            }
        }
        return componentsByStructure;
    }

    private List<SalarySlipDto> readSalarySlips(InputStream is, List<String> errors) throws IOException {
        List<SalarySlipDto> salarySlips = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                try {
                    String[] parts = line.split(",");
                    if (parts.length >= 4) {
                        SalarySlipDto slip = new SalarySlipDto();
                        slip.setEmployee(parts[0].trim());
                        slip.setSalaryStructure(parts[1].trim());
                        slip.setMonth(parts[2].trim());
                        slip.setBaseSalary(Integer.parseInt(parts[3].trim()));
                        salarySlips.add(slip);
                    }
                } catch (Exception e) {
                    errors.add(String.format("Erreur ligne %d: %s", lineNumber, e.getMessage()));
                }
            }
        }
        return salarySlips;
    }

    private void createSalaryComponents(Map<String, List<SalaryComponentDto>> componentsByStructure,
            String sessionId, List<String> errors) {
        Set<String> createdComponents = new HashSet<>();

        // D'abord, collecter tous les composants uniques
        Map<String, SalaryComponentDto> uniqueComponents = new HashMap<>();
        for (List<SalaryComponentDto> components : componentsByStructure.values()) {
            for (SalaryComponentDto comp : components) {
                String componentName = comp.getName();
                if (!uniqueComponents.containsKey(componentName)) {
                    uniqueComponents.put(componentName, comp);
                }
            }
        }

        // Ensuite, créer chaque composant
        for (Map.Entry<String, SalaryComponentDto> entry : uniqueComponents.entrySet()) {
            SalaryComponentDto comp = entry.getValue();
            String componentName = entry.getKey();

            try {
                if (checkIfExists("Salary Component", componentName, sessionId)) {
                    continue; // Le composant existe déjà
                }

                // Créer l'objet composant avec tous les champs requis
                JsonObject component = new JsonObject();
                component.addProperty("doctype", "Salary Component");
                component.addProperty("salary_component", componentName);
                component.addProperty("type", comp.getType().equalsIgnoreCase("earning") ? "Earning" : "Deduction");
                component.addProperty("description", componentName);
                component.addProperty("company", comp.getCompany());
                component.addProperty("is_tax_applicable", 1);
                component.addProperty("is_flexible_benefit", 0);
                component.addProperty("variable_based_on_taxable_salary", 0);
                component.addProperty("depends_on_payment_days", 0);
                component.addProperty("is_tax_applicable_for_fy", 0);
                component.addProperty("round_to_the_nearest_integer", 0);
                component.addProperty("statistical_component", 0);
                component.addProperty("do_not_include_in_total", 0);
                component.addProperty("disabled", 0);
                component.addProperty("condition", "");
                component.addProperty("amount_based_on_formula", 1);
                component.addProperty("formula", comp.getFormule());
                component.addProperty("amount", 0);
                component.addProperty("docstatus", 1); // Marquer comme soumis

                // Appeler l'API pour créer le composant
                String response = callApi("Salary%20Component", "POST", gson.toJson(component), sessionId);

                // Vérifier la réponse
                if (response != null && !response.isEmpty()) {
                    JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                    if (responseObj.has("data") && responseObj.getAsJsonObject("data").has("name")) {
                        createdComponents.add(componentName);
                        // Soumettre le composant
                        String docName = responseObj.getAsJsonObject("data").get("name").getAsString();
                        callApi("Salary%20Component/" + docName + "/submit", "POST", "{}", sessionId);
                    }
                }

            } catch (Exception e) {
                errors.add(String.format("Erreur création composant %s: %s",
                        componentName, e.getMessage()));
            }
        }
    }

    private void createSalaryStructures(Map<String, List<SalaryComponentDto>> componentsByStructure,
            String sessionId, List<String> errors) {
        for (Map.Entry<String, List<SalaryComponentDto>> entry : componentsByStructure.entrySet()) {
            String structureName = entry.getKey();
            List<SalaryComponentDto> components = entry.getValue();

            try {
                // Vérifier si la structure existe déjà
                if (checkIfExists("Salary Structure", structureName, sessionId)) {
                    System.out.println("La structure " + structureName + " existe déjà, passage à la suivante");
                    continue;
                }

                // Créer des ensembles pour suivre les composants uniques par type
                Map<String, SalaryComponentDto> uniqueEarnings = new LinkedHashMap<>();
                Map<String, SalaryComponentDto> uniqueDeductions = new LinkedHashMap<>();

                // Filtrer les doublons et organiser par type
                for (SalaryComponentDto comp : components) {
                    if (comp == null)
                        continue;

                    String componentName = comp.getName() != null ? comp.getName().trim() : "";
                    String componentType = comp.getType() != null ? comp.getType().trim().toLowerCase() : "";

                    if (componentName.isEmpty() || componentType.isEmpty()) {
                        System.out.println("Composant ignoré : nom ou type vide");
                        continue;
                    }

                    // Vérifier que le composant existe dans ERPNext
                    if (!checkIfExists("Salary Component", componentName, sessionId)) {
                        String errorMsg = String.format("Le composant %s n'existe pas dans le système", componentName);
                        System.out.println(errorMsg);
                        errors.add(errorMsg);
                        continue;
                    }

                    // Ajouter au type approprié, en évitant les doublons
                    if ("earning".equals(componentType)) {
                        if (!uniqueEarnings.containsKey(componentName)) {
                            System.out.println("Ajout du gain : " + componentName);
                            uniqueEarnings.put(componentName, comp);
                        }
                    } else if ("deduction".equals(componentType)) {
                        if (!uniqueDeductions.containsKey(componentName)) {
                            System.out.println("Ajout de la déduction : " + componentName);
                            uniqueDeductions.put(componentName, comp);
                        }
                    }
                }

                // Créer la structure de salaire
                JsonObject structure = new JsonObject();
                structure.addProperty("doctype", "Salary Structure");
                structure.addProperty("name", structureName);
                structure.addProperty("company", components.get(0).getCompany());
                structure.addProperty("is_active", "Yes");
                structure.addProperty("from_date", DATE_FORMAT.format(new Date()));
                structure.addProperty("to_date", "");
                structure.addProperty("payroll_frequency", "Monthly");
                structure.addProperty("docstatus", 0);

                // Ajouter les gains s'il y en a
                if (!uniqueEarnings.isEmpty()) {
                    JsonArray earnings = new JsonArray();
                    for (SalaryComponentDto comp : uniqueEarnings.values()) {
                        JsonObject item = createSalaryStructureItem(comp);
                        if (item != null) {
                            System.out.println("Ajout du gain à la structure : " + comp.getName());
                            earnings.add(item);
                        }
                    }
                    if (earnings.size() > 0) {
                        structure.add("earnings", earnings);
                    }
                }

                // Ajouter les déductions s'il y en a
                if (!uniqueDeductions.isEmpty()) {
                    JsonArray deductions = new JsonArray();
                    for (SalaryComponentDto comp : uniqueDeductions.values()) {
                        JsonObject item = createSalaryStructureItem(comp);
                        if (item != null) {
                            System.out.println("Ajout de la déduction à la structure : " + comp.getName());
                            deductions.add(item);
                        }
                    }
                    if (deductions.size() > 0) {
                        structure.add("deductions", deductions);
                    }
                }

                // Créer la structure
                String response = callApi("Salary%20Structure", "POST", gson.toJson(structure), sessionId);

                // Soumettre la structure
                if (response != null && !response.isEmpty()) {
                    try {
                        JsonObject responseObj = gson.fromJson(response, JsonObject.class);
                        if (responseObj.has("data") && responseObj.getAsJsonObject("data").has("name")) {
                            String docName = responseObj.getAsJsonObject("data").get("name").getAsString();
                            callApi("Salary%20Structure/" + docName + "/submit", "POST", "{}", sessionId);
                        }
                    } catch (Exception e) {
                        errors.add(String.format("Erreur lors de la soumission de la structure %s: %s",
                                structureName, e.getMessage()));
                    }
                }

            } catch (Exception e) {
                errors.add(String.format("Erreur création structure %s: %s",
                        structureName, e.getMessage()));
            }
        }
    }

    private JsonObject createSalaryStructureItem(SalaryComponentDto comp) {
        try {
            JsonObject item = new JsonObject();
            item.addProperty("salary_component", comp.getName().trim());
            item.addProperty("amount", comp.getFormule().trim());
            item.addProperty("conditional_formula", "");
            item.addProperty("amount_based_on_formula", 1);
            item.addProperty("doctype", "Salary Detail");
            item.addProperty("parentfield",
                    comp.getType().trim().equalsIgnoreCase("earning") ? "earnings" : "deductions");
            item.addProperty("parenttype", "Salary Structure");
            return item;
        } catch (Exception e) {
            System.err.println("Erreur création item pour " + comp.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private void createEmployees(List<EmployeeDto> employees, String sessionId, List<String> errors) {
        for (EmployeeDto emp : employees) {
            try {
                if (checkIfExists("Employee", emp.getRef(), sessionId)) {
                    continue; // L'employé existe déjà
                }

                JsonObject employee = new JsonObject();
                employee.addProperty("doctype", "Employee");
                employee.addProperty("employee", emp.getRef());
                employee.addProperty("first_name", emp.getPrenom());
                employee.addProperty("last_name", emp.getNom());
                employee.addProperty("gender", emp.getGenre().equalsIgnoreCase("Masculin") ? "Male" : "Female");
                employee.addProperty("date_of_birth", DATE_FORMAT.format(emp.getDateNaissance()));
                employee.addProperty("date_of_joining", DATE_FORMAT.format(emp.getDateEmbauche()));
                employee.addProperty("company", emp.getCompany());
                employee.addProperty("status", "Active");

                callApi("Employee", "POST", gson.toJson(employee), sessionId);

            } catch (Exception e) {
                errors.add(String.format("Erreur création employé %s: %s",
                        emp.getRef(), e.getMessage()));
            }
        }
    }

    private void createSalarySlips(List<SalarySlipDto> salarySlips, String sessionId, List<String> errors) {
        for (SalarySlipDto slip : salarySlips) {
            try {
                String slipName = String.format("%s-%s-%s",
                        slip.getEmployee(),
                        slip.getMonth().replace("/", "-"),
                        "SALARY");

                if (checkIfExists("Salary Slip", slipName, sessionId)) {
                    continue; // La fiche de paie existe déjà
                }

                JsonObject salarySlip = new JsonObject();
                salarySlip.addProperty("doctype", "Salary Slip");
                salarySlip.addProperty("employee", slip.getEmployee());
                salarySlip.addProperty("salary_structure", slip.getSalaryStructure());
                salarySlip.addProperty("start_date", slip.getMonth() + "-01");
                salarySlip.addProperty("end_date", slip.getMonth() + "-28");
                salarySlip.addProperty("posting_date", DATE_FORMAT.format(new Date()));
                salarySlip.addProperty("company", "My Company");
                salarySlip.addProperty("base", slip.getBaseSalary());

                callApi("Salary%20Slip", "POST", gson.toJson(salarySlip), sessionId);

            } catch (Exception e) {
                errors.add(String.format("Erreur création fiche de paie pour %s: %s",
                        slip.getEmployee(), e.getMessage()));
            }
        }
    }

    // Méthodes utilitaires pour les appels API
    private String callApi(String endpoint, String method, String requestBody, String sessionId) {
        try {
            // Configuration du client HTTP avec timeout
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(30))
                    .build();

            // Construction de la requête
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + endpoint))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("Authorization", "token " + sessionId)
                    .timeout(Duration.ofSeconds(30));

            // Ajout du corps pour les méthodes POST/PUT
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                if (requestBody != null) {
                    builder.method(method, HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8));
                } else {
                    builder.method(method, HttpRequest.BodyPublishers.noBody());
                }
            } else {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            // Envoi de la requête
            HttpResponse<String> response = client.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // Journalisation pour le débogage
            System.out.println(String.format("API %s %s - Status: %d",
                    method, endpoint, response.statusCode()));

            // Vérification du code de statut
            if (response.statusCode() >= 400) {
                String errorMsg = String.format("Erreur API %s: %d - %s",
                        endpoint, response.statusCode(), response.body());
                System.err.println(errorMsg);
                throw new IOException(errorMsg);
            }

            return response.body();

        } catch (IOException | InterruptedException e) {
            String errorMsg = String.format("Échec de l'appel API %s %s: %s",
                    method, endpoint, e.getMessage());
            System.err.println(errorMsg);
            throw new RuntimeException(errorMsg, e);
        }
    }

    private boolean checkIfExists(String doctype, String name, String sessionId) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }

        try {
            // Nettoyer le nom
            String cleanName = name.trim();

            // Vérifier d'abord dans le cache local
            String cacheKey = doctype + "_" + cleanName.toLowerCase();
            if (componentCache.containsKey(cacheKey)) {
                return componentCache.get(cacheKey);
            }

            // Utiliser une requête de liste avec filtres pour une meilleure fiabilité
            String filters = URLEncoder.encode("[[\"name\", \"=\", \"" + cleanName + "\"]]", StandardCharsets.UTF_8);
            String endpoint = String.format("%s?fields=[\"name\"]&filters=%s&limit_page_length=1",
                    doctype, filters);

            // Appel API pour vérifier l'existence
            String response = callApi(endpoint, "GET", null, sessionId);

            // Vérifier la réponse
            if (response == null || response.isEmpty()) {
                componentCache.put(cacheKey, false);
                return false;
            }

            // Essayer de parser la réponse JSON
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

            // Vérifier si la réponse contient des données
            boolean exists = false;
            if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
                JsonArray dataArray = jsonResponse.getAsJsonArray("data");
                exists = dataArray != null && dataArray.size() > 0;
            }

            // Mettre en cache le résultat
            componentCache.put(cacheKey, exists);

            if (exists) {
                System.out.println(String.format("Composant trouvé: %s/%s", doctype, cleanName));
            } else {
                System.out.println(String.format("Composant non trouvé: %s/%s", doctype, cleanName));
            }

            return exists;

        } catch (Exception e) {
            // En cas d'erreur, considérer que le document n'existe pas
            System.err.println("Erreur lors de la vérification de l'existence de " +
                    doctype + "/" + name + ": " + e.getMessage());
            return false;
        }
    }
}
