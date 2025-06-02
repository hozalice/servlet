<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.*" %>
<!DOCTYPE html>
<html>
<head>
    <title>Liste des Employés</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
<%@include file="sidebar.jsp"%>
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
            <th>ID</th>
            <th>Nom</th>
            <th>Département</th>
            <th>Poste</th>
            <th>Statut</th>
        </tr>
        </thead>
        <tbody>
        <%
            String employeesData = (String) request.getAttribute("employeesData");
            if (employeesData != null) {
                JsonParser parser = new JsonParser();
                JsonObject jsonResponse = parser.parse(employeesData).getAsJsonObject();
                JsonArray employees = jsonResponse.get("data").getAsJsonArray();

                for (JsonElement element : employees) {
                    JsonObject employee = element.getAsJsonObject();

                    String id = employee.has("name") && !employee.get("name").isJsonNull()
                            ? employee.get("name").getAsString()
                            : "";

                    String name = employee.has("employee_name") && !employee.get("employee_name").isJsonNull()
                            ? employee.get("employee_name").getAsString()
                            : "";

                    String department = employee.has("department") && !employee.get("department").isJsonNull()
                            ? employee.get("department").getAsString()
                            : "";

                    String designation = employee.has("designation") && !employee.get("designation").isJsonNull()
                            ? employee.get("designation").getAsString()
                            : "";

                    String status = employee.has("status") && !employee.get("status").isJsonNull()
                            ? employee.get("status").getAsString()
                            : "Inactive";
        %>
        <tr>
            <td><%= id %></td>
            <td><%= name %></td>
            <td><%= department %></td>
            <td><%= designation %></td>
            <td><%= status %></td>
        </tr>
        <%
                }
            }
        %>
        </tbody>
    </table>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
