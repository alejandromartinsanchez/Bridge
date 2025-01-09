package me.zurdo.music;

import io.javalin.http.Context;
import io.javalin.http.UnauthorizedResponse;
import me.zurdo.Database;
import me.zurdo.Utils;
import me.zurdo.auth.Jwt;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class SongApi {

    public static void handleSecurity(Context ctx) {
        String token = Utils.getTokenFromHeader(ctx);

        long id = Jwt.validateToken(token);


        boolean isArtist = Database.getJdbi().withHandle(handle ->
                handle.createQuery("SELECT id FROM users WHERE id = :id AND role = ARTIST")
                        .bind("id", id)
                        .mapTo(Long.class)
                        .findOne().isPresent()
        );


        if (!isArtist) {
            throw new UnauthorizedResponse("User is not an admin");
        }
    }

    /**
     * HTTP POST Request to /api/songs
     * Creates a song.
     * <p>
     * JSON Body Parameters:
     * - name: The name of the song.
     * - artist: The ID of the artist.
     * - album: The ID of the album.
     * - duration: The duration of the song in seconds.
     * - link: The YouTube link of the song.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void createSong(Context ctx) {
        handleSecurity(ctx);
        JSONObject body = new JSONObject(ctx.body());

        String name = body.getString("name");
        String lyrics = body.getString("lyrics");
        long artist = body.getLong("artist");
        long album = body.getLong("album");
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
     * HTTP GET Request to /api/songs
     * Retrieves a list of all songs.
     *
     * @param ctx the Javalin HTTP context
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
     * HTTP GET Request to /api/songs/{id}
     * Retrieves a song by its ID.
     * <p>
     * Path Parameters:
     * - id: The ID of the song to retrieve.
     *
     * @param ctx the Javalin HTTP context
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
     * HTTP GET Request to /api/songs
     * Retrieves a list of all songs.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void getSongsFromUser(Context ctx) {
        handleSecurity(ctx);
        System.out.println("Getting songs");
        List<Song> songs = Database.getJdbi().withHandle(handle -> handle.createQuery("""
                        SELECT id, name, lyrics, artist, album, link
                        FROM songs
                        """)
                .mapTo(Song.class)
                .list());

        ctx.json(songs);
    }



    /**
     * HTTP DELETE Request to /api/songs/{id}
     * Deletes a song by its ID.
     *
     * Query Parameters:
     * - id: The ID of the song to delete.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void deleteSong(Context ctx) {
        handleSecurity(ctx);
        long id = Utils.getIdFromPath(ctx);

        int deleted = Database.getJdbi().withHandle(handle ->
                handle.createUpdate("DELETE FROM songs WHERE id = :id")
                        .bind("id", id)
                        .execute()
        );

        if (deleted == 0) {
            ctx.status(404).result("Song not found");
        } else {
            ctx.status(204);
        }
    }

    /**
     * HTTP PATCH Request to /api/songs/{id}
     * Updates a song by its ID.
     *
     * Query Parameters:
     * - id: The ID of the song to update.
     * - name (optional): The new name of the song.
     * - lyrics (optional): The new lyrics of the song.
     * - artist (optional): The new artist of the song.
     * - album (optional): The new album of the song.
     * - duration (optional): The new duration of the song.
     * - link (optional): The new YouTube link of the song.
     *
     * @param ctx the Javalin HTTP context
     */
    public static void updateSong(Context ctx) {
        handleSecurity(ctx);
        long id = Utils.getIdFromPath(ctx);
        JSONObject body = new JSONObject(ctx.body());

        String name = body.optString("name", null);
        String lyrics = body.optString("lyrics", null);
        Long artist = body.has("artist") ? body.getLong("artist") : null;
        Long album = body.has("album") ? body.getLong("album") : null;
        String link = body.optString("link", null);

        StringBuilder sql = new StringBuilder("UPDATE songs SET ");
        boolean first = true;

        if (name != null) {
            sql.append("name = :name");
            first = false;
        }
        if (lyrics != null) {
            if (!first) sql.append(", ");
            sql.append("lyrics = :lyrics");
            first = false;
        }
        if (artist != null) {
            if (!first) sql.append(", ");
            sql.append("artist = :artist");
            first = false;
        }
        if (album != null) {
            if (!first) sql.append(", ");
            sql.append("album = :album");
            first = false;
        }
        if (link != null) {
            if (!first) sql.append(", ");
            sql.append("link = :link");
        }
        sql.append(" WHERE id = :id");

        Song song = Database.getJdbi().withHandle(handle -> {
            var update = handle.createUpdate(sql.toString()).bind("id", id);
            if (name != null) update.bind("name", name);
            if (lyrics != null) update.bind("lyrics", lyrics);
            if (artist != null) update.bind("artist", artist);
            if (album != null) update.bind("album", album);
            if (link != null) update.bind("link", link);

            update.execute();
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
