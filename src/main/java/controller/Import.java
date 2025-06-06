package controller;

import java.io.*;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@WebServlet("/import")
@MultipartConfig
public class Import extends HttpServlet {
    private static final long serialVersionUID = 1L;

    private final HttpClient httpClient;
    private final String apiBaseUrl = "http://172.25.36.0:8000/api/resource/";
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    String sid;

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public Import() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Récupération du SID
        String sid = getSid();
        if (sid == null || sid.isEmpty()) {
            HttpSession session = request.getSession(true);
            sid = (String) session.getAttribute("sid");
            setSid(sid);
        }

        // Gestion de la réinitialisation des données
        if ("resetdata".equals(request.getParameter("action"))) {
            boolean success = resetdata(sid);
            response.sendRedirect("dashboard.jsp?reset=" + (success ? "success" : "error"));
            return;
        }

        // Configuration de la réponse pour les autres actions
        try (PrintWriter out = response.getWriter()) {
            if (sid == null || sid.isEmpty()) {
                sendErrorResponse(response, "Session invalide. Veuillez vous reconnecter.");
                return;
            }

            // Ajout du cookie SID dans la réponse
            Cookie cookie = new Cookie("sid", sid);
            cookie.setMaxAge(-1); // Cookie de session
            response.addCookie(cookie);

            // Récupérer les fichiers uploadés
            Part employeesPart = request.getPart("employees");
            Part structuresPart = request.getPart("structures");
            Part paiesPart = request.getPart("paies");

            if (employeesPart == null || structuresPart == null || paiesPart == null) {
                sendErrorResponse(response, "Tous les fichiers CSV sont requis");
                return;
            }

            List<String> errors = importDataAsync(
                    employeesPart.getInputStream(),
                    structuresPart.getInputStream(),
                    paiesPart.getInputStream(),
                    request);

            JsonObject result = new JsonObject();
            if (errors.isEmpty()) {
                result.addProperty("success", true);
                result.addProperty("message", "Import réalisé avec succès");
            } else {
                result.addProperty("success", false);
                result.add("errors", gson.toJsonTree(errors));
            }

            out.write(gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
            resetdata(getSid());
            sendErrorResponse(response, "Erreur lors de l'import: " + e.getMessage());
        }
    }

