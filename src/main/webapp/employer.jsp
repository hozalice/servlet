<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="fr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Liste des Employés</title>
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

        .container {
            max-width: 1200px;
            margin: 0 auto;
            background-color: white;
            padding: 2rem;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }

        h1 {
            color: #1F272E;
            margin-bottom: 2rem;
            font-size: 24px;
        }

        .search-container {
            margin-bottom: 2rem;
        }

        .search-input {
            width: 100%;
            max-width: 400px;
            padding: 0.75rem;
            border: 1px solid #ddd;
            border-radius: 4px;
            font-size: 16px;
        }

        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 1rem;
        }

        th, td {
            padding: 1rem;
            text-align: left;
            border-bottom: 1px solid #ddd;
        }

        th {
            background-color: #f8f9fa;
            color: #4C5A67;
            font-weight: 600;
        }

        tr:hover {
            background-color: #f5f7fa;
        }

        .status {
            padding: 0.25rem 0.75rem;
            border-radius: 999px;
            font-size: 14px;
            font-weight: 500;
        }

        .status-active {
            background-color: #dcfce7;
            color: #166534;
        }

        .status-inactive {
            background-color: #fee2e2;
            color: #991b1b;
        }

        .loading {
            text-align: center;
            padding: 2rem;
            color: #4C5A67;
        }
    </style>
</head>
<body>
    <nav class="sidebar">
        <div class="sidebar-header">
            <h1>RH System</h1>
        </div>
        <ul class="nav-menu">
            <li class="nav-item">
                <a href="dashboard.jsp" class="nav-link">Dashboard</a>
            </li>
            <li class="nav-item">
                <a href="Employer.jsp" class="nav-link active">Employés</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Paiements</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Fiches de paie</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Congés</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Formation</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Évaluation</a>
            </li>
            <li class="nav-item">
                <a href="#" class="nav-link">Rapports</a>
            </li>
        </ul>
    </nav>

    <div class="container main-content">
        <h1>Liste des Employés</h1>
        
        <div class="search-container">
            <input type="text" 
                   id="searchInput" 
                   class="search-input" 
                   placeholder="Rechercher un employé..." 
                   autocomplete="off">
        </div>

        <div id="employeeTable">
            <table>
                <thead>
                    <tr>
                        <th>ID</th>
                        <th>Nom</th>
                        <th>Département</th>
                        <th>Poste</th>
                        <th>Statut</th>
                    </tr>
                </thead>
                <tbody id="employeeList">
                    <!-- Les données seront insérées ici dynamiquement -->
                </tbody>
            </table>
        </div>
    </div>

    <script>
        // Fonction pour charger les employés
        function loadEmployees(searchQuery = '') {
            const tbody = document.getElementById('employeeList');
            tbody.innerHTML = '<tr><td colspan="5" class="loading">Chargement...</td></tr>';

            fetch('Employer-Controller${not empty searchQuery ? concat("?search=", searchQuery) : ""}')
                .then(response => response.json())
                .then(data => {
                    if (data.message && Array.isArray(data.message)) {
                        const employees = data.message;
                        tbody.innerHTML = employees.map(employee => `
                            <tr>
                                <td>${employee.name}</td>
                                <td>${employee.employee_name}</td>
                                <td>${employee.department || '-'}</td>
                                <td>${employee.designation || '-'}</td>
                                <td>
                                    <span class="status ${employee.status === 'Active' ? 'status-active' : 'status-inactive'}">
                                        ${employee.status}
                                    </span>
                                </td>
                            </tr>
                        `).join('');
                    } else {
                        tbody.innerHTML = '<tr><td colspan="5" class="loading">Aucun employé trouvé</td></tr>';
                    }
                })
                .catch(error => {
                    console.error('Erreur:', error);
                    tbody.innerHTML = '<tr><td colspan="5" class="loading">Erreur lors du chargement des données</td></tr>';
                });
        }

        // Gestionnaire d'événements pour la recherche
        let searchTimeout;
        document.getElementById('searchInput').addEventListener('input', (e) => {
            clearTimeout(searchTimeout);
            searchTimeout = setTimeout(() => {
                loadEmployees(e.target.value);
            }, 300);
        });

        // Charger les employés au chargement de la page
        document.addEventListener('DOMContentLoaded', () => {
            loadEmployees();
        });
    </script>
</body>
</html>