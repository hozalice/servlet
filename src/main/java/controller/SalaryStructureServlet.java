package controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

@WebServlet("/salary-structure")
public class SalaryStructureServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String sid = (String) request.getSession().getAttribute("sid");
        if (sid == null) {
            response.sendRedirect("login.jsp");
            return;
        }

        URL url = new URL("http://172.25.36.0:8000/api/method/frappe.desk.form.load.getdoctype?doctype=Salary Structure");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Cookie", "sid=" + sid);

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            InputStreamReader reader = new InputStreamReader(connection.getInputStream());
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();

            // Vérifie et extrait les champs depuis docs[0].fields
            JsonArray fields = null;
            if (json.has("docs")) {
                JsonArray docs = json.getAsJsonArray("docs");
                if (docs.size() > 0) {
                    JsonObject doc = docs.get(0).getAsJsonObject();
                    if (doc.has("fields")) {
                        fields = doc.getAsJsonArray("fields");
                    }
                }
            }

            if (fields != null) {
                System.out.println(fields);
                request.setAttribute("fields", fields);
                request.getRequestDispatcher("/salaryStructure.jsp").forward(request, response);
            } else {
                request.setAttribute("error", "Impossible de récupérer les champs.");
                request.getRequestDispatcher("/error.jsp").forward(request, response);
            }
        } else {
            request.setAttribute("error", "Erreur lors de la récupération de la structure : code " + responseCode);
            request.getRequestDispatcher("/error.jsp").forward(request, response);
        }
    }
}
