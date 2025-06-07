// package controller;

// import jakarta.servlet.ServletException;
// import jakarta.servlet.annotation.MultipartConfig;
// import jakarta.servlet.annotation.WebServlet;
// import jakarta.servlet.http.*;
// import java.io.IOException;
// import java.io.InputStream;
// import com.google.gson.Gson;

// @WebServlet("/ImportChat")
// @MultipartConfig
// public class SalaryImportController extends HttpServlet {
// private static final long serialVersionUID = 1L;
// private final SalaryImportService importService = new SalaryImportService();
// private final Gson gson = new Gson();

// @Override
// protected void doPost(HttpServletRequest request, HttpServletResponse
// response)
// throws ServletException, IOException {

// response.setContentType("application/json");
// response.setCharacterEncoding("UTF-8");

// try {
// // Récupérer les fichiers téléchargés
// Part employeesFile = request.getPart("employees");
// Part componentsFile = request.getPart("structures");
// Part salarySlipsFile = request.getPart("paies");

// // Récupérer le SID de la session
// String sessionId = request.getParameter("sid");
// HttpSession session = request.getSession(true);
// sessionId = (String) session.getAttribute("sid");

// if (sessionId == null || sessionId.trim().isEmpty()) {
// sendError(response, "Session ID manquant", 400);
// return;
// }

// // Vérifier que tous les fichiers sont présents
// if (employeesFile == null || componentsFile == null || salarySlipsFile ==
// null) {
// sendError(response, "Tous les fichiers sont requis (employees, components,
// salarySlips)", 400);
// return;
// }

// // Traiter les fichiers
// try (InputStream employeesStream = employeesFile.getInputStream();
// InputStream componentsStream = componentsFile.getInputStream();
// InputStream salarySlipsStream = salarySlipsFile.getInputStream()) {

// // Appeler le service d'importation
// SalaryImportService.ImportResult result = importService.processImport(
// employeesStream, componentsStream, salarySlipsStream, sessionId);

// // Renvoyer le résultat
// response.setStatus(result.success ? 200 : 400);
// response.getWriter().write(gson.toJson(result));

// } catch (Exception e) {
// sendError(response, "Erreur lors du traitement des fichiers: " +
// e.getMessage(), 500);
// }

// } catch (Exception e) {
// sendError(response, "Erreur de traitement: " + e.getMessage(), 500);
// }
// }

// private void sendError(HttpServletResponse response, String message, int
// status)
// throws IOException {
// response.setStatus(status);
// response.getWriter().write(gson.toJson(
// new SalaryImportService.ImportResult(false, message, null)));
// }
// }