    // Méthode utilitaire pour les réponses d'erreur
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        JsonObject error = new JsonObject();
        error.addProperty("success", false);
        error.addProperty("message", message);
        response.getWriter().write(gson.toJson(error));
    }

    public List<String> importDataAsync(InputStream employesCsv, InputStream structuresCsv,
            InputStream paiesCsv, HttpServletRequest request) throws Exception {
        List<String> erreurs = new ArrayList<>();
        HttpSession session = request.getSession(true); // true pour créer si nécessaire
        // ⚠️ Récupération du sid depuis la session. Assurez-vous qu'il provient d'un
        // login valide ERPNext
        String sid = (String) session.getAttribute("sid");
        String Username = (String) session.getAttribute("loggedInUser");
        System.out.println("SID: " + sid);
        System.out.println(Username);

        // 1. Charger et valider les employés
        List<EmployeDto> employes = lireEmployesAsync(employesCsv, erreurs);
        if (!erreurs.isEmpty())
            return erreurs;

        // 2. Charger et valider les structures salariales
        List<StructureDto> structures = lireStructuresAsync(structuresCsv, erreurs);
        if (!erreurs.isEmpty())
            return erreurs;

        // 3. Charger et valider les paies
        List<PaieDto> paies = lirePaiesAsync(paiesCsv, erreurs);
        if (!erreurs.isEmpty())
            return erreurs;

        // 4. Créer / vérifier les companies
        Set<String> companies = new HashSet<>();
        for (EmployeDto e : employes) {
            companies.add(e.getCompany());
        }

        for (String company : companies) {
            Result result = creerCompanySiNonExistante(company, sid);
            if (!result.success) {
                erreurs.add("Erreur création company: " + company + " - Détail: " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty())
            return erreurs;

        // 5. Créer les employés
        for (EmployeDto emp : employes) {
            GenderResult genderResult = traduireGenre(emp.getGenre());
            if (!genderResult.success) {
                erreurs.add("Erreur traduction genre pour " + emp.getRef() +
                        " - Genre fourni : '" + emp.getGenre() + "' - " + genderResult.errorMessage);
            } else {
                emp.setGenre(genderResult.translatedGender);
            }
        }
        if (!erreurs.isEmpty())
            return erreurs;

        for (EmployeDto emp : employes) {
            Result result = creerEmploye(emp, sid);
            if (!result.success) {
                erreurs.add("Erreur création employe pour " + emp.getRef() + " - " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty())
            return erreurs;

        // 6. Créer les structures salariales regroupées
        Map<String, List<StructureDto>> groupes = structures.stream()
                .collect(Collectors.groupingBy(StructureDto::getSalaryStructure));

        for (Map.Entry<String, List<StructureDto>> entry : groupes.entrySet()) {
            Result result = creerStructureRegroupee(entry.getKey(), entry.getValue(), sid);
            if (!result.success) {
                erreurs.add("Erreur création de " + entry.getKey() + " : " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty())
            return erreurs;

        // 7. Assigner les structures salariales
        for (PaieDto p : paies) {
            Result result = assignerStructureSalaire(p, sid);
            if (!result.success) {
                erreurs.add("Erreur assignation structure pour : " + p.getRefEmploye() +
                        " mois " + dateFormat.format(p.getMois()) + " - Détail: " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty())
            return erreurs;

        // 8. Créer les paies
        for (PaieDto p : paies) {
            Result result = creerPaie(p, sid);
            if (!result.success) {
                erreurs.add("Erreur création paie pour : " + p.getRefEmploye() +
                        " mois " + dateFormat.format(p.getMois()) + " - Détail: " + result.errorMessage);
            }
        }

        return erreurs;
    }

    private GenderResult traduireGenre(String genre) {
        if (genre == null) {
            return new GenderResult(false, null, "Genre null");
        }

        String genderNormalized = genre.trim().toLowerCase();
        String translatedGender = null;

        switch (genderNormalized) {
            case "masculin":
            case "m":
                translatedGender = "Male";
                break;
            case "feminin":
            case "f":
                translatedGender = "Female";
                break;
        }

        if (translatedGender == null) {
            return new GenderResult(false, null, "Genre non reconnu : " + genre);
        }

        return new GenderResult(true, translatedGender, null);
    }

    private List<EmployeDto> lireEmployesAsync(InputStream stream, List<String> erreurs) throws IOException {
        List<EmployeDto> liste = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                String[] parts = line.split(",");
                System.out.println("parts: " + parts.length);

                if (parts.length != 7) {
                    erreurs.add("Fichier Employés : ligne " + lineNumber + " - Nombre de colonnes incorrect");
                    continue;
                }

                try {
                    Date dateEmbauche = dateFormat.parse(parts[4]);
                    Date dateNaissance = dateFormat.parse(parts[5]);

                    EmployeDto employe = new EmployeDto();
                    employe.setRef(parts[0]);
                    employe.setNom(parts[1]);
                    employe.setPrenom(parts[2]);
                    employe.setGenre(parts[3]);
                    employe.setDateEmbauche(dateEmbauche);
                    employe.setDateNaissance(dateNaissance);
                    employe.setCompany(parts[6]);

                    liste.add(employe);
                } catch (ParseException e) {
                    // Appel à resetdata sans stocker le résultat car non utilisé
                    resetdata(getSid());
                    erreurs.add("Fichier Employés : ligne " + lineNumber + " - Date invalide");
                }
            }
        }

        return liste;
    }

    private List<StructureDto> lireStructuresAsync(InputStream stream, List<String> erreurs) throws IOException {
        List<StructureDto> liste = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                String[] parts = line.split(",");
                if (parts.length < 6) {
                    erreurs.add("Fichier Structure Salariale : ligne " + lineNumber +
                            " - Nombre de colonnes incorrect (attendu 6, trouvé " + parts.length + ")");
                    continue;
                }

                String type = parts[3].trim().toLowerCase();
                if (!type.equals("earning") && !type.equals("deduction")) {
                    erreurs.add("Fichier Structure Salariale : ligne " + lineNumber +
                            " - Type invalide : " + parts[3]);
                    continue;
                }

                type = Character.toUpperCase(type.charAt(0)) + type.substring(1);

                StructureDto structure = new StructureDto();
                structure.setSalaryStructure(parts[0].trim());
                structure.setName(parts[1].trim());
                structure.setAbbr(parts[2].trim());
                structure.setType(type);
                structure.setValeur(parts[4].trim());
                structure.setCompany(parts[5].trim());

                liste.add(structure);
            }
        }

        return liste;
    }

    private List<PaieDto> lirePaiesAsync(InputStream stream, List<String> erreurs) throws IOException {
        List<PaieDto> liste = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1)
                    continue; // Skip header

                String[] parts = line.split(",");
                if (parts.length != 4) {
                    erreurs.add("Fichier Paies : ligne " + lineNumber + " - Nombre de colonnes incorrect");
                    continue;
                }

                try {
                    Date mois = dateFormat.parse(parts[0]);
                    int salaireBase = Integer.parseInt(parts[2]);

                    PaieDto paie = new PaieDto();
                    paie.setMois(mois);
                    paie.setRefEmploye(parts[1]);
                    paie.setSalaireBase(salaireBase);
                    paie.setSalaire(parts[3]);

                    liste.add(paie);
                } catch (ParseException | NumberFormatException e) {
                    // Appel à resetdata sans stocker le résultat car non utilisé
                    resetdata(getSid());
                    erreurs.add("Fichier Paies : ligne " + lineNumber + " - Données invalides");
                }
            }
        }

        return liste;
    }

    private Result creerCompanySiNonExistante(String companyName, String sid) {
        try {
            String baseUrl = "http://172.25.36.0:8000/api/resource/";

            // Vérification de l'existence d'une Holiday List
            String holidayListEndpoint = "Holiday%20List";
            String filtersHoliday = URLEncoder.encode("[[\"holiday_list_name\",\"=\",\"holiday\"]]",
                    StandardCharsets.UTF_8);
            String urlCheckHoliday = baseUrl + holidayListEndpoint + "?filters=" + filtersHoliday;

            HttpRequest requestCheckHoliday = HttpRequest.newBuilder()
                    .uri(URI.create(urlCheckHoliday))
                    .GET()
                    .header("Cookie", "sid=" + sid)
                    .build();

            HttpResponse<String> responseCheckHoliday = httpClient.send(requestCheckHoliday,
                    HttpResponse.BodyHandlers.ofString());
            if (responseCheckHoliday.statusCode() != 200) {
                return new Result(false, "Échec vérification Holiday List : " + responseCheckHoliday.statusCode());
            }

            // Vérification si la Company existe déjà
            String filtersCompany = URLEncoder.encode("[[\"name\",\"=\",\"" + companyName + "\"]]",
                    StandardCharsets.UTF_8);
            String urlGet = baseUrl + "Company?filters=" + filtersCompany;

            HttpRequest requestGet = HttpRequest.newBuilder()
                    .uri(URI.create(urlGet))
                    .GET()
                    .header("Cookie", "sid=" + sid)
                    .build();

            HttpResponse<String> responseGet = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());
            if (responseGet.statusCode() != 200) {
                return new Result(false, "GET Company failed: " + responseGet.statusCode());
            }

            JsonObject jsonGet = JsonParser.parseString(responseGet.body()).getAsJsonObject();
            if (jsonGet.has("data") && jsonGet.get("data").isJsonArray()) {
                JsonArray dataArray = jsonGet.getAsJsonArray("data");
                if (dataArray.size() > 0) {
                    return new Result(true, null); // Company already exists
                }
            }

            // Création de la nouvelle Company
            JsonObject companyData = new JsonObject();
            companyData.addProperty("doctype", "Company");
            companyData.addProperty("company_name", companyName);
            companyData.addProperty("default_currency", "USD");
            companyData.addProperty("default_holiday_list", "holiday");

            String postUrl = baseUrl + "Company";

            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(postUrl))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid) // ✅ Authentification correcte ici
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(companyData)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false,
                        "POST Company failed: " + responsePost.statusCode() + " - " + responsePost.body());
            }

            return new Result(true, null);

        } catch (Exception e) {
            Boolean resetdata = resetdata(getSid());
            return new Result(false, "Erreur création company: " + companyName + " - Détail: " + e.getMessage());
        }
    }

    private Result creerSalaryComponentSiNonExistant(String name, String type, String abbr, String sid) {
        try {

            String baseUrl = "http://172.25.36.0:8000/api/resource/";
            String doctypePath = "Salary%20Component"; // ✅ encodage manuel du chemin avec %20
            String filters = URLEncoder.encode("[[\"name\",\"=\",\"" + name + "\"]]", StandardCharsets.UTF_8);
            String getUrl = baseUrl + doctypePath + "?filters=" + filters;

            URI getUri = URI.create(getUrl); // ✅ URI valide avec %20 au lieu d'espace

            HttpRequest requestGet = HttpRequest.newBuilder()
                    .uri(getUri)
                    .GET()
                    .header("Cookie", "sid=" + sid)
                    .build();

            HttpResponse<String> responseGet = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());

            if (responseGet.statusCode() != 200) {
                return new Result(false, "GET Salary Component failed: " + responseGet.body());
            }

            JsonObject jsonGet = JsonParser.parseString(responseGet.body()).getAsJsonObject();
            if (jsonGet.has("data")) {
                JsonArray results = jsonGet.getAsJsonArray("data");
                if (results.size() > 0) {
                    return new Result(true, null); // déjà existant
                }
            }

            // Création du Salary Component
            JsonObject payload = new JsonObject();
            payload.addProperty("doctype", "Salary Component");
            payload.addProperty("salary_component", name);
            payload.addProperty("salary_component_abbr", abbr);
            payload.addProperty("type", type);
            payload.addProperty("is_tax_applicable", 1);
            payload.addProperty("amount_based_on_formula", 1);
            payload.addProperty("depends_on_payment_days", 0);

            String postUrl = baseUrl + doctypePath;
            URI postUri = URI.create(postUrl); // ✅ URI avec %20
            System.out.println("salary componement cree : =====" + name);
            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(postUri)
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "POST Salary Component failed: " + responsePost.body());
            }

            return new Result(true, null);

        } catch (Exception e) {
            // Boolean resetdata = resetdata(getSid());
            return new Result(false, "Erreur création Salary Component: " + e.getMessage());
        }
    }

    private Result creerEmploye(EmployeDto e, String sid) {
        try {
            // Vérifier d'abord si l'employé existe déjà
            String filter = "[[\"employee_number\",\"=\",\"" + e.getRef() + "\"]]";
            String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());
            String urlCheck = apiBaseUrl + "Employee?filters=" + encodedFilter;

            HttpRequest requestCheck = HttpRequest.newBuilder()
                    .uri(URI.create(urlCheck))
                    .header("Cookie", "sid=" + sid)
                    .GET()
                    .build();

            HttpResponse<String> responseCheck = httpClient.send(requestCheck, HttpResponse.BodyHandlers.ofString());

            if (responseCheck.statusCode() == 200) {
                JsonObject jsonCheck = JsonParser.parseString(responseCheck.body()).getAsJsonObject();
                if (jsonCheck.has("data")) {
                    JsonArray dataArray = jsonCheck.getAsJsonArray("data");
                    if (dataArray.size() > 0) {
                        System.out.println("Employé déjà existant: " + e.getRef());
                        return new Result(true, null); // Employé déjà existant
                    }
                }
            }

            // Créer l'employé avec des données complètes
            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Employee");
            data.addProperty("employee_name", e.getNom() + " " + e.getPrenom());
            data.addProperty("employee_number", e.getRef());
            data.addProperty("first_name", e.getNom());
            data.addProperty("last_name", e.getPrenom());
            data.addProperty("gender", e.getGenre());
            data.addProperty("date_of_birth", new SimpleDateFormat("yyyy-MM-dd").format(e.getDateNaissance()));
            data.addProperty("date_of_joining", new SimpleDateFormat("yyyy-MM-dd").format(e.getDateEmbauche()));
            data.addProperty("company", e.getCompany());
            data.addProperty("status", "Active");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Employee"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("Employé créé avec succès: " + e.getRef());
                return new Result(true, null);
            } else {
                return new Result(false, "Erreur création employé: " + response.body());
            }

        } catch (Exception ex) {
            Boolean resetdata = resetdata(getSid());
            return new Result(false, "Exception création employé: " + ex.getMessage());
        }
    }

    /**
     * Crée une structure de salaire groupée avec ses composants
     * 
     * @param salaryStructure Le nom de la structure de salaire
     * @param lignes          La liste des composants de la structure
     * @param sid             L'identifiant de session
     * @return Un objet Result indiquant le succès ou l'échec de l'opération
     */
    private Result creerStructureRegroupee(String salaryStructure, List<StructureDto> lignes, String sid) {
        try {
            // Vérifier d'abord si la structure existe déjà
            String checkUrl = apiBaseUrl + "Salary%20Structure/"
                    + URLEncoder.encode(salaryStructure, StandardCharsets.UTF_8.toString());

            // Vérifier si la structure existe déjà et la supprimer si c'est le cas
            // pour éviter les doublons
            HttpRequest checkRequest = HttpRequest.newBuilder()
                    .uri(URI.create(checkUrl))
                    .header("Cookie", "sid=" + sid)
                    .GET()
                    .build();

            HttpResponse<String> checkResponse = httpClient.send(checkRequest, HttpResponse.BodyHandlers.ofString());
            if (checkResponse.statusCode() == 200) {
                // La structure existe déjà, on la supprime d'abord
                HttpRequest deleteRequest = HttpRequest.newBuilder()
                        .uri(URI.create(checkUrl))
                        .header("Cookie", "sid=" + sid)
                        .DELETE()
                        .build();
                httpClient.send(deleteRequest, HttpResponse.BodyHandlers.ofString());
            }

            JsonArray earnings = new JsonArray();
            JsonArray deductions = new JsonArray();
            List<String> erreurs = new ArrayList<>();
            Map<String, StructureDto> composantsUniques = new LinkedHashMap<>();
            String company = null;

            // 1. Nettoyage et validation des données d'entrée
            for (StructureDto s : lignes) {
                if (s == null || s.getName() == null || s.getName().trim().isEmpty()) {
                    continue;
                }
                company = s.getCompany(); // On prend la dernière company non nulle

                // Créer une clé unique avec le nom et le type pour éviter les doublons
                String cleComposant = (s.getName().trim() + "|" + s.getType()).toLowerCase();

                // Vérification des doublons avec même nom/type mais propriétés différentes
                if (composantsUniques.containsKey(cleComposant)) {
                    StructureDto existant = composantsUniques.get(cleComposant);
                    if (!existant.getAbbr().equalsIgnoreCase(s.getAbbr())) {
                        erreurs.add(
                                String.format("Conflit de définition pour le composant '%s' (abréviation différente)",
                                        s.getName()));
                    }
                } else {
                    composantsUniques.put(cleComposant, s);
                }
            }

            // 2. Création des composants uniques
            // On utilise directement composantsUniques qui contient déjà les composants
            // uniques
            for (StructureDto s : composantsUniques.values()) {
                // Créer le composant s'il n'existe pas
                Result compResult = creerSalaryComponentSiNonExistant(s.getName(), s.getType(), s.getAbbr(), sid);
                if (!compResult.success) {
                    erreurs.add(
                            String.format("Erreur avec le composant '%s': %s", s.getName(), compResult.errorMessage));
                    continue;
                }

                // Créer la ligne pour le composant
                JsonObject line = new JsonObject();
                line.addProperty("salary_component", s.getName());
                line.addProperty("abbr", s.getAbbr());
                line.addProperty("formula", s.getValeur());
                line.addProperty("is_tax_applicable", 1);
                line.addProperty("amount_based_on_formula", 1);
                line.addProperty("depends_on_payment_days", 0);

                // Ajouter au tableau approprié
                if (s.getType().equalsIgnoreCase("earning")) {
                    earnings.add(line);
                } else if (s.getType().equalsIgnoreCase("deduction")) {
                    deductions.add(line);
                } else {
                    erreurs.add(String.format("Type de composant invalide pour '%s': %s", s.getName(), s.getType()));
                }
            }

            // 3. Si erreurs, on arrête tout
            if (!erreurs.isEmpty()) {
                return new Result(false, "Erreurs de validation des composants :\n" + String.join("\n", erreurs));
            }

            // Validation des tableaux vides
            if (earnings.size() == 0 && deductions.size() == 0) {
                return new Result(false, "Aucun composant valide trouvé pour la structure de salaire");
            }

            // 4. Création de la structure de salaire
            JsonObject payload = new JsonObject();
            payload.addProperty("doctype", "Salary Structure");
            payload.addProperty("name", salaryStructure);
            payload.addProperty("is_active", "Yes");
            payload.add("earnings", earnings);
            payload.add("deductions", deductions);
            if (company != null && !company.trim().isEmpty()) {
                payload.addProperty("company", company.trim());
            }

            // 5. Envoi de la requête de création
            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "Échec de la création de la structure de salaire: " +
                        (responsePost.body() != null ? responsePost.body() : "Réponse vide"));
            }

            // 6. Soumission de la structure
            String encodedName = URLEncoder.encode(salaryStructure, StandardCharsets.UTF_8.toString());
            JsonObject submitPayload = new JsonObject();
            submitPayload.addProperty("docstatus", 1);

            HttpRequest requestSubmit = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure/" + encodedName))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(submitPayload)))
                    .build();

            HttpResponse<String> responseSubmit = httpClient.send(requestSubmit, HttpResponse.BodyHandlers.ofString());

            if (responseSubmit.statusCode() != 200) {
                return new Result(false, "Échec de la soumission de la structure de salaire: " +
                        (responseSubmit.body() != null ? responseSubmit.body() : "Réponse vide"));
            }

            return new Result(true, null);

        } catch (Exception e) {
            return new Result(false, "Erreur lors de la création de la structure de salaire: " + e.getMessage());
        }
    }

    private Result assignerStructureSalaire(PaieDto paie, String sid) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(paie.getMois());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date moisDebut = cal.getTime();
            System.out.println("reference emplyer ----------------------------------------->" + paie.getRefEmploye());
            String employeeName = getEmployeeNameByEmployeeNumber(paie.getRefEmploye(), sid);

            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Salary Structure Assignment");
            data.addProperty("employee", employeeName);
            data.addProperty("salary_structure", paie.getSalaire());
            data.addProperty("from_date", new SimpleDateFormat("yyyy-MM-dd").format(moisDebut));
            data.addProperty("base", paie.getSalaireBase());

            // URL encodée pour espace
            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure%20Assignment"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "Échec de la création de l'assignation de structure de salaire: " +
                        (responsePost.body() != null ? responsePost.body() : "Réponse vide"));
            }

            JsonElement jsonElement = JsonParser.parseString(responsePost.body());
            JsonObject jsonPost = jsonElement.getAsJsonObject();
            String name = jsonPost.getAsJsonObject("data").get("name").getAsString();

            JsonObject submitPayload = new JsonObject();
            submitPayload.addProperty("docstatus", 1);

            // URL encodée aussi ici + potentiellement encodage de 'name' si besoin
            String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString());
            HttpRequest requestSubmit = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure%20Assignment/" + encodedName))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .header("Accept", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(submitPayload)))
                    .build();

            HttpResponse<String> responseSubmit = httpClient.send(requestSubmit, HttpResponse.BodyHandlers.ofString());

            if (responseSubmit.statusCode() != 200) {
                return new Result(false, "Échec de la soumission de l'assignation de structure de salaire: " +
                        (responseSubmit.body() != null ? responseSubmit.body() : "Réponse vide"));
            }

            return new Result(true, null);

        } catch (Exception e) {
            return new Result(false, "Erreur lors de l'assignation de la structure de salaire: " + e.getMessage());
        }
    }

    private Result creerPaie(PaieDto paie, String sid) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(paie.getMois());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date moisDebut = cal.getTime();

            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.DAY_OF_MONTH, -1);
            Date moisFin = cal.getTime();

            // Récupération correcte du nom de l'employé
            String employeeName = getEmployeeNameByEmployeeNumber(paie.getRefEmploye(), sid);

            // Vérification si le Salary Slip existe déjà
            String filter = "[[\"employee\",\"=\",\"" + employeeName + "\"]," +
                    "[\"start_date\",\">=\",\"" + new SimpleDateFormat("yyyy-MM-dd").format(moisDebut) + "\"]," +
                    "[\"end_date\",\"<=\",\"" + new SimpleDateFormat("yyyy-MM-dd").format(moisFin) + "\"]]";
            String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());

            String urlCheck = apiBaseUrl + "Salary%20Slip?filters=" + encodedFilter;

            HttpRequest requestGet = HttpRequest.newBuilder()
                    .uri(URI.create(urlCheck))
                    .header("Cookie", "sid=" + sid)
                    .GET()
                    .build();

            HttpResponse<String> responseGet = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());

            if (responseGet.statusCode() != 200) {
                return new Result(false, "GET Salary Slip failed: " + responseGet.body());
            }

            JsonObject jsonGet = JsonParser.parseString(responseGet.body()).getAsJsonObject();
            if (jsonGet.has("data")) {
                JsonArray results = jsonGet.getAsJsonArray("data");
                if (results.size() > 0) {
                    return new Result(true, null); // Déjà existant
                }
            }

            // Création du Salary Slip avec docstatus = 1 directement
            JsonObject data = new JsonObject();
            data.addProperty("doctype", "Salary Slip");
            data.addProperty("employee", employeeName);
            data.addProperty("employee_name", getEmployeeFullNameByEmployeeNumber(paie.getRefEmploye(), sid));
            data.addProperty("salary_structure", paie.getSalaire());
            data.addProperty("start_date", new SimpleDateFormat("yyyy-MM-dd").format(moisDebut));
            data.addProperty("end_date", new SimpleDateFormat("yyyy-MM-dd").format(moisFin));
            data.addProperty("posting_date", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            data.addProperty("docstatus", 1); // DIRECTEMENT SOUMIS

            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Slip"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "POST Salary Slip failed: " + responsePost.body());
            }

            return new Result(true, null);

        } catch (Exception ex) {
            Boolean resetdata = resetdata(getSid());
            return new Result(false, "Exception: " + ex.getMessage());
        }
    }

    private String getEmployeeNameByEmployeeNumber(String employeeNumber, String sid) throws Exception {
        String filter = "[[\"employee_number\",\"=\",\"" + employeeNumber + "\"]]";
        String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());
        String url = apiBaseUrl + "Employee?filters=" + encodedFilter;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur lors de la récupération de l'employé: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (json.has("data")) {
            JsonArray data = json.getAsJsonArray("data");
            if (data.size() > 0) {
                JsonObject employee = data.get(0).getAsJsonObject();
                String employeeName = employee.get("name").getAsString();
                System.out.println("Employee trouvé: " + employeeName + " pour le numéro: " + employeeNumber);
                return employeeName;
            }
        }

        throw new Exception("Aucun employé trouvé avec le numéro : " + employeeNumber);
    }

    private String getEmployeeFullNameByEmployeeNumber(String employeeNumber, String sid) throws Exception {
        String filter = "[[\"employee_number\",\"=\",\"" + employeeNumber + "\"]]";
        String encodedFilter = URLEncoder.encode(filter, StandardCharsets.UTF_8.toString());
        String fields = URLEncoder.encode("[\"employee_name\"]", StandardCharsets.UTF_8.toString());
        String url = apiBaseUrl + "Employee?filters=" + encodedFilter + "&fields=" + fields;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Cookie", "sid=" + sid)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("Erreur lors de la récupération du nom complet de l'employé: " + response.body());
        }

        JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();

        if (json.has("data")) {
            JsonArray data = json.getAsJsonArray("data");
            if (data.size() > 0) {
                JsonObject employee = data.get(0).getAsJsonObject();
                if (employee.has("employee_name")) {
                    return employee.get("employee_name").getAsString();
                }
            }
        }

        throw new Exception("Aucun employé trouvé avec le numéro : " + employeeNumber);
    }

    /**
     * Réinitialise les données en appelant l'API de réinitialisation
     * 
     * @param sid L'identifiant de session
     * @return true si la réinitialisation a réussi, false sinon
     */
    public static boolean resetdata(String sid) {
        try {
            // Désactiver la réinitialisation des composants par défaut
            String apiUrl = "http://172.25.36.0:8000/api/method/hrms.api.reset_data.reset_data";

            // Création du corps de la requête
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("preserve_defaults", true);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Cookie", "sid=" + sid)
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(requestBody)))
                    .build();

            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Vérification du statut de réponse
            if (response.statusCode() == 200) {
                JsonElement jsonResponse = new Gson().fromJson(response.body(), JsonElement.class);
                if (jsonResponse != null && jsonResponse.isJsonObject()) {
                    JsonObject jsonObj = jsonResponse.getAsJsonObject();
                    if (jsonObj.has("message")) {
                        JsonElement message = jsonObj.get("message");
                        if (message.isJsonPrimitive()) {
                            return message.getAsString().toLowerCase().contains("success");
                        }
                    }
                }
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Classes DTO
    public static class EmployeDto {
        private String ref;
        private String nom;
        private String prenom;
        private String genre;
        private Date dateEmbauche;
        private Date dateNaissance;
        private String company;

        public EmployeDto() {
        }

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

    public static class StructureDto {
        private String salaryStructure;
        private String name;
        private String abbr;
        private String type;
        private String valeur;
        private String company;

        public StructureDto() {
        }

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

        public String getValeur() {
            return valeur;
        }

        public void setValeur(String valeur) {
            this.valeur = valeur;
        }

        public String getCompany() {
            return company;
        }

        public void setCompany(String company) {
            this.company = company;
        }
    }

    public static class PaieDto {
        private Date mois;
        private String refEmploye;
        private int salaireBase;
        private String salaire;

        public PaieDto() {
        }

        public Date getMois() {
            return mois;
        }

        public void setMois(Date mois) {
            this.mois = mois;
        }

        public String getRefEmploye() {
            return refEmploye;
        }

        public void setRefEmploye(String refEmploye) {
            this.refEmploye = refEmploye;
        }

        public int getSalaireBase() {
            return salaireBase;
        }

        public void setSalaireBase(int salaireBase) {
            this.salaireBase = salaireBase;
        }

        public String getSalaire() {
            return salaire;
        }

        public void setSalaire(String salaire) {
            this.salaire = salaire;
        }
    }

    // Classes de résultats
    public static class Result {
        public final boolean success;
        public final String errorMessage;

        public Result(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    public static class GenderResult {
        public final boolean success;
        public final String translatedGender;
        public final String errorMessage;

        public GenderResult(boolean success, String translatedGender, String errorMessage) {
            this.success = success;
            this.translatedGender = translatedGender;
            this.errorMessage = errorMessage;
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");

        try {
            boolean result = resetdata(sid);

            if (result) {
                response.sendRedirect("dashboard.jsp");
            }
        } catch (Exception e) {
            session.setAttribute("errorMsg", "Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }

}