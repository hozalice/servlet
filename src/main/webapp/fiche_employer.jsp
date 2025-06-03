<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.io.PrintWriter" %>
<!DOCTYPE html>
<html>
<head>
  <title>Fiche Employé</title>
  <style>
    body {
      font-family: Arial, sans-serif;
      background-color: #f4f6f9;
      margin: 0;
      padding: 0;
    }

    .container {
      margin: 40px auto;
      padding: 20px;
      max-width: 1200px;
      background-color: #fff;
      border-radius: 12px;
      box-shadow: 0 4px 10px rgba(0, 0, 0, 0.1);
    }

    h2 {
      text-align: center;
      margin-bottom: 20px;
      color: #333;
    }

    .input-group {
      display: flex;
      justify-content: center;
      gap: 10px;
      margin-bottom: 30px;
      flex-wrap: wrap;
    }

    .input-group input, .input-group button {
      padding: 10px;
      font-size: 14px;
      border-radius: 6px;
      border: 1px solid #ccc;
    }

    .input-group button {
      background-color: #007bff;
      color: #fff;
      border: none;
      cursor: pointer;
    }

    .input-group button:hover {
      background-color: #0056b3;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      border-radius: 10px;
      overflow: hidden;
      background-color: #fafafa;
    }

    th, td {
      padding: 12px 15px;
      border-bottom: 1px solid #ddd;
      text-align: center;
    }

    th {
      background-color: #007bff;
      color: white;
      font-weight: normal;
    }

    tr:hover {
      background-color: #f1f1f1;
    }

    .voir-plus {
      text-decoration: none;
      color: #007bff;
      font-weight: bold;
    }

    .voir-plus:hover {
      text-decoration: underline;
    }

    .alert {
      margin-top: 20px;
      text-align: center;
      color: red;
      font-weight: bold;
    }
  </style>
</head>
<body>

<%@ include file="sidebar.jsp" %>

<div class="container">
  <h2>Fiches de Paie des Employés</h2>

  <%
    String employeesData = (String) request.getAttribute("fiche_employerData");
    String firstEmployeeId = "";

    if (employeesData != null && !employeesData.isEmpty()) {
      try {
        JsonParser parser = new JsonParser();
        JsonObject jsonResponse = parser.parse(employeesData).getAsJsonObject();

        if (jsonResponse.has("data") && jsonResponse.get("data").isJsonArray()) {
          JsonArray fiche_employerData = jsonResponse.getAsJsonArray("data");

          if (fiche_employerData.size() > 0) {
            JsonObject firstEmployee = fiche_employerData.get(0).getAsJsonObject();
            if (firstEmployee.has("employee")) {
              firstEmployeeId = firstEmployee.get("employee").getAsString();
            }
  %>

  <form action="/Salary-Slip" method="get">
    <div class="input-group">
      <input type="number" name="mois" placeholder="Mois (1-12)" min="1" max="12" >
      <input type="number" name="annee" placeholder="Année (ex : 2025)" min="1950" >
      <input type="hidden" name="employer" value="<%= firstEmployeeId %>">
      <button type="submit">Rechercher</button>
    </div>
  </form>

  <table>
    <thead>
    <tr>
      <th>ID</th>
      <th>Name</th>
      <th>Employé</th>
      <th>Début</th>
      <th>Fin</th>
      <th>Date publication</th>
      <th>Salaire Brut</th>
      <th>Salaire Net</th>
      <th>Statut</th>
      <th>Action</th>
    </tr>
    </thead>
    <tbody>
    <%
      for (JsonElement fiche_employer : fiche_employerData) {
        JsonObject fiche = fiche_employer.getAsJsonObject();
        String employer = fiche.has("employee") ? fiche.get("employee").getAsString() : "";
        String name = fiche.has("name") ? fiche.get("name").getAsString() : "";
        String employee_name = fiche.has("employee_name") ? fiche.get("employee_name").getAsString() : "";
        String posting_date = fiche.has("posting_date") ? fiche.get("posting_date").getAsString() : "";
        String start_date = fiche.has("start_date") ? fiche.get("start_date").getAsString() : "";
        String end_date = fiche.has("end_date") ? fiche.get("end_date").getAsString() : "";
        String net_pay = fiche.has("net_pay") ? fiche.get("net_pay").getAsString() : "";
        String gross_pay = fiche.has("gross_pay") ? fiche.get("gross_pay").getAsString() : "";
        String status = fiche.has("status") ? fiche.get("status").getAsString() : "Inconnue";
    %>
    <tr>
      <td><%= employer %></td>
      <td><%=name%></td>
      <td><%= employee_name %></td>
      <td><%= start_date %></td>
      <td><%= end_date %></td>
      <td><%= posting_date %></td>
      <td><%= gross_pay %> Ar</td>
      <td><%= net_pay %> Ar</td>
      <td><%= status %></td>
      <td>
        <a href="/Salary-Export?namesalaryslip=<%= name %>" class="voir-plus">Export to PDF</a>
      </td>
    </tr>
    <%
      }
    %>
    </tbody>
  </table>

  <%
  } else {
  %>
  <div class="alert">Aucune fiche de paie trouvée.</div>
  <%
    }
  } else {
  %>
  <div class="alert">Structure de données invalide.</div>
  <%
    }
  } catch (Exception e) {
    e.printStackTrace();
  %>
  <div class="alert">Erreur lors du chargement des données.</div>
  <%
    }
  } else {
  %>
  <div class="alert">Aucune donnée reçue.</div>
  <%
    }
  %>

</div>
</body>
</html>
