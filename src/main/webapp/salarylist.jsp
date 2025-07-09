<%@ page import="com.google.gson.JsonArray" %>
<%@ page import="com.google.gson.JsonElement" %>
<%@ page import="com.google.gson.JsonObject" %><%--
  Created by IntelliJ IDEA.
  User: Hozalice
  Date: 25/06/2025
  Time: 10:58
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<html>
<head>
    <title>Liste des Salary Slips</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: #f4f6f8;
            margin: 0;
            padding: 0;
        }
        .container {
            width: 65%;
            margin: 40px auto;
            background: #f8f9fa;
            border-radius: 12px;
            box-shadow: 0 2px 12px rgba(0,0,0,0.07);
            padding: 32px 40px;
        }
        h2 {
            color: #1565c0;
            text-align: center;
            margin-bottom: 30px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            background: #f4f6f8;
        }
        th, td {
            padding: 14px 10px;
            text-align: left;
        }
        th {
            background: #e3e7ed;
            color: #1565c0;
            font-weight: 600;
            border-bottom: 2px solid #b0b8c1;
        }
        tr {
            background: #f8f9fa;
            transition: background 0.2s;
        }
        tr:nth-child(even) {
            background: #e9ecef;
        }
        tr:hover {
            background: #d0e2ff;
        }
        td {
            color: #333;
            border-bottom: 1px solid #e0e0e0;
        }
    </style>
</head>
<body>
<%@ include file="sidebar.jsp" %>
<div class="container">
    <h2>Liste des Salary Slips</h2>
    <%
        JsonArray slips = (JsonArray) request.getAttribute("matchedSlips");
    %>
    <table>
        <thead>
        <tr>
            <th>Slip Name</th>
            <th>Employee</th>
            <th>Employee name</th>
            <th>Start Date</th>
            <th>End Date</th>
            <th>Net Pay</th>
            <th>Gross Pay</th>
        </tr>
        </thead>
        <tbody>
        <%
            if (slips != null) {
                for (JsonElement el : slips) {
                    JsonObject slip = el.getAsJsonObject();
                    String slipName = slip.has("name") ? slip.get("name").getAsString() : "";
                    String employee = slip.has("employee") ? slip.get("employee").getAsString() : "";
                    String employee_name = slip.has("employee_name") ? slip.get("employee_name").getAsString() : "";
                    String startDate = slip.has("start_date") ? slip.get("start_date").getAsString() : "";
                    String endDate = slip.has("end_date") ? slip.get("end_date").getAsString() : "";
                    double netPay = slip.has("net_pay") ? slip.get("net_pay").getAsDouble() : 0.0;
                    double grossPay = slip.has("gross_pay") ? slip.get("gross_pay").getAsDouble() : 0.0;
        %>
            <tr>
                <td><%= slipName %></td>
                <td><%= employee %></td>
                <td><%= employee_name %></td>
                <td><%= startDate %></td>
                <td><%= endDate %></td>
                <td><%= netPay %></td>
                <td><%= grossPay %></td>
            </tr>
        <%
                }
            }
        %>
        </tbody>
    </table>
</div>
</body>
</html>
