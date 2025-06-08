<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.net.URLEncoder" %>
<%
    String employeesJson = (String) request.getAttribute("employees");
    String groupedSalariesJson = (String) request.getAttribute("groupedSalaries");
    String allSalarySlipsJson = (String) request.getAttribute("allSalarySlips");
    String error = (String) request.getAttribute("error");
    
    JsonParser parser = new JsonParser();
    JsonArray employees = new JsonArray();
    JsonObject groupedSalaries = new JsonObject();
    JsonArray allSalarySlips = new JsonArray();
    
    try {
        if (employeesJson != null && !employeesJson.isEmpty()) {
            employees = parser.parse(employeesJson).getAsJsonArray();
        }
        if (groupedSalariesJson != null && !groupedSalariesJson.isEmpty()) {
            groupedSalaries = parser.parse(groupedSalariesJson).getAsJsonObject();
        }
        if (allSalarySlipsJson != null && !allSalarySlipsJson.isEmpty()) {
            allSalarySlips = parser.parse(allSalarySlipsJson).getAsJsonArray();
        }
    } catch (Exception e) {
        e.printStackTrace();
        error = "Erreur lors du traitement des données: " + e.getMessage();
    }
    
    NumberFormat formatter = NumberFormat.getNumberInstance(Locale.FRANCE);
    formatter.setMinimumFractionDigits(2);
    formatter.setMaximumFractionDigits(2);
%>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Gestion des Salaires</title>
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
        .table th {
            font-weight: 600;
            color: #4a5568;
            background-color: #f8f9fa;
            border-top: none;
        }
        .table td {
            vertical-align: middle;
        }
        .form-select {
            max-width: 200px;
        }
        .total-row {
            font-weight: 600;
            background-color: #f8f9fa;
        }
        .month-filter {
            display: flex;
            gap: 10px;
            align-items: center;
            margin-bottom: 20px;
        }
        .salary-card {
            background-color: #fff;
            border: 1px solid #ddd;
            border-radius: 10px;
            padding: 20px;
        }
    </style>
