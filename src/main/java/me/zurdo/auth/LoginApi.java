package me.zurdo.auth;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Database;
import me.zurdo.Utils;
import org.json.JSONObject;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashMap;
import java.util.Map;

public class LoginApi {

    /**
     * Endpoint para iniciar sesión de un usuario.
     * Valida las credenciales proporcionadas y genera un token JWT si son correctas.
     * @param ctx the Javalin HTTP context
     */
    public static void login(Context ctx) {
        JSONObject body = new JSONObject(ctx.body());

        String username = body.getString("username");
        String password = body.getString("password");

        String storedPasswordHash = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT password FROM users WHERE username = :username")
                        .bind("username", username)
                        .mapTo(String.class)
                        .findOne()
                        .orElse(null)
        );

        if (storedPasswordHash != null && BCrypt.checkpw(password, storedPasswordHash)) {
            long adminId = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT id FROM users WHERE username = :username")
                            .bind("username", username)
                            .mapTo(Long.class)
                            .findOne()
                            .orElse(-1L)
            );

            if (adminId == -1) {
                ctx.status(401).result("Invalid username or password.");
                return;
            }

            String token = Jwt.generateToken(adminId);
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            ctx.status(200).json(response);
        } else {
            ctx.status(401).result("Invalid username or password.");
        }
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     * Almacena las credenciales y otros datos del usuario en la base de datos.
     * @param ctx the Javalin HTTP context
     */
    public static void register(Context ctx) {
        JSONObject body = new JSONObject(ctx.body());

        String username = body.getString("username");
        String password = body.getString("password");
        String email = body.getString("email");
        String role = body.getString("role");


        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO users
                                (username, password, email, role, creation) VALUES
                                (:username, :password, :email, :role, NOW())
                                """)
                        .bind("username", username)
                        .bind("password", hashedPassword)
                        .bind("email", email)
                        .bind("role", role)
                        .execute()
        );

        ctx.status(201).result("User registered successfully.");
    }

    /**
     * Endpoint para validar el token JWT de un usuario.
     * Si el token es válido, devuelve los datos del usuario autenticado.
     * @param ctx the Javalin HTTP context
     */
    public static void validateToken(Context ctx) {
        String token = Utils.getTokenFromHeader(ctx);

        long id = Jwt.validateToken(token);

        if (id == -1) {
            throw new UnauthorizedResponse();
        }

        User user = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, username, email, role, creation
                        FROM users
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(User.class)
                .one());
        ctx.json(user);
    }

    /**
     * Endpoint para obtener el nombre de usuario basado en el ID proporcionado.
     * Devuelve el nombre de usuario correspondiente al ID recibido.
     * @param ctx the Javalin HTTP context
     */
    public static void getUsername(Context ctx) {
        long id = Utils.getIdFromPath(ctx);
        String username = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT username
                        FROM users
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(String.class)
                .one());
        ctx.json(username);
    }
}
