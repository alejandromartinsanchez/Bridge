package me.zurdo.music;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Database;
import me.zurdo.Utils;
import me.zurdo.auth.Jwt;
import org.json.JSONObject;
import java.util.List;

public class AlbumApi {

    /**
     * Endpoint para manejar la seguridad de las operaciones de álbumes
     * Verifica si el token JWT es válido y si el usuario tiene el rol de 'ARTIST'
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void handleSecurity(Context ctx) {
        String token = Utils.getTokenFromHeader(ctx);
        long id = Jwt.validateToken(token);

        boolean isArtist = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT id FROM users WHERE id = :id AND role = 'ARTIST'")
                        .bind("id", id)
                        .mapTo(Long.class)
                        .findOne().isPresent()
        );

        if (!isArtist) {
            throw new UnauthorizedResponse("User is not an artist");
        }
    }

    /**
     * Endpoint para obtener o crear un álbum
     * Si el álbum ya existe, lo devuelve; de lo contrario, lo crea y devuelve
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getOrCreateAlbum(Context ctx) {
        handleSecurity(ctx);
        JSONObject body = new JSONObject(ctx.body());

        String name = body.getString("name");
        long artist = body.getLong("artist");
        int year = body.getInt("year");

        long id = Database.getJdbi().withHandle(handle ->
                handle.createQuery("""
                                SELECT id
                                FROM albums
                                WHERE name = :name AND artist = :artist AND year = :year
                                """)
                        .bind("name", name)
                        .bind("artist", artist)
                        .bind("year", year)
                        .mapTo(Long.class)
                        .findOne()
                        .orElse(-1L)
        );

        if (id != -1) {
            Album album = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("""
                                    SELECT id, name, artist, year
                                    FROM albums
                                    WHERE id = :id
                                    """)
                            .bind("id", id)
                            .mapTo(Album.class)
                            .findOne()
                            .orElse(null)
            );
            ctx.json(album);
            return;
        }

        Album album = Database.getJdbi().withHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO albums (name, artist, year)
                                VALUES (:name, :artist, :year)
                                """)
                        .bind("name", name)
                        .bind("artist", artist)
                        .bind("year", year)
                        .executeAndReturnGeneratedKeys("id", "name", "artist", "year")
                        .mapTo(Album.class)
                        .one()
        );
        ctx.json(album);
    }

    /**
     * Endpoint para obtener la lista de todos los álbumes
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getAlbums(Context ctx) {
        List<Album> albums = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, artist, year
                        FROM albums
                        """)
                .mapTo(Album.class)
                .list());

        ctx.json(albums);
    }

    /**
     * Endpoint para obtener un álbum específico por su ID
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getAlbum(Context ctx) {
        long id = Utils.getIdFromPath(ctx);

        Album album = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, artist, year
                        FROM albums
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(Album.class)
                .findOne()
                .orElse(null)
        );

        if (album == null) {
            ctx.status(404).result("Album not found");
        } else {
            ctx.json(album);
        }
    }


    /**
     * Endpoint para obtener las canciones de un álbum
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getSongs(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));

        List<Song> songs = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                SELECT id, name, lyrics, artist, album, link
                FROM songs
                WHERE album = :id
                """)
                .bind("id", id)
                .mapTo(Song.class)
                .list());
        ctx.json(songs);
    }
}
