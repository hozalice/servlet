<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.io.PrintWriter" %>
<!DOCTYPE html>
<html>
<head>
  <title>Liste des Employés</title>
  <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<%@ include file="sidebar.jsp" %>
<div class="container mt-4">
  <h2>Liste des Employés</h2>

  <!-- Formulaire de recherche -->
  <form action="Employer-Controller" method="get" class="mb-4">
    <div class="input-group">
      <input type="text" name="search" class="form-control" placeholder="Rechercher un employé...">
      <button type="submit" class="btn btn-primary">Rechercher</button>
    </div>
  </form>

  <!-- Tableau des employés -->
  <table class="table table-striped">
    <thead>
    <tr>
      <th>name</th>
      <th>employee_name</th>
      <th>posting_date</th>
      <th>start_date</th>
      <th>end_date</th>
      <th>net_pay</th>
      <th>gross_pay</th>
      <th>status</th>
    </tr>
    </thead>
    <tbody>
    <%
      String employeesData = (String) request.getAttribute("fiche_employerData");
      if (employeesData != null && !employeesData.isEmpty()) {
        try {
          JsonParser parser = new JsonParser();
          JsonObject jsonResponse = parser.parse(employeesData).getAsJsonObject();

          // Vérifier si la clé "data" existe et est un tableau
          if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
            JsonArray fiche_employerData = jsonResponse.getAsJsonArray("data");

            for (JsonElement fiche_employer : fiche_employerData) {
              JsonObject fiche = fiche_employer.getAsJsonObject();

              String name = fiche.has("name") && !fiche.get("name").isJsonNull()
                      ? fiche.get("name").getAsString()
                      : "";

              String employee_name = fiche.has("employee_name") && !fiche.get("employee_name").isJsonNull()
                      ? fiche.get("employee_name").getAsString()
                      : "";

              String posting_date = fiche.has("posting_date") && !fiche.get("posting_date").isJsonNull()
                      ? fiche.get("posting_date").getAsString()
                      : "";

              String start_date = fiche.has("start_date") && !fiche.get("start_date").isJsonNull()
                      ? fiche.get("start_date").getAsString()
                      : "";

              String end_date = fiche.has("end_date") && !fiche.get("end_date").isJsonNull()
                      ? fiche.get("end_date").getAsString()
                      : "";

              String net_pay = fiche.has("net_pay") && !fiche.get("net_pay").isJsonNull()
                      ? fiche.get("net_pay").getAsString()
                      : "";

              String gross_pay = fiche.has("gross_pay") && !fiche.get("gross_pay").isJsonNull()
                      ? fiche.get("gross_pay").getAsString()
                      : "";

              String status = fiche.has("status") && !fiche.get("status").isJsonNull()
                      ? fiche.get("status").getAsString()
                      : "inconnue";
    %>
    <tr>
      <td><%= name %></td>
      <td><%= employee_name %></td>
      <td><%= posting_date %></td>
      <td><%= start_date %></td>
      <td><%= end_date %></td>
      <td><%= net_pay %></td>
      <td><%= gross_pay %></td>
      <td><%= status %></td>
    </tr>
    <%
      }
    } else {
    %>
    <tr><td colspan="8">Aucune donnée disponible.</td></tr>
    <%
        }
      } catch (Exception e) {
       e.printStackTrace();
      }
    } else {
    %>
    <tr><td colspan="8">Aucune donnée reçue.</td></tr>
    <%
      }
    %>
    </tbody>
  </table>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
