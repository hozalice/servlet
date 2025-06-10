package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.google.gson.*;
import jakarta.servlet.http.HttpSession;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@WebServlet("/graphe")
public class GrapheController extends HttpServlet {

    private static final String API_BASE_URL = "http://erpnext.localhost:8000/api/resource/Salary%20Slip";


    // Fonction utilitaire pour faire un GET HTTP avec cookie de session
    private String callApiWithSessionCookie(String urlStr, String sessionId) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sessionId);

        // Lecture de la réponse
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line);
        }
        in.close();

        return response.toString();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try {
            // Récupérer le SID depuis le cookie de la requête client
            HttpSession session = req.getSession();
            String sid = (String) session.getAttribute("sid");

            if (sid == null) {
                resp.sendRedirect("index.jsp");
                return;
            }

            // 1. Récupérer la liste des fiches (seulement leurs noms)
            String listUrl = API_BASE_URL + "?limit_page_length=0&fields=[\"name\"]";
            String listResponse = callApiWithSessionCookie(listUrl, sid);

            JsonObject listJson = JsonParser.parseString(listResponse).getAsJsonObject();
            JsonArray dataArray = listJson.getAsJsonArray("data");

            List<JsonObject> allDetails = new ArrayList<>();

            // 2. Pour chaque fiche, récupérer les détails complets
            for (JsonElement elem : dataArray) {
                String name = elem.getAsJsonObject().get("name").getAsString();

                // Encodage de l'URL : remplacer '+' par '%20' pour l'espace dans les segments d'URL
                String encodedName = URLEncoder.encode(name, StandardCharsets.UTF_8.toString()).replace("+", "%20");

                String detailUrl = API_BASE_URL + "/" + encodedName;
                String detailResponse = callApiWithSessionCookie(detailUrl, sid);
                JsonObject detailJson = JsonParser.parseString(detailResponse).getAsJsonObject().getAsJsonObject("data");

                allDetails.add(detailJson);
            }

            // 3. Retourner la liste complète en JSON
            String jsonResult = new Gson().toJson(allDetails);
            //resp.setContentType("application/json");
            //resp.setCharacterEncoding("UTF-8");
            //resp.getWriter().write(jsonResult);
            req.setAttribute("allsalaryslip",jsonResult);
            req.getRequestDispatcher("/graphe-salaire.jsp").forward(req,resp);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des fiches de paie.");
            } catch (Exception ignored) {}
        }
    }
}
