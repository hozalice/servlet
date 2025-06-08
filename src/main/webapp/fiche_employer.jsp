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
