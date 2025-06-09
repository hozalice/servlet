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
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #f5f7fa;
        }
        .content {
            margin-left: 200px;
            padding: 20px;
            min-height: 100vh;
        }
        .card {
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
            overflow: hidden;
        }
        .card-header {
            background: #f8f9fa;
            padding: 15px 20px;
            border-bottom: 1px solid #eaeaea;
        }
        .card-body {
            padding: 20px;
        }
        .btn {
            display: inline-block;
            padding: 6px 12px;
            margin: 5px;
            border: 1px solid #ddd;
            border-radius: 4px;
            background: #f8f9fa;
            color: #333;
            text-decoration: none;
            cursor: pointer;
        }
        .btn:hover {
            background: #e9ecef;
        }
        .btn-primary {
            background: #0d6efd;
            color: white;
            border-color: #0d6efd;
        }
        .btn-primary:hover {
            background: #0b5ed7;
            border-color: #0a58ca;
        }
        .btn-sm {
            padding: 3px 8px;
            font-size: 0.875em;
        }
        .btn-outline-primary {
            background: transparent;
            color: #0d6efd;
            border-color: #0d6efd;
        }
        .btn-outline-primary:hover {
            background: #0d6efd;
            color: white;
        }
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border: 1px solid transparent;
            border-radius: 4px;
        }
        .alert-info {
            color: #055160;
            background-color: #cff4fc;
            border-color: #b6effb;
        }
        .alert-danger {
            color: #842029;
            background-color: #f8d7da;
            border-color: #f5c2c7;
        }
        .table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
        }
        .table th, .table td {
            padding: 12px 15px;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }
        .table th {
            background-color: #f8f9fa;
            font-weight: bold;
        }
        .table-hover tbody tr:hover {
            background-color: #f5f5f5;
        }
        .text-end {
            text-align: right;
        }
        .text-center {
            text-align: center;
        }
        .mb-3 {
            margin-bottom: 1rem;
        }
        .form-select {
            display: block;
            width: 100%;
            padding: 0.375rem 2.25rem 0.375rem 0.75rem;
            font-size: 1rem;
            font-weight: 400;
            line-height: 1.5;
            color: #212529;
            background-color: #fff;
            background-image: url("data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23343a40' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='M2 5l6 6 6-6'/%3e%3c/svg%3e");
            background-repeat: no-repeat;
            background-position: right 0.75rem center;
            background-size: 16px 12px;
            border: 1px solid #ced4da;
            border-radius: 0.25rem;
            -webkit-appearance: none;
            -moz-appearance: none;
            appearance: none;
        }
        .form-label {
            display: inline-block;
            margin-bottom: 0.5rem;
            font-weight: 500;
        }
        .back-btn {
            display: inline-block;
            margin-bottom: 20px;
            text-decoration: none;
            color: #333;
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
                                    <div style="margin-bottom: 20px;">
                                    <div style="display: flex; flex-wrap: wrap; margin: 0 -10px;">
                                        <div style="flex: 0 0 25%; padding: 0 10px;">
                                            <label for="monthFilter" style="display: block; margin-bottom: 5px; font-weight: 500;">Filtrer par mois :</label>
                                            <select id="monthFilter" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                                                <option value="all">Tous les mois</option>
                                                <option value="01">Janvier</option>
                                                <option value="02">Février</option>
                                                <option value="03">Mars</option>
                                                <option value="04">Avril</option>
                                                <option value="05">Mai</option>
                                                <option value="06">Juin</option>
                                                <option value="07">Juillet</option>
                                                <option value="08">Août</option>
                                                <option value="09">Septembre</option>
                                                <option value="10">Octobre</option>
                                                <option value="11">Novembre</option>
                                                <option value="12">Décembre</option>
                                            </select>
                                        </div>
                                    </div>
                                </div>
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

    <script>
        document.addEventListener('DOMContentLoaded', function() {
            const monthFilter = document.getElementById('monthFilter');
            const rows = document.querySelectorAll('tbody tr');
            
            monthFilter.addEventListener('change', function() {
                const selectedMonth = this.value;
                
                rows.forEach(row => {
                    const dateCell = row.cells[0]; // La cellule qui contient la date
                    const dateText = dateCell.textContent.trim();
                    
                    // Extraire le mois de la date (format attendu: "Du YYYY-MM-DD au YYYY-MM-DD")
                    const dateMatch = dateText.match(/\d{4}-(\d{2})-\d{2}/);
                    
                    if (selectedMonth === 'all' || !dateMatch || dateMatch[1] === selectedMonth) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                });
                
                // Mettre à jour l'URL avec le filtre
                const url = new URL(window.location);
                if (selectedMonth === 'all') {
                    url.searchParams.delete('month');
                } else {
                    url.searchParams.set('month', selectedMonth);
                }
                window.history.pushState({}, '', url);
            });
            
            // Appliquer le filtre au chargement de la page si présent dans l'URL
            const urlParams = new URLSearchParams(window.location.search);
            const monthParam = urlParams.get('month');
            if (monthParam) {
                monthFilter.value = monthParam;
                monthFilter.dispatchEvent(new Event('change'));
            }
        });
    </script>
</body>
</html>
