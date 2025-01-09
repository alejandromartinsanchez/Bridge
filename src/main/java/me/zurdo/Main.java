package me.zurdo;

import io.javalin.Javalin;
import io.javalin.community.ssl.SslPlugin;
import me.zurdo.auth.LoginApi;
import me.zurdo.music.SongApi;
import org.json.JSONObject;

import java.time.LocalDate;

import static io.javalin.apibuilder.ApiBuilder.*;
import static io.javalin.apibuilder.ApiBuilder.get;

public class Main {
    public static void main(String[] args) {
        SslPlugin sslPlugin = new SslPlugin(sslConfig -> {
            sslConfig.http2 = true;
            sslConfig.secure = false;
            sslConfig.insecurePort = Config.apiPort;
        });

        Javalin app = Javalin.create(config -> {
            config.registerPlugin(sslPlugin);

            config.validation.register(LocalDate.class, s -> {
                String[] split = s.split("-");
                int year = Integer.parseInt(split[0]);
                int month = Integer.parseInt(split[1]);
                int day = Integer.parseInt(split[2]);
                return LocalDate.of(year, month, day);
            });
            config.validation.register(JSONObject.class, JSONObject::new);

            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(it -> {
                    it.allowHost("http://localhost:5172", "http://localhost:5173", "http://localhost:5174");
                    it.allowCredentials = true;
                });
            });

            config.showJavalinBanner = false;

            config.router.apiBuilder(() -> {

                path("api", () -> {
                    post("register", LoginApi::register);
                    post("login", LoginApi::login);
                    get("validate", LoginApi::validateToken);

                    path("songs", () -> {
                        put(SongApi::createSong);
                        get(SongApi::getSongs);

                        path("{id}", () -> {
                            get(SongApi::getSong);
                            patch(SongApi::updateSong);
                            delete(SongApi::deleteSong);
                        });
                    });
                });
            });
        }).start();
    }
}