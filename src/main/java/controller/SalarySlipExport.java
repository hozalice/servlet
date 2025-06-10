package controller;

import com.google.gson.*;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

@WebServlet("/Salary-Export")
public class SalarySlipExport extends HttpServlet {

    private static final String API_URL = "http://172.25.36.0:8000/api/resource/Salary%20Slip/";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String sid = (String) req.getSession().getAttribute("sid");
        String slipName = req.getParameter("namesalaryslip");

        if (sid == null || slipName == null || slipName.trim().isEmpty()) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Session ou nom de fiche manquant");
            return;
        }

        try {
            JsonObject slipData = fetchSalarySlipData(sid, slipName);
            if (slipData == null) {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Fiche de paie non trouvée");
                return;
            }
            generatePdf(resp, slipData, slipName);
        } catch (Exception e) {
            e.printStackTrace();
            resp.setContentType("text/plain");
            resp.getWriter().write("Erreur : " + e.getMessage());
        }
    }

    private JsonObject fetchSalarySlipData(String sid, String slipName) throws IOException {
        // Encodage UTF-8 du paramètre slipName
        String encodedName = URLEncoder.encode(slipName, StandardCharsets.UTF_8);
        // Remplacer '+' par '%20' pour garder les espaces correctement
        encodedName = encodedName.replace("+", "%20");
        // Remettre les slash '/' encodés en %2F à l'identique (car URLEncoder encode les '/')
        encodedName = encodedName.replace("%2F", "/");

        URL url = new URL(API_URL + encodedName);
        System.out.println("Requête API : " + url);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "sid=" + sid);
        conn.setRequestProperty("Accept", "application/json");

        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Erreur API : " + status);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
            return response.has("data") && response.get("data").isJsonObject()
                    ? response.getAsJsonObject("data")
                    : null;
        }
    }

    private void generatePdf(HttpServletResponse resp, JsonObject data, String name) throws Exception {
        resp.setContentType("application/pdf");
        resp.setHeader("Content-Disposition", "attachment; filename=\"Fiche-Paie-" + name + ".pdf\"");

        Document document = new Document();
        PdfWriter.getInstance(document, resp.getOutputStream());
        document.open();

        Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD, BaseColor.DARK_GRAY);
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font normalFont = new Font(Font.FontFamily.HELVETICA, 12);

        Paragraph title = new Paragraph("FICHE DE PAIE", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Informations de base
        PdfPTable infoTable = new PdfPTable(2);
        infoTable.setWidthPercentage(100);
        infoTable.setSpacingAfter(15);

        infoTable.addCell(cell("Nom Employé :", labelFont));
        infoTable.addCell(cell(getStringSafe(data, "employee_name"), normalFont));

        infoTable.addCell(cell("Employé ID :", labelFont));
        infoTable.addCell(cell(getStringSafe(data, "employee"), normalFont));

        infoTable.addCell(cell("Période :", labelFont));
        infoTable.addCell(cell(
                getStringSafe(data, "start_date") + " au " + getStringSafe(data, "end_date"),
                normalFont));

        infoTable.addCell(cell("Département :", labelFont));
        infoTable.addCell(cell(getStringSafe(data, "department"), normalFont));

        document.add(infoTable);

        // Revenus
        document.add(new Paragraph("Revenus", labelFont));
        PdfPTable earningsTable = new PdfPTable(2);
        earningsTable.setWidthPercentage(100);
        earningsTable.setSpacingAfter(10);
        earningsTable.addCell(headerCell("Libellé"));
        earningsTable.addCell(headerCell("Montant"));

        JsonArray earnings = data.has("earnings") && data.get("earnings").isJsonArray()
                ? data.getAsJsonArray("earnings")
                : new JsonArray();

        for (JsonElement elem : earnings) {
            JsonObject e = elem.getAsJsonObject();
            earningsTable.addCell(cell(getStringSafe(e, "salary_component"), normalFont));
            earningsTable.addCell(cell(getStringSafe(e, "amount"), normalFont));
        }
        document.add(earningsTable);

        // Déductions
        document.add(new Paragraph("Déductions", labelFont));
        PdfPTable deductionTable = new PdfPTable(2);
        deductionTable.setWidthPercentage(100);
        deductionTable.setSpacingAfter(10);
        deductionTable.addCell(headerCell("Libellé"));
        deductionTable.addCell(headerCell("Montant"));

        JsonArray deductions = data.has("deductions") && data.get("deductions").isJsonArray()
                ? data.getAsJsonArray("deductions")
                : new JsonArray();

        for (JsonElement elem : deductions) {
            JsonObject d = elem.getAsJsonObject();
            deductionTable.addCell(cell(getStringSafe(d, "salary_component"), normalFont));
            deductionTable.addCell(cell(getStringSafe(d, "amount"), normalFont));
        }
        document.add(deductionTable);

        // Résumé
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(100);
        summary.setSpacingBefore(20);

        summary.addCell(cell("Salaire Brut", labelFont));
        summary.addCell(cell(getStringSafe(data, "gross_pay"), normalFont));

        summary.addCell(cell("Déductions Totales", labelFont));
        summary.addCell(cell(getStringSafe(data, "total_deduction"), normalFont));

        summary.addCell(cell("Salaire Net", labelFont));
        summary.addCell(cell(getStringSafe(data, "net_pay"), normalFont));

        document.add(summary);

        document.close();
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell headerCell(String text) {
        Font font = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD, BaseColor.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(new BaseColor(0, 102, 204));
        cell.setPadding(6);
        return cell;
    }

    /**
     * Retourne la valeur String associée à la clé dans un JsonObject, ou "" si absente ou null.
     */
    private String getStringSafe(JsonObject obj, String key) {
        if (obj == null || key == null) return "";
        JsonElement elem = obj.get(key);
        if (elem != null && !elem.isJsonNull()) {
            try {
                return elem.getAsString();
            } catch (Exception e) {
                return "";
            }
        }
        return "";
    }
}
