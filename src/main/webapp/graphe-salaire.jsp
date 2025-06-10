<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html>
<head>
    <title>Graphique des salaires par mois</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        .chart-container {
            width: 900px;
            height: 600px; /* Hauteur augmentée */
            margin: 20px auto;
        }
    </style>
</head>
<body>
<jsp:include page="sidebar.jsp" />
<h2 style="text-align: center;">Évolution des Salaires et Composants</h2>

<div class="chart-container">
    <canvas id="salaryChart"></canvas>
</div>

<%
    String rawJson = (String) request.getAttribute("allsalaryslip");
    if (rawJson == null) rawJson = "[]";
%>
<script id="salary-data" type="application/json">
    <%= rawJson %>
</script>

<script>
    try {
        const rawJson = document.getElementById("salary-data").textContent;
        const salaryData = JSON.parse(rawJson);

        // Mois de 1 à 12 pour l'axe des abscisses
        const allMonths = Array.from({length: 12}, (_, i) => 'Mois ' + (i + 1));

        // Préparer les données pour le graphique
        const totalData = new Array(12).fill(0);
        const detailsData = {};

        // Initialiser les structures pour chaque mois
        allMonths.forEach(month => {
            detailsData[month] = {};
        });

        // Agrégation des données
        salaryData.forEach(slip => {
            const date = new Date(slip.posting_date);
            const monthIndex = date.getMonth(); // 0-11
            const monthLabel = 'Mois ' + (monthIndex + 1);

            // Calcul du total
            const netPay = slip.net_pay || 0;
            totalData[monthIndex] = (totalData[monthIndex] || 0) + netPay;

            // Traitement des gains
            if (Array.isArray(slip.earnings)) {
                slip.earnings.forEach(e => {
                    const comp = e.salary_component || 'Gain Autre';
                    detailsData[monthLabel][comp] = (detailsData[monthLabel][comp] || 0) + (e.amount || 0);
                });
            }

            // Traitement des déductions
            if (Array.isArray(slip.deductions)) {
                slip.deductions.forEach(d => {
                    const comp = d.salary_component || 'Déduction Autre';
                    detailsData[monthLabel][comp] = (detailsData[monthLabel][comp] || 0) - (d.amount || 0);
                });
            }
        });

        // Extraire tous les types de composants uniques
        const allComponents = new Set();
        Object.values(detailsData).forEach(monthData => {
            Object.keys(monthData).forEach(comp => allComponents.add(comp));
        });

        // Palette de couleurs saturées
        const saturatedColors = [
            '#FF0000', '#00FF00', '#0000FF', '#FF00FF', '#FFFF00', '#00FFFF',
            '#FF4500', '#9400D3', '#008000', '#4B0082', '#FF8C00', '#7CFC00',
            '#8A2BE2', '#DC143C', '#006400', '#9932CC', '#8B0000', '#483D8B'
        ];

        // Préparer les données pour chaque composant
        let colorIndex = 0;
        const componentDatasets = Array.from(allComponents).map(comp => {
            const data = allMonths.map(month => detailsData[month][comp] || 0);
            const color = saturatedColors[colorIndex % saturatedColors.length];
            colorIndex++;

            return {
                label: comp,
                data: data,
                borderColor: color,
                backgroundColor: color + '40', // Ajoute de la transparence
                borderWidth: 3, // Ligne plus épaisse
                tension: 0.3, // Courbure légèrement augmentée
                fill: false,
                order: 1
            };
        });

        // Créer le graphique
        const ctx = document.getElementById('salaryChart').getContext('2d');
        new Chart(ctx, {
            type: 'line',
            data: {
                labels: allMonths,
                datasets: [
                    {
                        label: 'Total des salaires',
                        data: totalData,
                        borderColor: '#2E86DE',
                        backgroundColor: '#2E86DE40',
                        borderWidth: 4,
                        tension: 0.3,
                        fill: false,
                        order: 0
                    },
                    ...componentDatasets
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false,
                },
                plugins: {
                    title: {
                        display: true,
                        text: 'Évolution des Salaires et Composants',
                        font: {
                            size: 16
                        }
                    },
                    legend: {
                        position: 'bottom',
                        labels: {
                            boxWidth: 12,
                            padding: 20,
                            font: {
                                size: 12
                            }
                        }
                    },
                    tooltip: {
                        mode: 'index',
                        intersect: false
                    }
                },
                scales: {
                    y: {
                        type: 'linear',
                        display: true,
                        position: 'left',
                        title: {
                            display: true,
                            text: 'Montant (USD)',
                            font: {
                                weight: 'bold'
                            }
                        },
                        beginAtZero: false,
                        grid: {
                            color: 'rgba(0,0,0,0.1)'
                        }
                    },
                    x: {
                        grid: {
                            display: false
                        }
                    }
                },
                elements: {
                    point: {
                        radius: 4, // Points plus visibles
                        hoverRadius: 6
                    }
                }
            }
        });

    } catch (error) {
        console.error("Erreur lors du traitement des données :", error);
    }
</script>

</body>
</html>