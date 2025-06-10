<nav class="sidebar">
  <div class="sidebar-header">
      <h1>RH System</h1>
  </div>
  <ul class="nav-menu">
      <li class="nav-item">
          <a href="dashboard.jsp" class="nav-link active">Importation de données</a>
      </li>
      <li class="nav-item">
          <a href="/Employer-Controller" class="nav-link">Employés</a>
      </li>
      <li class="nav-item">
          <a href="/statistiques" class="nav-link">Total Salaires</a>
      </li>
      <li class="nav-item">
          <a href="/Salary-Slip" class="nav-link">Fiches de paie</a>
      </li>
      <li class="nav-item">
      <li class="nav-item">
          <a href="/salaries" class="nav-link">Detail Fiche de paye</a>
      </li>
      <li class="nav-item">
          <a href="/graphe" class="nav-link">Graphe</a>
      </li>
  </ul>
</nav>

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
    </style>
