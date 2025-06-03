<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Dashboard RH</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", "Helvetica Neue", sans-serif;
        }

        body {
            background-color: #f5f7fa;
            display: flex;
        }

        .sidebar {
            width: 280px;
            background-color: white;
            height: 100vh;
            position: fixed;
            right: 0;
            box-shadow: -2px 0 4px rgba(0, 0, 0, 0.1);
            padding: 2rem 0;
        }

        .sidebar-header {
            padding: 0 1.5rem;
            margin-bottom: 2rem;
        }

        .sidebar-header h1 {
            color: #1F272E;
            font-size: 24px;
            margin-bottom: 0.5rem;
        }

        .nav-menu {
            list-style: none;
        }

        .nav-item {
            margin-bottom: 0.5rem;
        }

        .nav-link {
            display: flex;
            align-items: center;
            padding: 0.75rem 1.5rem;
            color: #4C5A67;
            text-decoration: none;
            transition: all 0.2s;
        }

        .nav-link:hover {
            background-color: #f5f7fa;
            color: #2490EF;
        }

        .nav-link.active {
            background-color: #e6f0f9;
            color: #2490EF;
            border-right: 3px solid #2490EF;
        }

        .main-content {
            flex: 1;
            padding: 2rem;
            margin-right: 280px;
        }

        .dashboard-header {
            margin-bottom: 2rem;
        }

        .dashboard-header h2 {
            color: #1F272E;
            font-size: 24px;
            margin-bottom: 0.5rem;
        }

        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 1.5rem;
            margin-bottom: 2rem;
        }

        .stat-card {
            background-color: white;
            padding: 1.5rem;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
        }

        .stat-card h3 {
            color: #4C5A67;
            font-size: 14px;
            margin-bottom: 0.5rem;
        }

        .stat-card .value {
            color: #1F272E;
            font-size: 24px;
            font-weight: 600;
        }

        .import-section {
            background-color: white;
            padding: 2rem;
            border-radius: 10px;
            box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
            margin-top: 3rem;
        }

        .import-section h2 {
            margin-bottom: 1.5rem;
            color: #1F272E;
        }

        .import-section label {
            display: block;
            margin-bottom: 0.5rem;
            color: #4C5A67;
            font-weight: 500;
        }

        .import-section input[type="file"] {
            width: 100%;
            margin-bottom: 1rem;
            padding: 0.5rem;
            border: 1px solid #ccc;
            border-radius: 6px;
        }

        .import-section button {
            background-color: #2490EF;
            color: white;
            padding: 0.75rem 1.5rem;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-size: 16px;
            transition: background-color 0.3s;
        }

        .import-section button:hover {
            background-color: #1a76c2;
        }

        .import-section .message-success {
            color: green;
            margin-top: 1rem;
        }

        .import-section .message-error {
            color: red;
            margin-top: 1rem;
        }
    </style>
</head>
<body>
<%@ include file="sidebar.jsp" %>

<main class="main-content">
    <div class="dashboard-header">
        <h2>Tableau de bord</h2>
        <p>Bienvenue dans votre espace de gestion RH</p>
    </div>

    <div class="stats-grid">
        <div class="stat-card">
            <h3>Employés actifs</h3>
            <div class="value">127</div>
        </div>
        <div class="stat-card">
            <h3>Nouveaux ce mois</h3>
            <div class="value">4</div>
        </div>
        <div class="stat-card">
            <h3>Congés en cours</h3>
            <div class="value">8</div>
        </div>
        <div class="stat-card">
            <h3>Évaluations en attente</h3>
            <div class="value">12</div>
        </div>
    </div>

    <!-- Section d'importation CSV -->
    <div class="import-section">
        <h2>Import des fichiers CSV</h2>

        <form action="import-csv" method="post" enctype="multipart/form-data">
            <div>
                <label for="employesCsv">Fichier Employés (.csv) :</label>
                <input type="file" name="employesCsv" id="employesCsv" accept=".csv" required />
            </div>
            <div>
                <label for="structuresCsv">Fichier Structures Salariales (.csv) :</label>
                <input type="file" name="structuresCsv" id="structuresCsv" accept=".csv" required />
            </div>
            <div>
                <label for="paiesCsv">Fichier Paies (.csv) :</label>
                <input type="file" name="paiesCsv" id="paiesCsv" accept=".csv" required />
            </div>
            <button type="submit">Importer</button>
        </form>

        <%-- Affichage uniquement du message de succès --%>
        <% if (request.getAttribute("message") != null) { %>
        <p class="message-success"><%= request.getAttribute("message") %></p>
        <% } %>
    </div>
</main>
</body>
</html>
