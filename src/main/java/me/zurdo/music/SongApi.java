package me.zurdo.music;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Database;
import me.zurdo.Utils;
import me.zurdo.auth.Jwt;
import me.zurdo.auth.User;
import org.json.JSONObject;
import java.io.IOException;
import java.util.List;

public class SongApi {

    /**
     * Endpoint para manejar la seguridad de las operaciones de canciones
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
            throw new UnauthorizedResponse("User is not an admin");
        }
    }

    /**
     * Endpoint para crear una nueva canción
     * Verifica que el usuario tenga los permisos necesarios y luego inserta los datos de la canción en la base de datos
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void createSong(Context ctx) {
        handleSecurity(ctx);
        JSONObject body = new JSONObject(ctx.body());

        String name = body.getString("name");
        String lyrics = body.getString("lyrics");
        long artist = body.getLong("artist");
        // Manejar "album" como nulo si no está presente
        Long album = body.has("album") && !body.isNull("album") ? body.getLong("album") : null;
        String link = body.getString("link");

        Song song = Database.getJdbi().withHandle(handle ->
                handle.createUpdate("""
                                INSERT INTO songs (name, lyrics, artist, album, link)
                                VALUES (:name, :lyrics, :artist, :album, :link)
                                """)
                        .bind("name", name)
                        .bind("lyrics", lyrics)
                        .bind("artist", artist)
                        .bind("album", album)
                        .bind("link", link)
                        .executeAndReturnGeneratedKeys("id", "name", "lyrics", "artist", "album", "link")
                        .mapTo(Song.class)
                        .one()
        );

        ctx.json(song);
    }


    /**
     * Endpoint para obtener todas las canciones almacenadas
     * Recupera y devuelve la lista completa de canciones en formato JSON
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getSongs(Context ctx) {
        List<Song> songs = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, lyrics, artist, album, link
                        FROM songs
                        """)
                .mapTo(Song.class)
                .list());

        ctx.json(songs);
    }

    /**
     * Endpoint para obtener los detalles de una canción específica por su ID
     * Devuelve un error 404 si la canción no se encuentra
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getSong(Context ctx) {
        long id = Utils.getIdFromPath(ctx);

        Song song = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, lyrics, artist, album, link
                        FROM songs
                        WHERE id = :id
                        """)
                .bind("id", id)
                .mapTo(Song.class)
                .findOne()
                .orElse(null)
        );

        if (song == null) {
            ctx.status(404).result("Song not found");
        } else {
            ctx.json(song);
        }
    }

    /**
     * Endpoint para obtener las canciones asociadas a un usuario específico
     * Recupera todas las canciones creadas por el usuario autenticado
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void getSongsFromUser(Context ctx) {
        User user = Utils.getUserfromToken(ctx);
        List<Song> songs = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, lyrics, artist, album, link
                        FROM songs
                        WHERE artist = :id
                        """)
                .bind("id", user.id())
                .mapTo(Song.class)
                .list());

        ctx.json(songs);
    }

    /**
     * Endpoint para eliminar canciones específicas
     * Verifica que el usuario sea el propietario de las canciones antes de eliminarlas
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void deleteSongs(Context ctx) {
        User user = Utils.getUserfromToken(ctx);

        ObjectMapper objectMapper = new ObjectMapper();
        List<Long> ids;
        try {
            ids = objectMapper.readValue(ctx.body(), new TypeReference<List<Long>>() {});
        } catch (IOException e) {
            ctx.status(400).result("Invalid request body");
            return;
        }

        if (ids == null || ids.isEmpty()) {
            ctx.status(400).result("No song IDs provided");
            return;
        }

        for (Long id : ids) {
            boolean isOwner = Database.getJdbi().withHandle(handle ->
                    handle.createQuery("SELECT COUNT(*) FROM songs WHERE id = :id AND artist = :userId")
                            .bind("id", id)
                            .bind("userId", user.id())
                            .mapTo(Long.class)
                            .one() > 0
            );

            if (!isOwner) {
                ctx.status(403).result("User is not the owner of one or more songs");
                return;
            }
        }

        int deleted = Database.getJdbi().withHandle(handle ->
                handle.createUpdate("DELETE FROM songs WHERE id IN (<ids>)")
                        .bindList("ids", ids)
                        .execute()
        );

        if (deleted == 0) {
            ctx.status(404).result("No songs found to delete");
        } else {
            ctx.status(204);
        }
    }

    /**
     * Endpoint para actualizar los datos de una canción específica
     * Verifica que el usuario sea el propietario antes de realizar la actualización
     * @param ctx Contexto de la solicitud HTTP
     */
    public static void updateSong(Context ctx) {
        System.out.println("updateSong");
        User user = Utils.getUserfromToken(ctx);
        long id = Utils.getIdFromPath(ctx);

        boolean isOwner = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM songs WHERE id = :id AND artist = :userId")
                        .bind("id", id)
                        .bind("userId", user.id())
                        .mapTo(Long.class)
                        .one() > 0
        );

        if (!isOwner) {
            throw new UnauthorizedResponse("User is not the owner of the song");
        }

        JSONObject body = new JSONObject(ctx.body());
        String name = body.getString("name");
        String lyrics = body.getString("lyrics");
        Long artist = body.getLong("artist");
        Long album = body.isNull("album") || body.getLong("album") <= 0 ? null : body.getLong("album");
        String link = body.getString("link");

        String sql = "UPDATE songs SET name = :name, lyrics = :lyrics, artist = :artist, album = :album, link = :link WHERE id = :id";

        Song song = Database.getJdbi().withHandle(handle -> {
            handle.createUpdate(sql)
                    .bind("id", id)
                    .bind("name", name)
                    .bind("lyrics", lyrics)
                    .bind("artist", artist)
                    .bind("album", album)
                    .bind("link", link)
                    .execute();

            return handle.createQuery("""
                SELECT id, name, lyrics, artist, album, link
                FROM songs
                WHERE id = :id
                """)
                    .bind("id", id)
                    .mapTo(Song.class)
                    .one();
        });

        ctx.json(song);
    }
}
