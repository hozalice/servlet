package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@WebServlet("/Salary-Export")
public class SalarySlipExport extends HttpServlet {

    // URL de l'API d'exportation PDF
    private static final String API_EXPORT_URL = "http://172.25.36.0:8000/api/method/frappe.utils.print_format.download_pdf";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sid = (String) req.getSession().getAttribute("sid");
        if (sid == null) {
            resp.sendRedirect("index.jsp");
            return;
        }

        String nameSalarySlip = req.getParameter("namesalaryslip");
        if (nameSalarySlip == null || nameSalarySlip.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Le nom de la fiche de paie est requis.");
            return;
        }

        // Construction de l'URL d'exportation du PDF
        String urlStr = API_EXPORT_URL +
                "?doctype=Salary%20Slip" +
                "&name=" + URLEncoder.encode(nameSalarySlip, StandardCharsets.UTF_8) +
                "&format=Standard";

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Utiliser le SID pour l'authentification
        conn.setRequestProperty("Cookie", "sid=" + sid);
        conn.setRequestProperty("Accept", "application/pdf");
        conn.setRequestProperty("User-Agent", "Java-Client");

        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            // Préparer la réponse pour le téléchargement
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"SalarySlip-" + nameSalarySlip + ".pdf\"");

            try (InputStream inputStream = conn.getInputStream();
                 OutputStream outputStream = resp.getOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.flush();
            }

        } else {
            resp.setContentType("text/plain");
            resp.setStatus(responseCode);
            resp.getWriter().write("Erreur lors de l’export du PDF. Code HTTP : " + responseCode);
        }

        conn.disconnect();
    }
}