</head>
<body>
    <div class="container-fluid">
        <div class="row">
            <%@ include file="sidebar.jsp" %>
            
            <main class="col-md-9 ms-sm-auto col-lg-10 px-md-4">
                <div class="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
                    <h1 class="h2">Fiches de paie par employé</h1>
                </div>

                <% if (error != null) { %>
                    <div class="alert alert-danger" role="alert">
                        <%= error %>
                    </div>
                <% } %>

                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">Liste des employés</h5>
                    </div>
                    <div class="card-body">
                        <% if (employees.size() == 0) { %>
                            <div class="no-data">
                                Aucun employé trouvé.
                            </div>
                        <% } else { %>
                            <div class="accordion" id="employeesAccordion">
                                <% 
                                for (JsonElement empElement : employees) {
                                    JsonObject employee = empElement.getAsJsonObject();
                                    String empId = employee.get("name").getAsString();
                                    String empName = employee.has("employee_name") ? employee.get("employee_name").getAsString() : "";
                                    String empNumber = employee.has("employee_number") ? employee.get("employee_number").getAsString() : "";
                                    
                                    JsonArray empSalaries = groupedSalaries.has(empId) ? groupedSalaries.getAsJsonArray(empId) : new JsonArray();
                                %>
                                    <div class="accordion-item">
                                        <h2 class="accordion-header" id="heading<%= empId %>">
                                            <button class="accordion-button collapsed" type="button" data-bs-toggle="collapse" 
                                                data-bs-target="#collapse<%= empId %>" aria-expanded="false" aria-controls="collapse<%= empId %>">
                                                <div class="d-flex justify-content-between w-100">
                                                    <div>
                                                        <strong><%= empName %></strong>
                                                        <% if (!empNumber.isEmpty()) { %>
                                                            <span class="text-muted ms-2">#<%= empNumber %></span>
                                                        <% } %>
                                                    </div>
                                                    <span class="badge bg-primary rounded-pill">
                                                        <%= empSalaries.size() %> fiche(s) de paie
                                                    </span>
                                                </div>
                                            </button>
                                        </h2>
                                        <div id="collapse<%= empId %>" class="accordion-collapse collapse" 
                                            aria-labelledby="heading<%= empId %>" data-bs-parent="#employeesAccordion">
                                            <div class="accordion-body p-3">
                                                <% if (empSalaries.size() == 0) { %>
                                                    <div class="alert alert-info mb-0">
                                                        Aucune fiche de paie disponible pour cet employé.
                                                    </div>
                                                <% } else { %>
                                                    <% for (JsonElement slipElement : empSalaries) { 
                                                        JsonObject slip = slipElement.getAsJsonObject();
                                                        String slipName = slip.has("name") ? slip.get("name").getAsString() : "";
                                                        String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : "";
                                                        String endDate = slip.has("end_date") ? slip.get("end_date").getAsString() : "";
                                                        double grossPay = slip.has("gross_pay") ? slip.get("gross_pay").getAsDouble() : 0.0;
                                                        double netPay = slip.has("net_pay") ? slip.get("net_pay").getAsDouble() : 0.0;
                                                        String status = slip.has("status") ? slip.get("status").getAsString() : "Inconnu";
                                                    %>
                                                        <div class="salary-card mb-3">
                                                            <div class="d-flex justify-content-between align-items-center mb-2">
                                                                <h6 class="mb-0">Période : <%= startDate %> au <%= endDate %></h6>
                                                                <span class="badge <%= "Draft".equals(status) ? "bg-warning" : "bg-success" %>">
                                                                    <%= status %>
                                                                </span>
                                                            </div>
                                                            
                                                            <!-- Ligne des totaux -->
                                                            <div class="row mb-3">
                                                                <div class="col-md-6">
                                                                    <div class="mb-1">
                                                                        <small class="text-muted">Salaire brut :</small>
                                                                        <div><%= formatter.format(grossPay) %> Ar</div>
                                                                    </div>
                                                                </div>
                                                                <div class="col-md-6">
                                                                    <div class="mb-1">
                                                                        <small class="text-muted">Salaire net :</small>
                                                                        <div class="fw-bold"><%= formatter.format(netPay) %> Ar</div>
                                                                    </div>
                                                                </div>
                                                            </div>
                                                            
                                                            <!-- Détails des gains -->
                                                            <% if (slip.has("earnings")) { 
                                                                JsonElement earningsElem = slip.get("earnings");
                                                                JsonArray earningsArray = new JsonArray();
                                                                
                                                                if (earningsElem.isJsonArray()) {
                                                                    earningsArray = earningsElem.getAsJsonArray();
                                                                } else if (earningsElem.isJsonObject() && earningsElem.getAsJsonObject().has("data")) {
                                                                    JsonElement data = earningsElem.getAsJsonObject().get("data");
                                                                    if (data.isJsonArray()) {
                                                                        earningsArray = data.getAsJsonArray();
                                                                    }
                                                                }
                                                                
                                                                if (earningsArray.size() > 0) { %>
                                                                    <div class="mb-3">
                                                                        <h6 class="text-primary mb-2">Gains</h6>
                                                                        <div class="table-responsive">
                                                                            <table class="table table-sm table-borderless mb-0">
                                                                                <tbody>
                                                                                    <% for (JsonElement earning : earningsArray) { 
                                                                                        JsonObject e = earning.getAsJsonObject();
                                                                                        String label = "";
                                                                                        double amount = 0.0;
                                                                                        
                                                                                        if (e.has("salary_component")) {
                                                                                            label = e.get("salary_component").getAsString();
                                                                                        } else if (e.has("salary_component_name")) {
                                                                                            label = e.get("salary_component_name").getAsString();
                                                                                        }
                                                                                        
                                                                                        if (e.has("amount")) {
                                                                                            amount = e.get("amount").getAsDouble();
                                                                                        }
                                                                                    %>
                                                                                        <tr>
                                                                                            <td class="ps-0"><%= label %></td>
                                                                                            <td class="text-end pe-0"><%= formatter.format(amount) %> Ar</td>
                                                                                        </tr>
                                                                                    <% } %>
                                                                                </tbody>
                                                                            </table>
                                                                        </div>
                                                                    </div>
                                                                <% }
                                                            } %>
                                                            
                                                            <!-- Détails des déductions -->
                                                            <% if (slip.has("deductions")) { 
                                                                JsonElement deductionsElem = slip.get("deductions");
                                                                JsonArray deductionsArray = new JsonArray();
                                                                
                                                                if (deductionsElem.isJsonArray()) {
                                                                    deductionsArray = deductionsElem.getAsJsonArray();
                                                                } else if (deductionsElem.isJsonObject() && deductionsElem.getAsJsonObject().has("data")) {
                                                                    JsonElement data = deductionsElem.getAsJsonObject().get("data");
                                                                    if (data.isJsonArray()) {
                                                                        deductionsArray = data.getAsJsonArray();
                                                                    }
                                                                }
                                                                
                                                                if (deductionsArray.size() > 0) { %>
                                                                    <div class="mb-3">
                                                                        <h6 class="text-danger mb-2">Déductions</h6>
                                                                        <div class="table-responsive">
                                                                            <table class="table table-sm table-borderless mb-0">
                                                                                <tbody>
                                                                                    <% for (JsonElement deduction : deductionsArray) { 
                                                                                        JsonObject d = deduction.getAsJsonObject();
                                                                                        String label = "";
                                                                                        double amount = 0.0;
                                                                                        
                                                                                        if (d.has("salary_component")) {
                                                                                            label = d.get("salary_component").getAsString();
                                                                                        } else if (d.has("salary_component_name")) {
                                                                                            label = d.get("salary_component_name").getAsString();
                                                                                        }
                                                                                        
                                                                                        if (d.has("amount")) {
                                                                                            amount = d.get("amount").getAsDouble();
                                                                                        }
                                                                                    %>
                                                                                        <tr>
                                                                                            <td class="ps-0"><%= label %></td>
                                                                                            <td class="text-end pe-0">-<%= formatter.format(amount) %> Ar</td>
                                                                                        </tr>
                                                                                    <% } %>
                                                                                </tbody>
                                                                            </table>
                                                                        </div>
                                                                    </div>
                                                                <% }
                                                            } %>
                                                            
                                                            <div class="text-end mt-3">
                                                                <a href="/Salary-Export?namesalaryslip=<%= URLEncoder.encode(slipName, "UTF-8") %>" class="btn btn-sm btn-primary">
                                                                    <i class="bi bi-download"></i> Exporter en PDF
                                                                </a>
                                                            </div>
                                                        </div>
                                                    <% } %>
                                                <% } %>
                                            </div>
                                        </div>
                                    </div>
                                <% } %>
                            </div>
                        <% } %>
                    </div>
                </div>
            </main>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
