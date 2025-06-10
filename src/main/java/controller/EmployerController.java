package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet(name = "EmployerController", value = "/Employer-Controller")
public class EmployerController extends HttpServlet {

    private static final String API_BASE_URL = "http://172.25.36.0:8000/api/resource/Employee";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter out = resp.getWriter();
        try {
            String findemployeename = req.getParameter("employe_name");
            String sid = (String) req.getSession().getAttribute("sid");
            if (sid == null) {
                resp.sendRedirect("login.jsp");
                return;
            }

            String filters = "[]";
            if (findemployeename != null && !findemployeename.trim().isEmpty()) {
                // Construire le filtre JSON avec un like sur employee_name
                String filterJson = "[[\"employee_name\", \"like\", \"%"+ findemployeename.trim() +"%\"]]";
                filters = URLEncoder.encode(filterJson, StandardCharsets.UTF_8);
            }

            String urlStr = API_BASE_URL + "?fields=[\"name\",\"employee_name\",\"department\",\"designation\",\"status\"]&filters=" + filters;

            URL url = new URL(urlStr);
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
                JsonElement jsonElement = JsonParser.parseString(response.toString());
                JsonObject jsonObj = jsonElement.getAsJsonObject();
                JsonArray employees = jsonObj.getAsJsonArray("data");

                // Nettoyer les données pour assurer que tous les champs existent
                for (JsonElement element : employees) {
                    JsonObject employee = element.getAsJsonObject();
                    if (!employee.has("department")) employee.addProperty("department", "");
                    if (!employee.has("designation")) employee.addProperty("designation", "");
                    if (!employee.has("status")) employee.addProperty("status", "");
                }

                req.setAttribute("employeesData", jsonObj.toString());
                req.getRequestDispatcher("employees.jsp").forward(req, resp);
            } else {
                String errorMessage = "Error: " + responseCode;
                try {
                    JsonElement jsonElement = JsonParser.parseString(response.toString());
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
