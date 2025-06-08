<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.*" %>
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
        }

        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f8f9fa;
            min-height: 100vh;
            color: #333;
            margin: 0;
            padding: 0;
        }

        .container {
            width: 60%;
            margin: 0 auto;
            padding: 1.5rem;
            background: white;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
            margin-top: 1rem;
            margin-bottom: 1rem;
        }

        h2 {
            text-align: center;
            color: #2c3e50;
            margin-bottom: 2rem;
            font-size: 2rem;
            font-weight: 600;
            border-bottom: 2px solid #e9ecef;
            padding-bottom: 1rem;
        }

        .search-container {
            margin-bottom: 2rem;
            display: flex;
            justify-content: center;
        }

        .search-form {
            display: flex;
            width: 100%;
            max-width: none;
            background: #f8f9fa;
            border-radius: 6px;
            overflow: hidden;
            border: 1px solid #dee2e6;
            transition: all 0.3s ease;
        }

        .search-form:focus-within {
            border-color: #6c757d;
            box-shadow: 0 0 0 0.2rem rgba(108, 117, 125, 0.25);
        }

        .search-input {
            flex: 1;
            padding: 0.75rem 1rem;
            border: none;
            outline: none;
            font-size: 1rem;
            background: transparent;
        }

        .search-btn {
            padding: 0.75rem 2rem;
            background: #6c757d;
            color: white;
            border: none;
            cursor: pointer;
            font-size: 1rem;
            font-weight: 500;
            transition: all 0.3s ease;
        }

        .search-btn:hover {
            background: #5a6268;
        }

        .table-container {
            background: white;
            border-radius: 6px;
            overflow: hidden;
            border: 1px solid #dee2e6;
            margin-top: 1.5rem;
        }

        table {
            width: 100%;
            border-collapse: collapse;
        }

        thead {
            background: #f8f9fa;
            border-bottom: 2px solid #dee2e6;
        }

        th {
            padding: 1rem;
            text-align: left;
            font-weight: 600;
            font-size: 0.9rem;
            letter-spacing: 0.5px;
            text-transform: uppercase;
            color: #495057;
            border-right: 1px solid #dee2e6;
        }

        th:last-child {
            border-right: none;
        }

        tbody tr {
            transition: all 0.2s ease;
            border-bottom: 1px solid #f1f3f4;
        }

        tbody tr:hover {
            background: #f8f9fa;
        }

        td {
            padding: 1rem;
            border-right: 1px solid #f1f3f4;
            vertical-align: middle;
        }

        td:last-child {
            border-right: none;
        }

        .action-btn {
            background: #6c757d;
            color: white;
            border: none;
            padding: 0.5rem 1rem;
            border-radius: 4px;
            cursor: pointer;
            font-size: 0.875rem;
            font-weight: 500;
            transition: all 0.3s ease;
            text-decoration: none;
            display: inline-block;
        }

        .action-btn:hover {
            background: #5a6268;
            color: white;
            text-decoration: none;
        }

        .status-badge {
            padding: 0.25rem 0.5rem;
            border-radius: 4px;
            font-size: 0.75rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .status-active {
            background: #d4edda;
            color: #155724;
            border: 1px solid #c3e6cb;
        }

        .status-inactive {
            background: #f8d7da;
            color: #721c24;
            border: 1px solid #f5c6cb;
        }

        .empty-state {
            text-align: center;
            padding: 3rem;
            color: #666;
            font-size: 1.1rem;
        }

        .empty-state i {
            font-size: 3rem;
            margin-bottom: 1rem;
            opacity: 0.5;
        }

        @media (max-width: 768px) {
            .container {
                margin: 0.5rem;
                padding: 1rem;
                border-radius: 6px;
            }

            h2 {
                font-size: 1.5rem;
            }

            .table-container {
                overflow-x: auto;
            }

            table {
                min-width: 700px;
            }

            th, td {
                padding: 0.75rem 0.5rem;
                font-size: 0.875rem;
            }
        }

        /* Animation d'entrée */
        @keyframes fadeIn {
            from {
                opacity: 0;
            }
            to {
                opacity: 1;
            }
        }

        .container {
            animation: fadeIn 0.5s ease-out;
        }

        /* Styles pour le statut */
        .status-badge {
            padding: 0.3rem 0.8rem;
            border-radius: 20px;
            font-size: 0.8rem;
            font-weight: 600;
            text-transform: uppercase;
            letter-spacing: 0.5px;
        }

        .status-active {
            background: rgba(39, 174, 96, 0.1);
            color: #27ae60;
            border: 1px solid rgba(39, 174, 96, 0.3);
        }

        .status-inactive {
            background: rgba(231, 76, 60, 0.1);
            color: #e74c3c;
            border: 1px solid rgba(231, 76, 60, 0.3);
        }
    </style>
</head>
<body>
<%@include file="sidebar.jsp"%>

<div class="container">
    <h2>Liste des Employés</h2>

    <!-- Formulaire de recherche -->
    <div class="search-container">
        <form action="Employer-Controller" method="get" class="search-form">
            <input type="text" name="search" class="search-input" placeholder="Rechercher un employé...">
            <button type="submit" class="search-btn">Rechercher</button>
        </form>
    </div>

    <!-- Tableau des employés -->
    <div class="table-container">
        <table>
            <thead>
            <tr>
                <th>ID</th>
                <th>Nom</th>
                <th>Département</th>
                <th>Poste</th>
                <th>Statut</th>
                <th>Actions</th>
            </tr>
            </thead>
            <tbody>
            <%
                String employeesData = (String) request.getAttribute("employeesData");
                if (employeesData != null) {
                    JsonParser parser = new JsonParser();
                    JsonObject jsonResponse = parser.parse(employeesData).getAsJsonObject();
                    JsonArray employees = jsonResponse.get("data").getAsJsonArray();

                    if (employees.size() == 0) {
            %>
            <tr>
                <td colspan="6" class="empty-state">
                    <i>📋</i><br>
                    Aucun employé trouvé
                </td>
            </tr>
            <%
            } else {
                for (JsonElement element : employees) {
                    JsonObject employee = element.getAsJsonObject();
                    String id = employee.has("name") && !employee.get("name").isJsonNull() ?
                            employee.get("name").getAsString() : "";
                    String name = employee.has("employee_name") && !employee.get("employee_name").isJsonNull() ?
                            employee.get("employee_name").getAsString() : "";
                    String department = employee.has("department") && !employee.get("department").isJsonNull() ?
                            employee.get("department").getAsString() : "";
                    String designation = employee.has("designation") && !employee.get("designation").isJsonNull() ?
                            employee.get("designation").getAsString() : "";
                    String status = employee.has("status") && !employee.get("status").isJsonNull() ?
                            employee.get("status").getAsString() : "Inactive";

                    String statusClass = status.equalsIgnoreCase("Active") ? "status-active" : "status-inactive";
            %>
            <tr>
                <td><%= id %></td>
                <td><%= name %></td>
                <td><%= department %></td>
                <td><%= designation %></td>
                <td>
                                            <span class="status-badge <%= statusClass %>"><%= status %>
                                            </span>
                </td>
                <td>
                    <a href="/Salary-Slip?employer=<%= id %>" class="action-btn">
                        Voir détails
                    </a>
                </td>
            </tr>
            <%
                    }
                }
            } else {
            %>
            <tr>
                <td colspan="6" class="empty-state">
                    <i>⚠️</i><br>
                    Aucune donnée disponible
                </td>
            </tr>
            <%
                }
            %>
            </tbody>
        </table>
    </div>
</div>
</body>
</html>