<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.text.NumberFormat" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.Calendar" %>

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
            width: 65%;
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
            background-color: #f8f9fa;
            border-bottom: 1px solid #eaeaea;
            padding: 15px 20px;
        }
        .card-header h2 {
            margin: 0;
            font-size: 1.5rem;
            color: #333;
        }
        .card-body {
            padding: 20px;
        }
        .accordion-button {
            width: 100%;
            text-align: left;
            background: none;
            border: none;
            padding: 15px 20px;
            font-weight: 500;
            cursor: pointer;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .accordion-button:after {
            content: '▼';
            font-size: 0.8em;
            transition: transform 0.2s;
        }
        .accordion-button[aria-expanded="true"]:after {
            transform: rotate(180deg);
        }
        .accordion-collapse {
            display: none;
            overflow: hidden;
        }
        .accordion-collapse.show {
            display: block;
        }
        .alert {
            padding: 15px;
            margin-bottom: 20px;
            border: 1px solid transparent;
            border-radius: 4px;
        }
        .alert-danger {
            color: #721c24;
            background-color: #f8d7da;
            border-color: #f5c6cb;
        }
        .alert-info {
            color: #0c5460;
            background-color: #d1ecf1;
            border-color: #bee5eb;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 15px 0;
        }
        th, td {
            padding: 12px 15px;
            text-align: left;
            border-bottom: 1px solid #dee2e6;
        }
        th {
            background-color: #f8f9fa;
            font-weight: 600;
        }
        .btn {
            display: inline-block;
            padding: 6px 12px;
            margin-bottom: 0;
            font-size: 14px;
            font-weight: 400;
            line-height: 1.5;
            text-align: center;
            white-space: nowrap;
            vertical-align: middle;
            cursor: pointer;
            border: 1px solid transparent;
            border-radius: 4px;
            text-decoration: none;
        }
        .btn-primary {
            color: #fff;
            background-color: #007bff;
            border-color: #007bff;
        }
        .btn-primary:hover {
            background-color: #0069d9;
            border-color: #0062cc;
        }
        .badge {
            display: inline-block;
            padding: 0.35em 0.65em;
            font-size: 0.75em;
            font-weight: 700;
            line-height: 1;
            text-align: center;
            white-space: nowrap;
            vertical-align: baseline;
            border-radius: 0.25rem;
        }
        .bg-success {
            background-color: #28a745 !important;
        }
        .bg-warning {
            background-color: #ffc107 !important;
            color: #212529;
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

                    double base = 0, overtime = 0, bonus = 0, benefits = 0, deductions = 0;

                    if (totalsJsonStr != null && !totalsJsonStr.isEmpty()) {
                        JsonObject totalsJson = JsonParser.parseString(totalsJsonStr).getAsJsonObject();

                        JsonObject earnings = totalsJson.getAsJsonObject("earningsTotals");
                        if (earnings != null) {
                            base += earnings.has("Salaire de Base") ? earnings.get("Salaire de Base").getAsDouble() : 0.0;
                            base += earnings.has("Base Mensuelle") ? earnings.get("Base Mensuelle").getAsDouble() : 0.0;
                            base += earnings.has("Salaire Fixe") ? earnings.get("Salaire Fixe").getAsDouble() : 0.0;
                            base += earnings.has("Salaire Base") ? earnings.get("Salaire Base").getAsDouble() : 0.0;

                            bonus += earnings.has("Bonus Performance") ? earnings.get("Bonus Performance").getAsDouble() : 0.0;
                            bonus += earnings.has("Prime Ancienneté") ? earnings.get("Prime Ancienneté").getAsDouble() : 0.0;

                            benefits += earnings.has("Indemnité") ? earnings.get("Indemnité").getAsDouble() : 0.0;
                            benefits += earnings.has("Indemnité Logement") ? earnings.get("Indemnité Logement").getAsDouble() : 0.0;
                            benefits += earnings.has("Indemnité Transport") ? earnings.get("Indemnité Transport").getAsDouble() : 0.0;
                        }

                        JsonObject deductionJson = totalsJson.getAsJsonObject("deductionsTotals");
                        if (deductionJson != null) {
                            deductions += deductionJson.has("Impôt sur revenu") ? deductionJson.get("Impôt sur revenu").getAsDouble() : 0.0;
                            deductions += deductionJson.has("Taxe Professionnelle") ? deductionJson.get("Taxe Professionnelle").getAsDouble() : 0.0;
                            deductions += deductionJson.has("Taxe sociale") ? deductionJson.get("Taxe sociale").getAsDouble() : 0.0;
                        }
                    }

                    double net = base + overtime + bonus + benefits - deductions;
                %>

                <%-- Bloc affiché uniquement si le JSON est fourni --%>
                <% if (totalsJsonStr != null && !totalsJsonStr.isEmpty()) { %>
                <div id="totalsContainer" style="background: #f8f9fa; padding: 15px; border-radius: 5px; margin-top: 15px;">
                    <h4>Récapitulatif</h4>
                    <div style="display: flex; gap: 30px; flex-wrap: wrap;">
                        <div>
                            <div>Salaire de base: <span><%= String.format("%.2f", base) %></span> Ar</div>
                            <div>Heures supplémentaires: <span><%= String.format("%.2f", overtime) %></span> Ar</div>
                            <div>Bonus: <span><%= String.format("%.2f", bonus) %></span> €</div>
                        </div>
                        <div>
                            <div>Avantages: <span><%= String.format("%.2f", benefits) %></span> Ar</div>
                            <div>Retenues: <span><%= String.format("%.2f", deductions) %></span> Ar</div>
                            <div><strong>Total net: <span><%= String.format("%.2f", net) %></span> Ar</strong></div>
                        </div>
                    </div>
                </div>
                <% } %>
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
                                                        <%
                                                            // Récupération sécurisée des valeurs avec des valeurs par défaut
                                                            double baseSalary = slip.has("base_salary") && !slip.get("base_salary").isJsonNull() ? slip.get("base_salary").getAsDouble() : 0.0;
                                                            double overtimePay = slip.has("overtime_pay") && !slip.get("overtime_pay").isJsonNull() ? slip.get("overtime_pay").getAsDouble() : 0.0;
                                                            double bonuss = slip.has("bonus") && !slip.get("bonus").isJsonNull() ? slip.get("bonus").getAsDouble() : 0.0;
                                                            double benefitss = slip.has("benefits") && !slip.get("benefits").isJsonNull() ? slip.get("benefits").getAsDouble() : 0.0;
                                                            double totalDeductions = slip.has("total_deductions") && !slip.get("total_deductions").isJsonNull() ? Math.abs(slip.get("total_deductions").getAsDouble()) : 0.0;
                                                            double netSalary = slip.has("net_pay") && !slip.get("net_pay").isJsonNull() ? slip.get("net_pay").getAsDouble() : 0.0;
                                                        %>
                                                        <div class="salary-card mb-3 salary-details"
                                                             data-base="<%= baseSalary %>"
                                                             data-overtime="<%= overtimePay %>"
                                                             data-bonus="<%= bonuss %>"
                                                             data-benefits="<%= benefitss %>"
                                                             data-deductions="<%= totalDeductions %>"
                                                             data-net="<%= netSalary %>">
                                                    <div class="salary-date" style="display:none;"><%= startDate %></div>
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
            document.getElementById('applyFilter').addEventListener('click', applyFilter);

            document.getElementById('resetFilter').addEventListener('click', function() {
                document.getElementById('startDate').value = '';
                document.getElementById('endDate').value = '';
                salaryData.forEach(salary => {
                    salary.element.style.display = '';
                });
                updateTotals(salaryData);
            });

            // Afficher tous les totaux par défaut
            updateTotals(salaryData);
        });

        // Script pour gérer l'accordéon
        document.addEventListener('DOMContentLoaded', function() {
            const accordionButtons = document.querySelectorAll('.accordion-button');

            accordionButtons.forEach(button => {
                button.addEventListener('click', function() {
                    const targetId = this.getAttribute('data-bs-target');
                    const target = document.querySelector(targetId);
                    const isExpanded = this.getAttribute('aria-expanded') === 'true';

                    // Fermer tous les autres panneaux
                    document.querySelectorAll('.accordion-collapse').forEach(panel => {
                        if (panel.id !== targetId.substring(1)) {
                            panel.classList.remove('show');
                            panel.previousElementSibling.setAttribute('aria-expanded', 'false');
                        }
                    });

                    // Basculer l'état du panneau actuel
                    target.classList.toggle('show');
                    this.setAttribute('aria-expanded', String(!isExpanded));
                });
            });
        });
    </script>
</body>
</html>
