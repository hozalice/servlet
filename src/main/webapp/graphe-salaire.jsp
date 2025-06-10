<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Graphique des salaires par mois</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        body {
            font-family: Arial, sans-serif;
            margin: 0; padding: 0;
            background-color: #f4f6f9;
        }
        .main-container {
            width: 70%;
            margin: 0 auto;
            padding: 20px;
        }
        h2 {
            text-align: center;
            margin-top: 30px;
            margin-bottom: 20px;
        }
        .form-container {
            display: flex;
            justify-content: center;
            margin-bottom: 30px;
        }
        form {
            display: flex;
            gap: 10px;
            align-items: center;
        }
        input[type="text"] {
            padding: 8px 12px;
            font-size: 16px;
            border: 1px solid #ccc;
            border-radius: 4px;
        }
        button {
            padding: 8px 16px;
            font-size: 16px;
            background-color: #2E86DE;
            color: white;
            border: none;
            border-radius: 4px;
            cursor: pointer;
        }
        .chart-container {
            width: 100%;
            height: 600px;
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 0 8px rgba(0,0,0,0.1);
        }
    </style>
</head>
<body>
<jsp:include page="sidebar.jsp" />

<div class="main-container">
    <h2>Évolution des Composants de Salaire</h2>

    <div class="form-container">
        <form action="/graphe" method="get">
            <label for="annee">Année :</label>
            <input type="text" id="annee" name="annee" placeholder="ex : 2024"
                   value="<%= request.getParameter("annee") != null ? request.getParameter("annee") : "" %>"/>
            <button type="submit">Filtrer</button>
        </form>
    </div>

    <div class="chart-container">
        <canvas id="salaryChart"></canvas>
    </div>
</div>

<%
    String rawJson = (String) request.getAttribute("totals");
    if (rawJson == null) rawJson = "{}";
%>

<script id="salary-data" type="application/json">
    <%= rawJson %>
</script>

<script>
    (function () {
        try {
            const rawJson = document.getElementById("salary-data").textContent;
            const salaryData = JSON.parse(rawJson);

            const earningsByMonth = salaryData.earningsByMonth || {};
            const deductionsByMonth = salaryData.deductionsByMonth || {};

            // Récupérer toutes les clés mois au format "YYYY-MM"
            const rawMonths = Object.keys({ ...earningsByMonth, ...deductionsByMonth }).sort();

            // Tableau des noms courts des mois en français
            const monthNames = ["Jan", "Fév", "Mar", "Avr", "Mai", "Juin", "Juil", "Août", "Sep", "Oct", "Nov", "Déc"];

            // Transformer "YYYY-MM" en "Mois" (ex : "2024-01" -> "Jan")
            const allMonths = rawMonths.map(m => {
                const monthIndex = parseInt(m.split("-")[1], 10) - 1;
                return monthNames[monthIndex] || m;
            });

            const allComponents = new Set();

            rawMonths.forEach(month => {
                if (earningsByMonth[month]) {
                    Object.keys(earningsByMonth[month]).forEach(c => allComponents.add(c));
                }
                if (deductionsByMonth[month]) {
                    Object.keys(deductionsByMonth[month]).forEach(c => allComponents.add(c));
                }
            });

            const saturatedColors = [
                '#FF0000', '#00FF00', '#0000FF', '#FF00FF', '#FFFF00', '#00FFFF',
                '#FF4500', '#9400D3', '#008000', '#4B0082', '#FF8C00', '#7CFC00',
                '#8A2BE2', '#DC143C', '#006400', '#9932CC', '#8B0000', '#483D8B'
            ];

            const datasets = [];
            let colorIndex = 0;

            allComponents.forEach(component => {
                const data = rawMonths.map(month => {
                    const earnings = earningsByMonth[month]?.[component] || 0;
                    const deductions = deductionsByMonth[month]?.[component] || 0;
                    return earnings + deductions; // déductions sont négatives déjà dans la méthode Java
                });
                datasets.push({
                    label: component,
                    data: data,
                    borderColor: saturatedColors[colorIndex % saturatedColors.length],
                    backgroundColor: saturatedColors[colorIndex % saturatedColors.length] + "40",
                    borderWidth: 2,
                    tension: 0.4,
                    fill: false
                });
                colorIndex++;
            });

            const ctx = document.getElementById('salaryChart').getContext('2d');
            new Chart(ctx, {
                type: 'line',
                data: {
                    labels: allMonths,
                    datasets: datasets
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    interaction: {
                        mode: 'index',
                        intersect: false
                    },
                    plugins: {
                        title: {
                            display: true,
                            text: 'Évolution des Composants de Salaire',
                            font: { size: 18 }
                        },
                        legend: {
                            position: 'bottom',
                            labels: {
                                boxWidth: 12,
                                padding: 20,
                                font: { size: 12 }
                            }
                        },
                        tooltip: {
                            mode: 'index',
                            intersect: false
                        }
                    },
                    scales: {
                        y: {
                            beginAtZero: false,
                            title: {
                                display: true,
                                text: 'Montant (Ar)',
                                font: { weight: 'bold' }
                            }
                        },
                        x: {
                            title: {
                                display: true,
                                text: 'Mois'
                            }
                        }
                    },
                    elements: {
                        point: {
                            radius: 4,
                            hoverRadius: 6
                        }
                    }
                }
            });
        } catch (error) {
            console.error("Erreur lors du chargement du graphique :", error);
        }
    })();
</script>


</body>
</html>
