<%--
  Created by IntelliJ IDEA.
  User: Hozalice
  Date: 25/06/2025
  Time: 09:00
  To change this template use File | Settings | File Templates.
--%>
<%@ page import="com.google.gson.*" %>
<%@ page import="java.util.*" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE html>
<html lang="fr">
<head>
  <meta charset="UTF-8">
  <title>Formulaire Salaire</title>
  <style>
    /* Ton style CSS reste inchangé ici */
    body {
      font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
      background: #f0f4f8;
      display: flex;
      justify-content: center;
      align-items: center;
      height: 100vh;
      margin: 0;
    }
    .form-container {
      background-color: #ffffff;
      border-radius: 10px;
      padding: 30px 40px;
      box-shadow: 0 0 15px rgba(0, 0, 0, 0.1);
      max-width: 450px;
      width: 100%;
      border-top: 5px solid #007BFF;
    }
    h2 {
      text-align: center;
      color: #007BFF;
      margin-bottom: 25px;
    }
    .form-group {
      margin-bottom: 20px;
    }
    label {
      display: block;
      margin-bottom: 6px;
      color: #333;
      font-weight: bold;
    }
    input, select {
      width: 100%;
      padding: 10px;
      border: 1px solid #ccc;
      border-radius: 6px;
      background-color: #f9f9f9;
      transition: border-color 0.3s ease;
    }
    input:focus, select:focus {
      border-color: #007BFF;
      outline: none;
      background-color: #fff;
    }
    button {
      width: 100%;
      padding: 10px;
      background-color: #007BFF;
      border: none;
      border-radius: 6px;
      color: #fff;
      font-size: 16px;
      cursor: pointer;
      transition: background-color 0.3s ease;
    }
    button:hover {
      background-color: #0056b3;
    }
  </style>
</head>
<body>
<%@ include file="sidebar.jsp" %>

<div class="form-container">
  <h2>Formulaire de règle de salaire</h2>
  <form method="post" action="/recherche">
    <div class="form-group">
      <label for="salary_component">Composant de salaire :</label>
      <select id="salary_component" name="salary_component">
        <%
          JsonArray salarieComponents = (JsonArray) request.getAttribute("salarie_component");
          if (salarieComponents != null) {
            for (JsonElement element : salarieComponents) {
              JsonObject comp = element.getAsJsonObject();
              String value = comp.get("name").getAsString();
              String label = comp.get("salary_component").getAsString();
              String type = comp.get("type").getAsString();
        %>
        <option value="<%= value %>"><%= label %> (<%= type %>)</option>
        <%
          }
        } else {
        %>
        <option disabled>Aucun composant trouvé</option>
        <%
          }
        %>
      </select>
    </div>

    <div class="form-group">
      <label for="condition">Condition :</label>
      <select id="condition" name="condition">
        <option value=">">Supérieur à</option>
        <option value="<">Inférieur à</option>
        <option value="=">Égal à</option>
      </select>
    </div>

    <div class="form-group">
      <label for="amount">Montant :</label>
      <input type="number" id="amount" name="amount" placeholder="Entrer un montant">
    </div>

    <button type="submit">Valider</button>
  </form>
</div>

</body>
</html>

