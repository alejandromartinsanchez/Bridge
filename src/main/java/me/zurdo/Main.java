package me.zurdo;

import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import me.zurdo.auth.LoginApi;
import me.zurdo.music.SongApi;
import me.zurdo.music.AlbumApi;
import me.zurdo.stats.PlayApi;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.get;

public class Main {
    public static void main(String[] args) {
        try {
            ConfigReader.readFile(Path.of("").toAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Configurar el plugin SSL para conexiones seguras
        SslPlugin sslPlugin = new SslPlugin(sslConfig -> {
            sslConfig.http2 = true;
            sslConfig.secure = false;
            sslConfig.insecurePort = Config.apiPort;
        });

        // Crear la aplicación Javalin con configuración personalizada
        Javalin app = Javalin.create(config -> {
            // Registrar el plugin SSL.
            config.registerPlugin(sslPlugin);

            // Configurar las validaciones personalizadas
            config.validation.register(LocalDate.class, s -> {
                String[] split = s.split("-");
                int year = Integer.parseInt(split[0]);
                int month = Integer.parseInt(split[1]);
                int day = Integer.parseInt(split[2]);
                return LocalDate.of(year, month, day);
            });

            // Valida objetos JSON usando JSONObject.
            config.validation.register(JSONObject.class, JSONObject::new);

            // Permitir solicitudes desde localhost en diferentes puertos
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://localhost:5172", "http://localhost:5173", "http://localhost:5174");
                    it.allowCredentials = true;
                });
            });

            config.showJavalinBanner = false;

            config.router.apiBuilder(() -> {

                // Definir rutas de la API

                path("api", () -> {

                    path("auth", () -> {
                        post("register", LoginApi::register);
                        post("login", LoginApi::login);
                        get("validate", LoginApi::validateToken);
                        path("{id}", () -> {
                            get(LoginApi::getUsername);
                        });
                    });

                    path("user", () -> {
                        get("songs", SongApi::getSongsFromUser);
                    });

                    path("songs", () -> {
                        put(SongApi::createSong);
                        get(SongApi::getSongs);
                        delete(SongApi::deleteSongs);

                        path("album/{albumId}", () -> {
                        });

                        path("{id}", () -> {
                            get(SongApi::getSong);
                            patch(SongApi::updateSong);
                        });
                    });

                    path("albums", () -> {
                        put(AlbumApi::getOrCreateAlbum);
                        get(AlbumApi::getAlbums);

                        path("{id}", () -> {
                            get(AlbumApi::getAlbum);
                            get("songs", AlbumApi::getSongs);
                        });
                    });

                    path("stats", () -> {

                        path("play", () -> {
                            path("{song}", () -> {
                                put(PlayApi::increment);
                            });
                        });

                        path("user", () -> {
                            get(PlayApi::getMostPlayedSongFromUser);
                            get("album", PlayApi::getMostPlayedAlbumFromUser);
                            get("artist", PlayApi::getMostPlayedArtistFromUser);
                        });

                        path("global", () -> {
                            get(PlayApi::getMostPlayedSong);
                            get("album", PlayApi::getMostPlayedAlbum);
                            get("artist", PlayApi::getMostPlayedArtist);
                        });
                    });
                });
            });
        });
        app.start(Config.apiPort);
    }
}