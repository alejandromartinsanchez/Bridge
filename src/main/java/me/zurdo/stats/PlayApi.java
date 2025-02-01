package me.zurdo.stats;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Database;
import me.zurdo.Utils;
import me.zurdo.auth.Jwt;
import me.zurdo.auth.User;
import me.zurdo.music.Album;
import me.zurdo.music.Song;

public class PlayApi {


    /**
     * Endpoint para manejar la seguridad de las operaciones de plays
     * Verifica si el token JWT es válido y si el usuario tiene el rol de 'ARTIST'
     * @param ctx Contexto de la solicitud HTTP
     */
    public static User handleSecurity(Context ctx) {
        String token = Utils.getTokenFromHeader(ctx);

        long id = Jwt.validateToken(token);


        User user = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT id, username, email, role, creation FROM users WHERE id = :id")
                        .bind("id", id)
                        .mapTo(User.class)
                        .findOne()
                        .orElse(null)
        );

        if (user == null) {
            throw new UnauthorizedResponse("User is not an admin");
        }
        return user;
    }

    /**
     * Registra una nueva reproducción en la base de datos
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void increment(Context ctx) {
        User listener = handleSecurity(ctx);
        long song = Long.parseLong(ctx.pathParam("song"));

        Database.getJdbi().useHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO plays (song, listener, date)
                                VALUES (:song, :listener, NOW())
                                """)
                        .bind("song", song)
                        .bind("listener", listener.id())
                        .execute()
        );
    }

    /**
     * Devuelve la canción más reproducida por el usuario autenticado
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedSongFromUser(Context ctx) {
        User user = handleSecurity(ctx);

        Song mostPlayedSong = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                SELECT s.id, s.name, s.lyrics, s.artist, s.album, s.link
                FROM plays p
                JOIN songs s ON p.song = s.id
                WHERE p.listener = :listener
                GROUP BY s.id
                ORDER BY COUNT(p.id) DESC
                LIMIT 1
                """)
                        .bind("listener", user.id())
                        .mapTo(Song.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedSong != null) {
            ctx.json(mostPlayedSong);
        } else {
            ctx.status(404).result("No se encontraron canciones.");
        }
    }

    /**
     * Devuelve el álbum más reproducido por el usuario autenticado
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedAlbumFromUser(Context ctx) {
        User user = handleSecurity(ctx);

        Album mostPlayedAlbum = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
            SELECT a.id, a.name, a.artist, a.year
            FROM plays p
            JOIN songs s ON p.song = s.id
            JOIN albums a ON s.album = a.id
            WHERE p.listener = :listener
            GROUP BY a.id, a.name, a.artist, a.year
            ORDER BY COUNT(s.id) DESC
            LIMIT 1
            """)
                        .bind("listener", user.id())
                        .mapTo(Album.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedAlbum != null) {
            ctx.json(mostPlayedAlbum);
        } else {
            ctx.status(404).result("No se encontraron álbumes.");
        }
    }

    /**
     * Devuelve el artista más reproducido por el usuario autenticado
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedArtistFromUser(Context ctx) {
        User user = handleSecurity(ctx);

        User mostPlayedArtist = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                SELECT u.id, u.username, u.email, u.role, u.creation
                FROM plays p
                JOIN songs s ON p.song = s.id
                JOIN users u ON s.artist = u.id
                WHERE p.listener = :listener
                GROUP BY u.id
                ORDER BY COUNT(p.id) DESC
                LIMIT 1
                """)
                        .bind("listener", user.id())
                        .mapTo(User.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedArtist != null) {
            ctx.json(mostPlayedArtist);
        } else {
            ctx.status(404).result("No se encontraron artistas más escuchados.");
        }
    }

    /**
     * Devuelve la canción más reproducida globalmente
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedSong(Context ctx) {
        handleSecurity(ctx);

        Song mostPlayedSong = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                    SELECT s.id, s.name, s.lyrics, s.artist, s.album, s.link
                    FROM plays p
                    JOIN songs s ON p.song = s.id
                    GROUP BY s.id, s.name, s.lyrics, s.artist, s.album, s.link
                    ORDER BY COUNT(p.song) DESC
                    LIMIT 1
                    """)
                        .mapTo(Song.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedSong != null) {
            ctx.json(mostPlayedSong);
        } else {
            ctx.status(404).result("No se encontraron canciones.");
        }
    }


    /**
     * Devuelve el álbum más reproducido globalmente
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedAlbum(Context ctx) {
        handleSecurity(ctx);

        Album mostPlayedAlbum = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                    SELECT a.id, a.name, a.artist, a.year
                    FROM plays p
                    JOIN songs s ON p.song = s.id
                    JOIN albums a ON s.album = a.id
                    WHERE s.album IS NOT NULL
                    GROUP BY a.id, a.name, a.artist, a.year
                    ORDER BY COUNT(p.song) DESC
                    LIMIT 1
                    """)
                        .mapTo(Album.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedAlbum != null) {
            ctx.json(mostPlayedAlbum);
        } else {
            ctx.status(404).result("No se encontraron álbumes.");
        }
    }

    /**
     * Devuelve el artista más reproducido globalmente
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getMostPlayedArtist(Context ctx) {
        handleSecurity(ctx);

        User mostPlayedArtist = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                    SELECT u.id, u.username, u.email, u.role, u.creation
                    FROM plays p
                    JOIN songs s ON p.song = s.id
                    JOIN users u ON s.artist = u.id
                    GROUP BY u.id, u.username, u.email, u.role, u.creation
                    ORDER BY COUNT(p.song) DESC
                    LIMIT 1
                    """)
                        .mapTo(User.class)
                        .findOne()
                        .orElse(null)
        );

        if (mostPlayedArtist != null) {
            ctx.json(mostPlayedArtist);
        } else {
            ctx.status(404).result("No se encontraron artistas.");
        }
    }
}
