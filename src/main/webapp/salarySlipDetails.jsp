<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<% 
    JsonObject employee = null;
    JsonArray salarySlips = new JsonArray();
    
    // Récupérer les données de la requête
    String employeeJson = (String) request.getAttribute("employee");
    String salarySlipsJson = (String) request.getAttribute("salarySlips");
    
    // Parser les données JSON
    if (employeeJson != null && !employeeJson.isEmpty()) {
        Gson gson = new Gson();
        employee = gson.fromJson(employeeJson, JsonObject.class);
    }
    
    if (salarySlipsJson != null && !salarySlipsJson.isEmpty()) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();
        JsonElement element = parser.parse(salarySlipsJson);
        if (element.isJsonArray()) {
            salarySlips = element.getAsJsonArray();
        }
    }
    
    // Formateur de nombres pour l'affichage des montants
    NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.FRANCE);
    numberFormat.setMinimumFractionDigits(2);
    numberFormat.setMaximumFractionDigits(2);
%>

<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Détails de la fiche de paie</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.0.0/css/all.min.css">
    <style>
        .content {
            margin-right: 280px;
            padding: 20px;
            min-height: 100vh;
            background-color: #f5f7fa;
        }
        .card {
            border: none;
            border-radius: 10px;
            box-shadow: 0 0 15px rgba(0,0,0,0.05);
            margin-bottom: 20px;
        }
        .card-header {
            background-color: #fff;
            border-bottom: 1px solid #eaeaea;
            font-weight: 600;
            padding: 15px 20px;
        }
        .employee-info {
            background-color: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
            margin-bottom: 20px;
        }
        .salary-details {
            margin-top: 20px;
        }
        .table th {
            font-weight: 600;
            color: #4a5568;
            background-color: #f8f9fa;
            border-top: none;
        }
        .back-btn {
            margin-bottom: 20px;
        }
    </style>
</head>
<body>
    <jsp:include page="sidebar.jsp" />
    
    <div class="content">
        <div class="container-fluid">
            <a href="salaries" class="btn btn-secondary back-btn">
                <i class="fas fa-arrow-left"></i> Retour à la liste
            </a>
            
            <% if (employee != null) { %>
                    
                    <div class="card">
                        <div class="card-header d-flex justify-content-between align-items-center">
                            <h4 class="mb-0">Fiche de paie</h4>
                        </div>
                        <div class="card-body">
                            <div class="employee-info">
                                <h4><%= employee.has("employee_name") ? employee.get("employee_name").getAsString() : "" %></h4>
                                <p class="mb-1">
                                    <strong>Matricule:</strong> <%= employee.has("employee_number") ? employee.get("employee_number").getAsString() : "N/A" %>
                                </p>
                                <p class="mb-1">
                                    <strong>Département:</strong> <%= employee.has("department") ? employee.get("department").getAsString() : "N/A" %>
                                </p>
                                <p class="mb-0">
                                    <strong>Poste:</strong> <%= employee.has("designation") ? employee.get("designation").getAsString() : "N/A" %>
                                </p>
                            </div>
                            
                            <div class="salary-details">
                                <h5 class="mb-3">Historique des fiches de paie</h5>
                                
                                <% if (salarySlips != null && salarySlips.size() > 0) { %>
                                    <div class="table-responsive">
                                        <table class="table table-hover">
                                            <thead>
                                                <tr>
                                                    <th>Période</th>
                                                    <th class="text-end">Salaire de base</th>
                                                    <th class="text-end">Net à payer</th>
                                                    <th class="text-center">Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                <% 
                                                for (JsonElement element : salarySlips) {
                                                    JsonObject slip = element.getAsJsonObject();
                                                    String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : "";
                                                    String endDate = slip.has("end_date") ? slip.get("end_date").getAsString() : "";
                                                    double base = slip.has("base") ? slip.get("base").getAsDouble() : 0;
                                                    double netPay = slip.has("net_pay") ? slip.get("net_pay").getAsDouble() : 0;
                                                    String name = slip.has("name") ? slip.get("name").getAsString() : "";
                                                %>
                                                <tr>
                                                    <td>Du <%= startDate %> au <%= endDate %></td>
                                                    <td class="text-end"><%= numberFormat.format(base) %></td>
                                                    <td class="text-end"><strong><%= numberFormat.format(netPay) %></strong></td>
                                                    <td class="text-center">
                                                        <a href="/Salary-Export?namesalaryslip=<%= name %>" 
                                                           class="btn btn-sm btn-outline-primary" target="_blank">
                                                            <i class="fas fa-download"></i> Exporter en PDF
                                                        </a>
                                                    </td>
                                                </tr>
                                                <% } %>
                                            </tbody>
                                        </table>
                                    </div>
                                <% } else { %>
                                    <div class="alert alert-info">
                                        Aucune fiche de paie trouvée pour cet employé.
                                    </div>
                                <% } %>
                            </div>
                        </div>
                    </div>
                <% } else { %>
                    <div class="alert alert-danger">
                        Impossible de charger les détails de l'employé. Veuillez réessayer.
                    </div>
                <% } %>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
