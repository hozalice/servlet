<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.*" %>
<%@ page import="com.google.gson.*" %>
<%
  String resumeMensuelJson = (String) request.getAttribute("resumeMensuel");
  Object anneeObj = request.getAttribute("annee");
  String anneeStr = (anneeObj != null) ? anneeObj.toString() : "Toutes";
%>
<html>
<head>
  <title>Statistiques Fiches de Paie</title>
  <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
  <style>
    body {
      font-family: 'Segoe UI', sans-serif;
      background-color: #f2f4f8;
      margin: 0;
      padding: 20px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    form {
      background-color: white;
      padding: 20px;
      width: 80%;
      max-width: 800px;
      border-radius: 10px;
      box-shadow: 0 2px 6px rgba(0, 0, 0, 0.1);
      margin-bottom: 30px;
    }

    label {
      margin-right: 10px;
      font-weight: bold;
    }

    input[type="number"], input[type="submit"] {
      padding: 8px;
      margin: 10px 5px;
      border: 1px solid #ccc;
      border-radius: 5px;
    }

    input[type="submit"] {
      background-color: #2e86de;
      color: white;
      cursor: pointer;
      transition: background-color 0.3s ease;
    }

    input[type="submit"]:hover {
      background-color: #1a5276;
    }

    .table-container {
      width: 90%;
      max-width: 900px;
      background-color: white;
      padding: 20px;
      border-radius: 12px;
      box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
      margin-bottom: 30px;
    }

    .chart-container {
      width: 90%;
      max-width: 1200px;
      background-color: white;
      padding: 20px;
      border-radius: 12px;
      box-shadow: 0 3px 10px rgba(0, 0, 0, 0.1);
      margin: 20px 0;
    }

    canvas {
      max-width: 100%;
      height: auto;
    }

    table {
      width: 100%;
      border-collapse: collapse;
      margin-top: 10px;
    }

    th, td {
      border: 1px solid #ccc;
      padding: 12px;
      text-align: left;
      vertical-align: top;
    }

    th {
      background-color: #2e86de;
      color: white;
    }

    tr:nth-child(even) {
      background-color: #f9f9f9;
    }

    tr:hover {
      background-color: #e0f0ff;
    }

    p {
      text-align: center;
      font-style: italic;
      color: #777;
      margin-top: 20px;
    }

    h1 {
      color: #2e86de;
      margin-top: 0;
      margin-bottom: 50px;
    }
  </style>
</head>
<body>

<jsp:include page="sidebar.jsp" />

<!-- Formulaire de filtre (uniquement année) -->
<form method="get" action="statistiques">
  <label for="annee">Année :</label>
  <input type="number" name="annee" id="annee"
         value="<%= (!"Toutes".equals(anneeStr)) ? anneeStr : "" %>"
         min="2000" max="2100" />

  <input type="submit" value="Filtrer" />
</form>

<!-- Liste des résultats sous forme de tableau -->
<div class="table-container">
  <%
    if (resumeMensuelJson != null && !resumeMensuelJson.isEmpty()) {
      JsonObject resumeJson = JsonParser.parseString(resumeMensuelJson).getAsJsonObject();
  %>
  <table>
    <thead>
    <tr>
      <th>Mois</th>
      <th>Total (MGA)</th>
      <th>Détails</th>
    </tr>
    </thead>
    <tbody>
    <% for (String key : resumeJson.keySet()) {
      JsonObject moisJson = resumeJson.getAsJsonObject(key);
      int moisAffiche = moisJson.get("mois").getAsInt();
      double total = moisJson.get("total").getAsDouble();
      JsonObject details = moisJson.getAsJsonObject("details");
      StringBuilder detailString = new StringBuilder();
      for (Map.Entry<String, JsonElement> entry : details.entrySet()) {
        detailString.append(entry.getKey()).append(" : ")
                .append(String.format("%,.2f", entry.getValue().getAsDouble()))
                .append(" MGA<br/>");
      }
    %>
    <tr>
      <td><%= moisAffiche %></td>
      <td><%= String.format("%,.2f", total) %></td>
      <td>
        <form method="get" action="/salaries" style="margin:0;">
          <input type="hidden" name="month" value="<%= moisAffiche %>" />
          <input type="year" name="year" value="<%= (!"Toutes".equals(anneeStr)) ? anneeStr : "" %>" />
          <input type="submit" value="Voir détails" style="background-color:#2e86de; color:white; border:none; padding:6px 10px; border-radius:5px; cursor:pointer;" />
        </form>
      </td>

    </tr>
    <% } %>
    </tbody>
  </table>
  <%
  } else {
  %>
  <p>Aucune donnée trouvée pour le filtre sélectionné.</p>
  <%
    }
  %>
</div>

<!-- Ajouter le conteneur du graphique -->
<div class="chart-container">
  <canvas id="salariesChart"></canvas>
</div>

<!-- Titre en bas -->
<h1>Statistiques mensuelles des fiches de paie</h1>

<script>
  // Récupérer les données du serveur
  const resumeMensuel = <%= resumeMensuelJson %>;
  
  // Préparer les données pour le graphique
  const labels = [];
  const totalData = [];
  const detailsData = {};
  
  // Vérifier si les données existent
  if (resumeMensuel) {
    Object.values(resumeMensuel).forEach(mois => {
      labels.push(`Mois ${mois.mois}`);
      totalData.push(mois.total);
      
      // Collecter tous les types de composants
      Object.keys(mois.details).forEach(type => {
        if (!detailsData[type]) {
          detailsData[type] = [];
        }
        detailsData[type].push(mois.details[type]);
      });
    });

    // Créer le graphique
    const ctx = document.getElementById('salariesChart').getContext('2d');
    new Chart(ctx, {
      type: 'line',
      data: {
        labels: labels,
        datasets: [
          {
            label: 'Total des salaires',
            data: totalData,
            borderColor: 'rgb(46, 134, 222)',
            tension: 0.1,
            fill: false,
            order: 0
          },
          ...Object.entries(detailsData).map(([type, data]) => ({
            label: type,
            data: data,
            borderColor: `hsl(${Math.random() * 360}, 70%, 50%)`,
            tension: 0.1,
            fill: false,
            order: 1
          }))
        ]
      },
      options: {
        responsive: true,
        interaction: {
          mode: 'index',
          intersect: false,
        },
        stacked: false,
        plugins: {
          title: {
            display: true,
            text: 'Évolution des Salaires et Composants'
          }
        },
        scales: {
          y: {
            type: 'linear',
            display: true,
            position: 'left',
            title: {
              display: true,
              text: 'Montant (€)'
            }
          }
        }
      }
    });
  }
</script>

</body>
</html>
