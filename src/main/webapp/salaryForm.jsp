<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.google.gson.JsonArray, com.google.gson.JsonObject" %>
<html>
<head>
  <title>Structure du Salaire</title>
</head>
<body>
<h1>Champs de Salary Structure</h1>

<%
  JsonArray fields = (JsonArray) request.getAttribute("fields");
  if (fields != null) {
    for (int i = 0; i < fields.size(); i++) {
      JsonObject field = fields.get(i).getAsJsonObject();
      String label = field.has("label") ? field.get("label").getAsString() : "(no label)";
      String fieldname = field.has("fieldname") ? field.get("fieldname").getAsString() : "(no fieldname)";
      String fieldtype = field.has("fieldtype") ? field.get("fieldtype").getAsString() : "(no fieldtype)";
%>
<div>
  <strong><%= label %></strong> — <%= fieldname %> (<%= fieldtype %>)
</div>
<%
  }
} else {
%>
<p>Aucun champ trouvé.</p>
<%
  }
%>

</body>
</html>
