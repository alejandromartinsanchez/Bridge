package me.zurdo;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.auth.Jwt;
import me.zurdo.auth.User;

public class Utils {

    /**
     * Obtiene el ID de la URL del contexto
     */
    public static long getIdFromPath(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid id format");
            return -1;
        }
    }

    /**
     * Obtiene el token de autenticación desde el encabezado de la solicitud
     * Si el token no está presente o no es válido, lanza una excepción de no autorizado
     */
    public static String getTokenFromHeader(Context ctx) {
        String authorization = ctx.header("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Invalid token");
        }
        return authorization.substring(7);
    }

    /**
     * Obtiene el usuario asociado al token JWT
     * Valida el token y, si es válido, recupera el usuario correspondiente desde la base de datos
     */
    public static User getUserfromToken(Context ctx) {
        String authorization = ctx.header("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedResponse("Invalid token");
        }
        String token = authorization.substring(7);
        long id = Jwt.validateToken(token);
        if (id == -1) {
            throw new UnauthorizedResponse("Invalid token");
        }
        return Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                                SELECT id, username, email, role, creation
                                FROM users WHERE id = :id
                                """)
                        .bind("id", id)
                        .mapTo(User.class)
                        .findOne()
                        .orElse(null)
        );
    }
}
