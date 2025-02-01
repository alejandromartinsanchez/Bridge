package me.zurdo.auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Config;
import java.time.Duration;
import java.util.Date;

public class Jwt {

    public static final Algorithm algorithm = Algorithm.HMAC256(Config.jwtSecret);
    private static final long EXPIRATION = Duration.ofDays(30).toMillis();

    /**
     * Genera un token JWT con una ID de usuario y una fecha de expiración
     * @param id ID del usuario que se incluirá en el token
     * @return El token JWT firmado como cadena
     */
    public static String generateToken(long id) {
        JWTCreator.Builder token = JWT.create()
                .withClaim("id", id)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION));

        return token.sign(algorithm);
    }


    /**
     * Valida un token JWT recibido
     * @param token Token JWT como cadena
     */
    public static long validateToken(String token) {
        if (token == null) return -1;
        try {
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT jwt = verifier.verify(token);

            if (!jwt.getClaims().containsKey("id")) {
                return -1;
            }

            if (jwt.getExpiresAt().before(new Date())) {
                throw new UnauthorizedResponse("Token has expired");
            }

            return jwt.getClaim("id").asLong();
        } catch (JWTVerificationException e) {
            throw new UnauthorizedResponse("Invalid token");
        }
    }

}
