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
    private final String apiBaseUrl = "http://erpnext.localhost:8000/api/resource/";
    private final Gson gson;
    private final SimpleDateFormat dateFormat;

    public Import() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
        this.dateFormat = new SimpleDateFormat("dd/MM/yyyy");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Configuration de la réponse
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Gestion de la session et du SID
        HttpSession session = request.getSession(true); // true pour créer si nécessaire
        // ⚠️ Récupération du sid depuis la session. Assurez-vous qu'il provient d'un login valide ERPNext
        String sid = (String) session.getAttribute("sid");
        System.out.println("SID: " + sid);

        if (sid == null || sid.isEmpty()) {
            sendErrorResponse(response, "Session invalide. Veuillez vous reconnecter.");
            return;
        }

        // Ajout du cookie SID dans la réponse
        Cookie cookie = new Cookie("sid", sid);
        cookie.setMaxAge(-1); // Cookie de session
        response.addCookie(cookie);

        try {
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
                    request
            );

            JsonObject result = new JsonObject();
            if (errors.isEmpty()) {
                result.addProperty("success", true);
                result.addProperty("message", "Import réalisé avec succès");
            } else {
                result.addProperty("success", false);
                result.add("errors", gson.toJsonTree(errors));
            }

            response.getWriter().write(gson.toJson(result));

        } catch (Exception e) {
            e.printStackTrace();
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

    private String getSessionId(HttpServletRequest request) throws Exception {
        // Essayer d'obtenir le SID depuis la session
        HttpSession session = request.getSession();
        String sid = (String) session.getAttribute("sid");
        System.out.println("sid d ato anaty fanction "+ sid);
        return sid;
    }

    public List<String> importDataAsync(InputStream employesCsv, InputStream structuresCsv,
                                        InputStream paiesCsv, HttpServletRequest request) throws Exception {
        List<String> erreurs = new ArrayList<>();
        HttpSession session = request.getSession(true); // true pour créer si nécessaire
        // ⚠️ Récupération du sid depuis la session. Assurez-vous qu'il provient d'un login valide ERPNext
        String sid = (String) session.getAttribute("sid");
        String Username = (String) session.getAttribute("loggedInUser");
        System.out.println("SID: " + sid);
        System.out.println(Username);

        // 1. Charger et valider les employés
        List<EmployeDto> employes = lireEmployesAsync(employesCsv, erreurs);
        if (!erreurs.isEmpty()) return erreurs;

        // 2. Charger et valider les structures salariales
        List<StructureDto> structures = lireStructuresAsync(structuresCsv, erreurs);
        if (!erreurs.isEmpty()) return erreurs;

        // 3. Charger et valider les paies
        List<PaieDto> paies = lirePaiesAsync(paiesCsv, erreurs);
        if (!erreurs.isEmpty()) return erreurs;

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
        if (!erreurs.isEmpty()) return erreurs;

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
        if (!erreurs.isEmpty()) return erreurs;

        for (EmployeDto emp : employes) {
            Result result = creerEmploye(emp, sid);
            if (!result.success) {
                erreurs.add("Erreur création employe pour " + emp.getRef() + " - " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty()) return erreurs;

        // 6. Créer les structures salariales regroupées
        Map<String, List<StructureDto>> groupes = structures.stream()
                .collect(Collectors.groupingBy(StructureDto::getSalaryStructure));

        for (Map.Entry<String, List<StructureDto>> entry : groupes.entrySet()) {
            Result result = creerStructureRegroupee(entry.getKey(), entry.getValue(), sid);
            if (!result.success) {
                erreurs.add("Erreur création de " + entry.getKey() + " : " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty()) return erreurs;

        // 7. Assigner les structures salariales
        for (PaieDto p : paies) {
            Result result = assignerStructureSalaire(p, sid);
            if (!result.success) {
                erreurs.add("Erreur assignation structure pour : " + p.getRefEmploye() +
                        " mois " + dateFormat.format(p.getMois()) + " - Détail: " + result.errorMessage);
            }
        }
        if (!erreurs.isEmpty()) return erreurs;

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
                if (lineNumber == 1) continue; // Skip header

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
                if (lineNumber == 1) continue; // Skip header

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
                if (lineNumber == 1) continue; // Skip header

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
                    erreurs.add("Fichier Paies : ligne " + lineNumber + " - Données invalides");
                }
            }
        }

        return liste;
    }
    private Result creerCompanySiNonExistante(String companyName, String sid) {
        try {
            String baseUrl = "http://erpnext.localhost:8000/api/resource/";

            // Vérification de l'existence d'une Holiday List
            String holidayListEndpoint = "Holiday%20List";
            String filtersHoliday = URLEncoder.encode("[[\"holiday_list_name\",\"=\",\"holiday\"]]", StandardCharsets.UTF_8);
            String urlCheckHoliday = baseUrl + holidayListEndpoint + "?filters=" + filtersHoliday;

            HttpRequest requestCheckHoliday = HttpRequest.newBuilder()
                    .uri(URI.create(urlCheckHoliday))
                    .GET()
                    .header("Cookie", "sid=" + sid)
                    .build();

            HttpResponse<String> responseCheckHoliday = httpClient.send(requestCheckHoliday, HttpResponse.BodyHandlers.ofString());
            if (responseCheckHoliday.statusCode() != 200) {
                return new Result(false, "Échec vérification Holiday List : " + responseCheckHoliday.statusCode());
            }

            // Vérification si la Company existe déjà
            String filtersCompany = URLEncoder.encode("[[\"name\",\"=\",\"" + companyName + "\"]]", StandardCharsets.UTF_8);
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
                    .header("Cookie", "sid=" + sid)  // ✅ Authentification correcte ici
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(companyData)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "POST Company failed: " + responsePost.statusCode() + " - " + responsePost.body());
            }

            return new Result(true, null);

        } catch (Exception e) {
            return new Result(false, "Erreur création company: " + companyName + " - Détail: " + e.getMessage());
        }
    }
    private Result creerSalaryComponentSiNonExistant(String name, String type, String abbr, String sid) {
        try {
            String baseUrl = "http://erpnext.localhost:8000/api/resource/";
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
            return new Result(false, "Exception création employé: " + ex.getMessage());
        }
    }

    private Result creerStructureRegroupee(String salaryStructure, List<StructureDto> lignes, String sid) {
        try {
            JsonArray earnings = new JsonArray();
            JsonArray deductions = new JsonArray();
            String company = lignes.isEmpty() ? null : lignes.get(0).getCompany();

            for (StructureDto s : lignes) {
                Result compResult = creerSalaryComponentSiNonExistant(s.getName(), s.getType(), s.getAbbr(), sid);
                if (!compResult.success) {
                    return compResult;
                }

                JsonObject line = new JsonObject();
                line.addProperty("salary_component", s.getName());
                line.addProperty("abbr", s.getAbbr());
                line.addProperty("formula", s.getValeur());
                line.addProperty("is_tax_applicable", 1);
                line.addProperty("amount_based_on_formula", 1);
                line.addProperty("depends_on_payment_days", 0);

                if (s.getType().toLowerCase().equals("earning")) {
                    earnings.add(line);
                } else if (s.getType().toLowerCase().equals("deduction")) {
                    deductions.add(line);
                }
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("doctype", "Salary Structure");
            payload.addProperty("name", salaryStructure);
            payload.addProperty("is_active", "Yes");
            payload.add("earnings", earnings);
            payload.add("deductions", deductions);
            payload.addProperty("company", company);

            // URL corrigée avec %20 pour espace
            HttpRequest requestPost = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure"))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(payload)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "POST Salary Structure failed: " + responsePost.body());
            }

            JsonObject submitPayload = new JsonObject();
            submitPayload.addProperty("docstatus", 1);

            // URL corrigée avec %20 pour espace + ajout du sid dans le header
            HttpRequest requestSubmit = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure/" + salaryStructure))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(submitPayload)))
                    .build();

            HttpResponse<String> responseSubmit = httpClient.send(requestSubmit, HttpResponse.BodyHandlers.ofString());

            if (responseSubmit.statusCode() != 200) {
                return new Result(false, "Submit Salary Structure failed: " + responseSubmit.body());
            }

            return new Result(true, null);

        } catch (Exception e) {
            return new Result(false, "Exception: " + e.getMessage());
        }
    }


    private Result assignerStructureSalaire(PaieDto paie, String sid) {
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(paie.getMois());
            cal.set(Calendar.DAY_OF_MONTH, 1);
            Date moisDebut = cal.getTime();
            System.out.println("reference emplyer ----------------------------------------->"+paie.getRefEmploye());
            String employeeName = getEmployeeNameByEmployeeNumber(paie.getRefEmploye(), sid); // Ajout du paramètre sid

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
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(data)))
                    .build();

            HttpResponse<String> responsePost = httpClient.send(requestPost, HttpResponse.BodyHandlers.ofString());

            if (responsePost.statusCode() != 200) {
                return new Result(false, "POST Salary Structure Assignment failed: " + responsePost.body());
            }

            JsonParser parser = new JsonParser();
            JsonElement jsonElement = parser.parse(responsePost.body());
            JsonObject jsonPost = jsonElement.getAsJsonObject();
            String name = jsonPost.getAsJsonObject("data").get("name").getAsString();

            JsonObject submitPayload = new JsonObject();
            submitPayload.addProperty("docstatus", 1);

            // URL encodée aussi ici + potentiellement encodage de 'name' si besoin
            HttpRequest requestSubmit = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "Salary%20Structure%20Assignment/" + name))
                    .header("Content-Type", "application/json")
                    .header("Cookie", "sid=" + sid)
                    .PUT(HttpRequest.BodyPublishers.ofString(gson.toJson(submitPayload)))
                    .build();

            HttpResponse<String> responseSubmit = httpClient.send(requestSubmit, HttpResponse.BodyHandlers.ofString());

            if (responseSubmit.statusCode() != 200) {
                return new Result(false, "Submit Salary Structure Assignment failed: " + responseSubmit.body());
            }

            return new Result(true, null);

        } catch (Exception ex) {
            return new Result(false, "Exception: " + ex.getMessage());
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
    public static boolean resetdata(String sid){

        return true;
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

        public EmployeDto() {}

        public String getRef() { return ref; }
        public void setRef(String ref) { this.ref = ref; }

        public String getNom() { return nom; }
        public void setNom(String nom) { this.nom = nom; }

        public String getPrenom() { return prenom; }
        public void setPrenom(String prenom) { this.prenom = prenom; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public Date getDateEmbauche() { return dateEmbauche; }
        public void setDateEmbauche(Date dateEmbauche) { this.dateEmbauche = dateEmbauche; }

        public Date getDateNaissance() { return dateNaissance; }
        public void setDateNaissance(Date dateNaissance) { this.dateNaissance = dateNaissance; }

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
    }

    public static class StructureDto {
        private String salaryStructure;
        private String name;
        private String abbr;
        private String type;
        private String valeur;
        private String company;

        public StructureDto() {}

        public String getSalaryStructure() { return salaryStructure; }
        public void setSalaryStructure(String salaryStructure) { this.salaryStructure = salaryStructure; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getAbbr() { return abbr; }
        public void setAbbr(String abbr) { this.abbr = abbr; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getValeur() { return valeur; }
        public void setValeur(String valeur) { this.valeur = valeur; }

        public String getCompany() { return company; }
        public void setCompany(String company) { this.company = company; }
    }

    public static class PaieDto {
        private Date mois;
        private String refEmploye;
        private int salaireBase;
        private String salaire;

        public PaieDto() {}

        public Date getMois() { return mois; }
        public void setMois(Date mois) { this.mois = mois; }

        public String getRefEmploye() { return refEmploye; }
        public void setRefEmploye(String refEmploye) { this.refEmploye = refEmploye; }

        public int getSalaireBase() { return salaireBase; }
        public void setSalaireBase(int salaireBase) { this.salaireBase = salaireBase; }

        public String getSalaire() { return salaire; }
        public void setSalaire(String salaire) { this.salaire = salaire; }
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


}