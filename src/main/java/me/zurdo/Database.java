package me.zurdo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.zurdo.auth.User;
import me.zurdo.music.Song;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.jackson2.Jackson2Plugin;

import java.sql.Timestamp;
import java.util.Date;


public class Database {
    private static final Jdbi JDBI;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(Config.dbUser);
        hikariConfig.setPassword(Config.dbPassword);
        hikariConfig.setJdbcUrl(Config.dbUrl);

        // Tablas
        JDBI = Jdbi.create(new HikariDataSource(hikariConfig));
        JDBI.registerRowMapper(ConstructorMapper.factory(User.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Song.class));

        // Register custom column mapper for java.util.Date
        JDBI.getConfig(ColumnMappers.class).register(Date.class, (rs, columnNumber, ctx) -> {
            Timestamp timestamp = rs.getTimestamp(columnNumber);
            return timestamp != null ? new Date(timestamp.getTime()) : null;
        });


        JDBI.installPlugin(new Jackson2Plugin());
    }

    public static Jdbi getJdbi() {
        return JDBI;
    }
}