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
     * HTTP POST Request to /api/login
     * Logs in a user.
     *
     * JSON Body Parameters:
     * - username: The username of the user.
     * - password: The password of the user.
     *
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
     * HTTP POST Request to /api/register
     * Registers a new user.
     *
     * JSON Body Parameters:
     * - username: The username of the user.
     * - password: The password of the user.
     *
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
     * HTTP GET Request to /api/admin/validate
     * Validates the user's token and returns the user information.
     *
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
}
