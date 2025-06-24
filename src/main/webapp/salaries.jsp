`salary-management.jsp`
```jsp
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Calendar" %>
<%@ page import="java.util.Map" %>

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
<html lang="fr">
<head>
    <meta charset="UTF-8" />
    <title>Gestion des Salaires</title>
    <style>
        /* Global Styles */
        body {
            font-family: 'Inter', Arial, sans-serif;
            margin: 0;
            padding: 0;
            background-color: #ffffff;
            color: #6b7280;
            line-height: 1.6;
            -webkit-font-smoothing: antialiased;
            -moz-osx-font-smoothing: grayscale;
        }

        /* Container and Layout */
        .content {
            width: 65%;
            padding: 4rem 2rem 5rem;
            box-sizing: border-box;
        }

        /* Card Component Base */
        .card {
            background: #ffffff;
            border-radius: 0.75rem;
            box-shadow: 0 4px 6px rgba(0,0,0,0.05);
            margin-bottom: 2.5rem;
            overflow: hidden;
            border: 1px solid #e5e7eb;
            transition: box-shadow 0.3s ease;
        }
        .card:hover {
            box-shadow: 0 8px 15px rgba(0,0,0,0.1);
        }

        /* Card Header */
        .card-header {
            background-color: #f9fafb;
            border-bottom: 1px solid #e5e7eb;
            padding: 1.25rem 2rem;
        }
        .card-header h2, .card-header h4, .card-header h5 {
            margin: 0;
            font-weight: 700;
            color: #111827;
            letter-spacing: -0.02em;
        }
        .card-header h2 {
            font-size: 2.5rem;
            line-height: 1.2;
        }
        .card-header h4 {
            font-size: 1.5rem;
        }
        .card-header h5 {
            font-size: 1.25rem;
            color: #374151;
        }

        /* Card Body */
        .card-body {
            padding: 2rem;
        }

        /* Form Filters Style */
        form {
            display: flex;
            flex-wrap: wrap;
            gap: 1rem 2rem;
            align-items: center;
            margin-bottom: 2rem;
            color: #374151;
        }
        form label {
            font-weight: 600;
            flex-shrink: 0;
        }
        form select {
            height: 38px;
            padding: 0 0.75rem;
            border-radius: 0.375rem;
            border: 1px solid #d1d5db;
            background-color: #fff;
            color: #374151;
            font-size: 1rem;
            transition: border-color 0.2s ease-in-out;
            cursor: pointer;
            min-width: 160px;
        }
        form select:focus {
            outline: none;
            border-color: #055160;
            box-shadow: 0 0 0 3px rgba(59,130,246,0.3);
        }
        form input[type="submit"] {
            background-color:#055160;
            color: #fff;
            border: none;
            border-radius: 0.375rem;
            padding: 0.5rem 1.5rem;
            font-size: 1rem;
            font-weight: 600;
            cursor: pointer;
            transition: background-color 0.2s ease;
            white-space: nowrap;
        }
        form input[type="submit"]:hover {
            background-color: #055160;
        }

        /* Alert Style */
        .alert {
            padding: 1rem 1.5rem;
            border-radius: 0.5rem;
            margin-bottom: 1.5rem;
            font-weight: 600;
        }
        .alert-danger {
            background-color: #fee2e2;
            color: #b91c1c;
            border: 1px solid #fca5a5;
        }
        .alert-info {
            background-color: #dbeafe;
            color: #055160;
            border: 1px solid #93c5fd;
        }

        /* Table Styles */
        .table-responsive {
            overflow-x: auto;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            font-size: 1rem;
            color: #374151;
            margin-top: 1rem;
            box-shadow: 0 0 0 1px #e5e7eb;
            border-radius: 0.5rem;
            overflow: hidden;
        }
        thead tr {
            background-color: #055160;
        }
        thead th {
            color: white;
            font-weight: 700;
            text-align: left;
            padding: 1rem 1.25rem;
            font-size: 1rem;
            user-select: none;
        }
        tbody tr {
            background: #fff;
            transition: background-color 0.15s ease;
            cursor: default;
        }
        tbody tr:hover {
            background-color: #e0e7ff;
        }
        tbody td {
            padding: 1rem 1.25rem;
            border-top: 1px solid #e5e7eb;
            vertical-align: middle;
        }
        tbody td.text-end {
            text-align: right;
            font-variant-numeric: tabular-nums;
        }

        /* Badges */
        .badge {
            display: inline-block;
            padding: 0.3em 0.7em;
            font-size: 0.8rem;
            font-weight: 600;
            color: #fff;
            border-radius: 0.5rem;
            user-select: none;
            text-transform: capitalize;
        }
        .bg-success {
            background-color: #22c55e; /* green-500 */
        }
        .bg-warning {
            background-color: #eab308; /* yellow-500 */
            color: #1e1e1e;
        }

        /* Button styles */
        .btn {
            display: inline-flex;
            align-items: center;
            padding: 0.375rem 0.75rem;
            font-size: 0.875rem;
            font-weight: 600;
            color: #055160;
            background-color: transparent;
            border: 2px solid #055160;
            border-radius: 0.5rem;
            cursor: pointer;
            text-decoration: none;
            transition: background-color 0.2s ease, color 0.2s ease;
        }
        .btn i {
            margin-right: 0.4rem;
        }
        .btn.btn-primary {
            background-color: #4a5568;
            color: white;
            border-color: #4a5568;
        }
        .btn.btn-primary:hover,
        .btn.btn-primary:focus {
            background-color:#4a5568;
            border-color: #4a5568;
            color: white;
            outline: none;
        }
        .btn:focus {
            outline: 2px solid #4a5568;
            outline-offset: 2px;
        }

        /* Salary cards improvements */
        .salary-card {
            border: 1px solid #4a5568;
            border-radius: 0.75rem;
            padding: 1.25rem 1.5rem;
            background-color: #f9fafb;
            box-shadow: 0 2px 6px rgba(59,130,246,0.15);
            transition: box-shadow 0.3s ease;
        }
        .salary-card:hover {
            box-shadow: 0 6px 12px rgba(59,130,246,0.25);
        }
        .salary-card h6 {
            color:#4a5568;
            font-weight: 700;
            font-size: 1.125rem;
            margin-bottom: 0.75rem;
        }
        .salary-card .badge {
            font-size: 0.85rem;
        }

        /* Summary Table specific */
        #totalsContainer .table {
            border-radius: 0.75rem;
            overflow: hidden;
            box-shadow: 0 3px 8px rgba(0,0,0,0.08);
        }
        #totalsContainer .table th {
            background-color: #4a5568;
            color: #fafafa;
            font-weight: 600;
            padding: 1rem 1.5rem;
            text-align: left;
            font-size: 1rem;
        }
        #totalsContainer .table td {
            padding: 1rem 1.5rem;
            font-weight: 500;
            color: #374151;
        }
        #totalsContainer .table tr.table-info td {
            background-color: #dbeafe;
            color: #4a5568;
            font-weight: 600;
        }
        #totalsContainer .table tr.table-warning td {
            background-color: #fef3c7;
            color: #92400e;
            font-weight: 600;
        }
        #totalsContainer .table tr.table-secondary td {
            background-color: #f3f4f6;
            color: #374151;
            font-weight: 700;
        }
        #totalsContainer .table tr.table-primary td {
            background-color: #bfdbfe;
            color: #4a5568;
            font-weight: 700;
        }

        /* Accordion button improvements */
        .accordion-button {
            font-weight: 600;
            font-size: 1rem;
            color: #4a5568;
            background: transparent;
            border: none;
            display: flex;
            justify-content: space-between;
            align-items: center;
            cursor: pointer;
            padding: 1rem 2rem;
            width: 100%;
            transition: color 0.3s ease;
            user-select: none;
        }
        .accordion-button:hover,
        .accordion-button:focus {
            color: #4a5568;
            outline: none;
        }
        .accordion-button:after {
            content: '▼';
            font-size: 0.85rem;
            transition: transform 0.3s ease;
            color: #4a5568;
            margin-left: 1rem;
        }
        .accordion-button[aria-expanded="true"]:after {
            transform: rotate(180deg);
        }
        .accordion-collapse {
            display: none;
            padding: 0 2rem 2rem;
            border-top: 1px solid #e5e7eb;
            animation: fadeInAccordion 0.4s ease forwards;
        }
        .accordion-collapse.show {
            display: block;
        }
        @keyframes fadeInAccordion {
            from {opacity: 0; transform: translateY(-10px);}
            to {opacity: 1; transform: translateY(0);}
        }

        /* Responsive adjustments */
        @media (max-width: 768px) {
            .content {
                padding: 3rem 1rem 3rem;
                width: 100%;
            }
            form {
                flex-direction: column;
                align-items: flex-start;
            }
            form select, form input[type="submit"] {
                width: 100%;
                max-width: none;
            }
            .card-header h2 {
                font-size: 2rem;
            }
            table td, table th {
                padding: 0.75rem 1rem;
            }
            .btn {
                font-size: 0.8rem;
                padding: 0.4rem 0.75rem;
            }
        }
    </style>
</head>
<body>
<jsp:include page="sidebar.jsp" />

<div class="content">
    <div class="card">
        <div class="card-header">
            <h2>Fiches de paie par employé</h2>
            <form method="get" action="salaries">
                <label for="month">Mois:</label>
                <select name="month" id="month">
                    <option value="">-- Tous les mois --</option>
                    <%
                        String[] mois = { "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre" };
                        for (int i = 1; i <= 12; i++) {
                    %>
                    <option value="<%= i %>" <%= request.getParameter("month") != null && request.getParameter("month").equals(String.valueOf(i)) ? "selected" : "" %>>
                        <%= mois[i - 1] %>
                    </option>
                    <% } %>
                </select>

                <label for="year">Année:</label>
                <select name="year" id="year">
                    <option value="">-- Toutes les années --</option>
                    <%
                        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                        for (int y = currentYear; y >= 2000; y--) {
                    %>
                    <option value="<%= y %>" <%= request.getParameter("year") != null && request.getParameter("year").equals(String.valueOf(y)) ? "selected" : "" %>>
                        <%= y %>
                    </option>
                    <% } %>
                </select>

                <input type="submit" value="Filtrer" />
            </form>
            <%
                String totalsJsonStr = (String) request.getAttribute("totalsJson");
                JsonObject totalsJson = null;
                JsonObject earnings = null;
                JsonObject deductions = null;

                if (totalsJsonStr != null && !totalsJsonStr.isEmpty()) {
                    totalsJson = JsonParser.parseString(totalsJsonStr).getAsJsonObject();
                    earnings = totalsJson.getAsJsonObject("earningsTotals");
                    deductions = totalsJson.getAsJsonObject("deductionsTotals");
                }

                double totalEarnings = 0;
                double totalDeductions = 0;
            %>

            <% if (totalsJson != null) { %>
            <div id="totalsContainer" class="card mb-4">
                <div class="card-header bg-light">
                    <h4 class="mb-0">Récapitulatif des gains et retenues</h4>
                </div>
                <div class="card-body p-0">
                    <div class="table-responsive">
                        <table class="table table-bordered table-hover mb-0">
                            <thead>
                            <tr>
                                <th>Type</th>
                                <th>Libellé</th>
                                <th class="text-end">Montant (Ar)</th>
                            </tr>
                            </thead>
                            <tbody>
                            <!-- Earnings -->
                            <tr class="table-info">
                                <td colspan="3">Gains</td>
                            </tr>
                            <% if (earnings != null) {
                                for (Map.Entry<String, JsonElement> entry : earnings.entrySet()) {
                                    String key = entry.getKey();
                                    double value = entry.getValue().getAsDouble();
                                    totalEarnings += value;
                            %>
                            <tr>
                                <td>Gain</td>
                                <td><%= key %></td>
                                <td class="text-end"><%= String.format("%,.2f", value) %></td>
                            </tr>
                            <% } } %>

                            <!-- Deductions -->
                            <tr class="table-warning">
                                <td colspan="3">Retenues</td>
                            </tr>
                            <% if (deductions != null) {
                                for (Map.Entry<String, JsonElement> entry : deductions.entrySet()) {
                                    String key = entry.getKey();
                                    double value = entry.getValue().getAsDouble();
                                    totalDeductions += value;
                            %>
                            <tr>
                                <td>Retenue</td>
                                <td><%= key %></td>
                                <td class="text-end"><%= String.format("%,.2f", value) %></td>
                            </tr>
                            <% } } %>

                            <!-- Totals -->
                            <tr class="table-secondary">
                                <td colspan="2" class="text-end fw-bold">Total Gains</td>
                                <td class="text-end fw-bold"><%= String.format("%,.2f", totalEarnings) %></td>
                            </tr>
                            <tr class="table-secondary">
                                <td colspan="2" class="text-end fw-bold">Total Retenues</td>
                                <td class="text-end fw-bold"><%= String.format("%,.2f", totalDeductions) %></td>
                            </tr>
                            <tr class="table-primary">
                                <td colspan="2" class="text-end fw-bold">Net à payer</td>
                                <td class="text-end fw-bold"><%= String.format("%,.2f", (totalEarnings - totalDeductions)) %></td>
                            </tr>
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
            <% } %>

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
                    <div class="alert alert-info">
                        Aucun employé trouvé.
                    </div>
                    <% } else { %>
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead>
                            <tr>
                                <th>Nom complet</th>
                                <th>Matricule</th>
                                <th>Nombre de fiches de paie</th>
                                <th class="text-center">Détails</th>
                            </tr>
                            </thead>
                            <tbody>
                            <%
                                for (JsonElement empElement : employees) {
                                    JsonObject employee = empElement.getAsJsonObject();
                                    String empId = employee.get("name").getAsString();
                                    String empName = employee.has("employee_name") ? employee.get("employee_name").getAsString() : "";
                                    String empNumber = employee.has("employee_number") ? employee.get("employee_number").getAsString() : "";

                                    JsonArray empSalaries = groupedSalaries.has(empId) ? groupedSalaries.getAsJsonArray(empId) : new JsonArray();
                            %>
                            <tr>
                                <td><%= empName %></td>
                                <td><%= empNumber %></td>
                                <td class="text-center"><%= empSalaries.size() %> fiche(s)</td>
                                <td class="text-center">
                                    <button class="btn btn-primary" type="button" data-bs-toggle="collapse"
                                            data-bs-target="#collapse<%= empId %>" aria-expanded="false"
                                            aria-controls="collapse<%= empId %>">
                                        <i class="fas fa-eye"></i> Voir les détails
                                    </button>
                                </td>
                            </tr>
                            <tr>
                                <td colspan="4" style="padding:0; border:none;">
                                    <div id="collapse<%= empId %>" class="accordion-collapse">
                                        <div class="card card-body mt-2 salary-card">
                                            <div class="accordion-body p-3">
                                                <% if (empSalaries.size() == 0) { %>
                                                <div class="alert alert-info mb-0">
                                                    Aucune fiche de paie disponible pour cet employé.
                                                </div>
                                                <% } else {
                                                    for (JsonElement slipElement : empSalaries) {
                                                        JsonObject slip = slipElement.getAsJsonObject();
                                                        String slipName = slip.has("name") ? slip.get("name").getAsString() : "";
                                                        String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : "";
                                                        String endDate = slip.has("end_date") ? slip.get("end_date").getAsString() : "";

                                                        // Récupération sécurisée des valeurs avec des valeurs par défaut
                                                        double baseSalary = 0.0;
                                                        double overtimePay = 0.0;
                                                        double bonuss = 0.0;
                                                        double benefitss = 0.0;
                                                        double slipTotalDeductions = 0.0;
                                                        double netSalary = 0.0;

                                                        if (slip.has("base_salary") && !slip.get("base_salary").isJsonNull()) {
                                                            baseSalary = slip.get("base_salary").getAsDouble();
                                                        }
                                                        if (slip.has("overtime_pay") && !slip.get("overtime_pay").isJsonNull()) {
                                                            overtimePay = slip.get("overtime_pay").getAsDouble();
                                                        }
                                                        if (slip.has("bonus") && !slip.get("bonus").isJsonNull()) {
                                                            bonuss = slip.get("bonus").getAsDouble();
                                                        }
                                                        if (slip.has("benefits") && !slip.get("benefits").isJsonNull()) {
                                                            benefitss = slip.get("benefits").getAsDouble();
                                                        }
                                                        if (slip.has("total_deductions") && !slip.get("total_deductions").isJsonNull()) {
                                                            slipTotalDeductions = Math.abs(slip.get("total_deductions").getAsDouble());
                                                        }
                                                        if (slip.has("net_pay") && !slip.get("net_pay").isJsonNull()) {
                                                            netSalary = slip.get("net_pay").getAsDouble();
                                                        }
                                                        String status = slip.has("status") ? slip.get("status").getAsString() : "Inconnu";
                                                %>
                                                <div class="salary-card mb-3 salary-details"
                                                     data-base="<%= baseSalary %>"
                                                     data-overtime="<%= overtimePay %>"
                                                     data-bonus="<%= bonuss %>"
                                                     data-benefits="<%= benefitss %>"
                                                     data-deductions="<%= slipTotalDeductions %>"
                                                     data-net="<%= netSalary %>">
                                                    <div class="salary-date" style="display:none;"><%= startDate %></div>
                                                    <div class="d-flex justify-content-between align-items-center mb-2">
                                                        <h6 class="mb-0">Période : <%= startDate %> au <%= endDate %></h6>
                                                        <span class="badge <%= "Draft".equals(status) ? "bg-warning" : "bg-success" %>">
                                                                    <%= status %>
                                                                </span>
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
                                                            <table class="table table-bordered table-hover mb-0">
                                                                <thead>
                                                                <tr>
                                                                    <th>Composant</th>
                                                                    <th class="text-end">Montant (Ar)</th>
                                                                </tr>
                                                                </thead>
                                                                <tbody>
                                                                <% for (JsonElement earning : earningsArray) {
                                                                    JsonObject e = earning.getAsJsonObject();
                                                                    String label = e.get("salary_component").getAsString();
                                                                    double amount = e.get("amount").getAsDouble();
                                                                %>
                                                                <tr>
                                                                    <td><%= label %></td>
                                                                    <td class="text-end"><%= formatter.format(amount) %> Ar</td>
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
                                                            <table class="table table-bordered table-hover mb-0">
                                                                <thead>
                                                                <tr>
                                                                    <th>Composant</th>
                                                                    <th class="text-end">Montant (Ar)</th>
                                                                </tr>
                                                                </thead>
                                                                <tbody>
                                                                <% for (JsonElement deduction : deductionsArray) {
                                                                    JsonObject d = deduction.getAsJsonObject();
                                                                    String label = d.get("salary_component").getAsString();
                                                                    double amount = d.get("amount").getAsDouble();
                                                                    if (amount<0){
                                                                        amount = (-1)*amount;
                                                                    }
                                                                %>
                                                                <tr>
                                                                    <td><%= label %></td>
                                                                    <td class="text-end"><%= formatter.format(amount) %> Ar</td>
                                                                </tr>
                                                                <% } %>
                                                                </tbody>
                                                            </table>
                                                        </div>
                                                    </div>
                                                    <% }
                                                    } %>

                                                    <!-- Affichage du salaire net -->
                                                    <div class="row mt-3">
                                                        <div class="col-md-6">
                                                            <div class="alert alert-primary mb-0" role="alert">
                                                                <h5 class="alert-heading">Salaire Net</h5>
                                                                <p class="mb-0 fs-4 fw-bold">
                                                                    <%= formatter.format(netSalary) %> Ar
                                                                </p>
                                                            </div>
                                                        </div>
                                                        <div class="col-md-6 text-end">
                                                            <a href="/Salary-Export?namesalaryslip=<%= URLEncoder.encode(slipName, "UTF-8") %>" class="btn btn-primary" aria-label="Exporter la fiche de paie <%= slipName %> en PDF">
                                                                <i class="bi bi-download"></i> Exporter en PDF
                                                            </a>
                                                        </div>
                                                    </div>
                                                </div>
                                                <% } %>
                                                <% } %>
                                            </div>
                                        </div>
                                </td>
                            </tr>
                            <% } %>
                            </tbody>
                        </table>
                    </div>
                    <% } %>
                </div>
            </div>
        </div>
    </div>
</div>

<script>
    // Données globales pour les totaux
    let salaryData = [];

    // Fonction pour formater un nombre avec 2 décimales
    function formatNumber(num) {
        return parseFloat(num || 0).toFixed(2).replace(/\.?0+$/, '');
    }

    // Fonction pour convertir une date au format YYYY-MM en timestamp
    function dateToTimestamp(dateStr) {
        if (!dateStr) return 0;
        const [year, month] = dateStr.split('-').map(Number);
        return new Date(year, month - 1, 1).getTime();
    }

    // Fonction pour extraire les données des fiches de paie
    function extractSalaryData() {
        const salaryElements = document.querySelectorAll('.salary-details');
        salaryData = [];

        salaryElements.forEach(el => {
            const salary = {
                element: el.parentElement.parentElement, // L'élément à afficher/masquer
                date: el.querySelector('.salary-date').textContent.trim(),
                base: parseFloat(el.getAttribute('data-base') || 0),
                overtime: parseFloat(el.getAttribute('data-overtime') || 0),
                bonus: parseFloat(el.getAttribute('data-bonus') || 0),
                benefits: parseFloat(el.getAttribute('data-benefits') || 0),
                deductions: parseFloat(el.getAttribute('data-deductions') || 0),
                net: parseFloat(el.getAttribute('data-net') || 0)
            };
            salaryData.push(salary);
        });
    }

    // Fonction pour mettre à jour les totaux
    function updateTotals(filteredData) {
        const totals = {
            base: 0,
            overtime: 0,
            bonus: 0,
            benefits: 0,
            deductions: 0,
            net: 0
        };

        filteredData.forEach(salary => {
            totals.base += salary.base;
            totals.overtime += salary.overtime;
            totals.bonus += salary.bonus;
            totals.benefits += salary.benefits;
            totals.deductions += salary.deductions;
            totals.net += salary.net;
        });

        // Mise à jour de l'UI
        document.getElementById('totalBase').textContent = formatNumber(totals.base);
        document.getElementById('totalOvertime').textContent = formatNumber(totals.overtime);
        document.getElementById('totalBonus').textContent = formatNumber(totals.bonus);
        document.getElementById('totalBenefits').textContent = formatNumber(totals.benefits);
        document.getElementById('totalDeductions').textContent = formatNumber(totals.deductions);
        document.getElementById('totalNet').textContent = formatNumber(totals.net);

        // Afficher le conteneur des totaux
        document.getElementById('totalsContainer').style.display = 'block';
    }

    // Initialisation au chargement de la page
    document.addEventListener('DOMContentLoaded', function() {
        // Extraire les données des fiches de paie
        extractSalaryData();

        // Ajouter les écouteurs d'événements
        const accordionButtons = document.querySelectorAll('.accordion-button, button[data-bs-toggle="collapse"]');

        accordionButtons.forEach(button => {
            button.addEventListener('click', function() {
                const targetId = this.getAttribute('data-bs-target');
                const target = document.querySelector(targetId);
                const isExpanded = this.getAttribute('aria-expanded') === 'true';

                // Fermer tous les autres panneaux
                document.querySelectorAll('.accordion-collapse').forEach(panel => {
                    if (panel.id !== targetId.substring(1)) {
                        panel.classList.remove('show');
                        const btn = document.querySelector(`button[data-bs-target="#${panel.id}"]`);
                        if (btn) btn.setAttribute('aria-expanded', 'false');
                    }
                });

                // Basculer l'état du panneau actuel
                target.classList.toggle('show');
                this.setAttribute('aria-expanded', String(!isExpanded));
            });
        });

        // Afficher tous les totaux par défaut
        updateTotals(salaryData);
    });
</script>
</body>
</html>
