package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;

@WebServlet(name = "AuthController", value = "/Auth-Controller")
public class AuthController extends HttpServlet {
    
    private static final String LOGIN_API_URL = "http://172.25.36.0:8000/api/method/login";

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String username = req.getParameter("usr");
        String password = req.getParameter("pwd");

        // Basic validation
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            resp.sendRedirect("login.jsp?error=" + URLEncoder.encode("Username and password are required.", StandardCharsets.UTF_8));
            return; // Stop processing
        }

        try {
            // Préparation de la requête
            URL url = new URL(LOGIN_API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            // Construction du corps de la requête
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("usr", username);
            requestBody.addProperty("pwd", password);

            // Envoi de la requête
            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);           
            }

            // Récupération du code de réponse
            int responseCode = conn.getResponseCode();
            
            // Si le code de réponse est 2xx (succès)
            if (responseCode >= 200 && responseCode < 300) {
                // Récupérer le cookie de session (SID) de la réponse
                String sid = null;
                String headerName = null;
                for (int i = 1; (headerName = conn.getHeaderFieldKey(i)) != null; i++) {
                    if (headerName.equals("Set-Cookie")) {
                        String cookie = conn.getHeaderField(i);
                        if (cookie.startsWith("sid=")) {
                            sid = cookie.substring(4, cookie.indexOf(";"));
                            System.out.println(sid);
                            break;
                        }
                    }
                }

                // Stocker le SID et le nom d'utilisateur dans la sessi on
                req.getSession().setAttribute("sid", sid);
                req.getSession().setAttribute("loggedInUser", username);
                resp.sendRedirect("dashboard.jsp");

            } else {
                // Cas d'erreur (code HTTP non-2xx)
                // Tenter de lire le corps de la réponse pour un message d'erreur
                String errorMessage = "API error with status " + responseCode;
                InputStream errorStream = conn.getErrorStream(); // Lire le flux d'erreur

                if (errorStream != null) {
                     try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        // Tenter de parser le corps pour un message d'erreur JSON
                        if (response.length() > 0) {
                             try {
                                 JsonElement jsonElement = new JsonParser().parse(response.toString());
                                 if (jsonElement != null && jsonElement.isJsonObject()) {
                                     JsonObject jsonResponse = jsonElement.getAsJsonObject();
                                     JsonElement messageElement = jsonResponse.get("message");
                                     if (messageElement != null && messageElement.isJsonPrimitive()) {
                                         errorMessage = messageElement.getAsString();
                                     } else {
                                         errorMessage += ". Unexpected error response format.";
                                     }
                                 } else {
                                     errorMessage += ". Response is not a JSON object.";
                                 }
                             } catch (Exception jsonParsingException) {
                                 // Handle JSON parsing errors if the body is not valid JSON
                                 jsonParsingException.printStackTrace(); // Log the parsing error
                                 errorMessage += ". Failed to parse error response body.";
                             }
                        } else {
                             errorMessage += ". Empty response body.";
                        }
                    } catch (IOException readError) {
                         readError.printStackTrace(); // Log error reading stream
                         errorMessage += ". Failed to read error response body.";
                    }
                } else {
                    errorMessage += ". No error stream available.";
                }
                
                resp.sendRedirect("login.jsp?error=" + URLEncoder.encode(errorMessage, StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            // Handle network or other exceptions (e.g., connection refused, malformed URL)
            e.printStackTrace();
            resp.sendRedirect("login.jsp?error=" + URLEncoder.encode("Server error during login attempt: " + e.getMessage(), StandardCharsets.UTF_8));
        } finally {
             // Ensure connection is closed
             // Note: HttpURLConnection is usually closed when streams are closed,
             // but explicit disconnect can be added if needed, though often not necessary with try-with-resources on streams.
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // Rediriger vers la page de login si tentative d'accès direct
        resp.sendRedirect("login.jsp");
    }
}