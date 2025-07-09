<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%
    String employeeJson = (String) request.getAttribute("employee");
    String salarySlipsJson = (String) request.getAttribute("salarySlips");
    
    JsonParser parser = new JsonParser();
    JsonObject employee = new JsonObject();
    JsonArray salarySlips = new JsonArray();
    
    try {
        if (employeeJson != null && !employeeJson.isEmpty()) {
            employee = parser.parse(employeeJson).getAsJsonObject();
        }
        
        if (salarySlipsJson != null && !salarySlipsJson.isEmpty()) {
            JsonElement element = parser.parse(salarySlipsJson);
            if (element.isJsonArray()) {
                salarySlips = element.getAsJsonArray();
            }
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    
    NumberFormat formatter = NumberFormat.getNumberInstance(Locale.FRANCE);
    formatter.setMinimumFractionDigits(2);
    formatter.setMaximumFractionDigits(2);
%>
<!DOCTYPE html>
<html>
<head>
    <title>Fiche Employé</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background-color: #f8f9fa;
            font-family: Arial, sans-serif;
            padding: 20px;
        }
        .container {
            background: white;
            padding: 25px;
            border-radius: 8px;
            box-shadow: 0 0 10px rgba(0,0,0,0.1);
            margin-top: 20px;
        }
        .employee-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .salary-card {
            border: 1px solid #dee2e6;
            border-radius: 8px;
            padding: 15px;
            margin-bottom: 15px;
        }
        .salary-header {
            display: flex;
            justify-content: space-between;
            margin-bottom: 10px;
            padding-bottom: 10px;
            border-bottom: 1px solid #dee2e6;
        }
        .salary-details {
            margin-top: 15px;
        }
        .no-data {
            text-align: center;
            color: #6c757d;
            padding: 20px;
        }
        /* Style pour le formulaire de génération de fiche de paie */
        form[action="/Generate"] {
            background: #f1f3f6;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 30px;
            box-shadow: 0 2px 8px rgba(0,0,0,0.04);
        }
        form[action="/Generate"] h2 {
            font-size: 1.3rem;
            margin-bottom: 18px;
            color: #0d6efd;
        }
        form[action="/Generate"] .form-row {
            display: flex;
            flex-wrap: wrap;
            gap: 20px;
            margin-bottom: 15px;
        }
        form[action="/Generate"] .form-group {
            flex: 1 1 200px;
            display: flex;
            flex-direction: column;
        }
        form[action="/Generate"] label {
            font-weight: 500;
            margin-bottom: 6px;
            color: #495057;
        }
        form[action="/Generate"] input[type="text"],
        form[action="/Generate"] input[type="number"],
        form[action="/Generate"] input[type="date"] {
            border: 1px solid #ced4da;
            border-radius: 4px;
            padding: 7px 10px;
            margin-bottom: 8px;
            background: #fff;
        }
        form[action="/Generate"] input[readonly] {
            background: #e9ecef;
        }
        form[action="/Generate"] input[type="checkbox"] {
            margin-right: 6px;
        }
        form[action="/Generate"] button[type="submit"] {
            background: #0d6efd;
            color: #fff;
            border: none;
            border-radius: 4px;
            padding: 8px 22px;
            font-weight: 500;
            transition: background 0.2s;
            margin-top: 10px;
        }
        form[action="/Generate"] button[type="submit"]:hover {
            background: #0b5ed7;
        }
    </style>
  </style>
</head>
<body>

<%@ include file="sidebar.jsp" %>

<div class="container">
    <div class="card mb-4">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">Informations de l'employé</h4>
        </div>
        <div class="card-body">
            <div class="row">
                <div class="col-md-6">
                    <p><strong>Nom :</strong> <%= employee.has("employee_name") ? employee.get("employee_name").getAsString() : "-" %></p>
                    <p><strong>Poste :</strong> <%= employee.has("designation") ? employee.get("designation").getAsString() : "-" %></p>
                </div>
                <div class="col-md-6">
                    <p><strong>Département :</strong> <%= employee.has("department") ? employee.get("department").getAsString() : "-" %></p>
                    <p><strong>Date d'embauche :</strong> <%= employee.has("date_of_joining") ? employee.get("date_of_joining").getAsString() : "-" %></p>
                </div>
            </div>
        </div>
    </div>
    <form action="/Generate" method="GET">
        <h2>Generer SSA et une Salary slip</h2>
        <div class="form-row">
            <div class="form-group">
                <label for="employee">Employé:</label>
                <input id="employee" name="employee" value="<%= employee.has("employee") ? employee.get("employee").getAsString() : "-" %>" readonly>

                <label for="employeename">Nom de l'employé:</label>
                <input id="employeename" name="anarana" value="<%= employee.has("employee_name") ? employee.get("employee_name").getAsString() : "-" %>">
            </div>
        </div>


    <div class="form-row">
        <div class="form-group">
            <label for="start_date">Date de début:</label>
            <input type="date" id="start_date" name="start_date" required>
        </div>
        <div class="form-group">
            <label for="end_date">Date de fin:</label>
            <input type="date" id="end_date" name="end_date" required>
        </div>
    </div>

    <div class="form-row">
        <div class="form-group">

            <label for="amount">Valeru du salaire</label>
            <input type="number" id="amount" name="salaire" >
        </div>
    </div>
        <div class="form-row">
            <div class="form-group">
                <input type="checkbox" id="ecraser" name="ecraser" value="1" >ecraser
                <br>
                <input type="checkbox" id="moyenne" name="moyenne" value="1" >moyenne de salaire
            </div>
        </div>
    <button type="submit">valider et generer</button>
    </form>
    <div class="card">
        <div class="card-header bg-primary text-white">
            <h4 class="mb-0">Fiches de paie</h4>
        </div>
        <div class="card-body">
            <% if (salarySlips.size() > 0) {
                for (JsonElement slipElement : salarySlips) {
                    JsonObject slip = slipElement.getAsJsonObject();
                    String slipName = slip.has("name") ? slip.get("name").getAsString() : "";
                    String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : "";
                    String endDate = slip.has("end_date") ? slip.get("end_date").getAsString() : "";
                    double netPay = slip.has("net_pay") ? slip.get("net_pay").getAsDouble() : 0.0;
                    double grossPay = slip.has("gross_pay") ? slip.get("gross_pay").getAsDouble() : 0.0;
                    String status = slip.has("status") ? slip.get("status").getAsString() : "Inconnu";
            %>
                <div class="card mb-3">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">Période : <%= startDate %> au <%= endDate %></h5>
                        <span class="badge <%= "Draft".equals(status) ? "bg-warning" : "bg-success" %>">
                            <%= status %>
                        </span>
                    </div>
                    <div class="card-body">
                        <div class="row">
                            <div class="col-md-6">
                                <p><strong>Salaire brut :</strong> <%= formatter.format(grossPay) %> Ar</p>
                            </div>
                            <div class="col-md-6">
                                <p><strong>Salaire net :</strong> <span class="text-success fw-bold"><%= formatter.format(netPay) %> Ar</span></p>
                            </div>

                        </div>
                        <div class="text-end mt-2">
                            <a href="/Salary-Export?namesalaryslip=<%= slipName %>" class="btn btn-sm btn-primary">
                                <i class="bi bi-download"></i> Exporter en PDF
                            </a>
                        </div>
                    </div>
                </div>
            <% }
            } else { %>
                <div class="alert alert-info mb-0">
                    Aucune fiche de paie disponible pour cet employé.
                </div>
            <% } %>
        </div>
    </div>
    
    <div class="mt-4 text-center">
        <a href="salaries.jsp" class="btn btn-secondary">
            <i class="bi bi-arrow-left"></i> Retour à la liste
        </a>
    </div>
</div>

<!-- Bootstrap Icons -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.8.1/font/bootstrap-icons.css">
<!-- Bootstrap JS Bundle with Popper -->
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
