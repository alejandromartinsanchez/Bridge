package me.zurdo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.zurdo.auth.User;
import me.zurdo.music.Album;
import me.zurdo.music.Song;
import me.zurdo.stats.Play;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.mapper.ColumnMappers;
import org.jdbi.v3.core.mapper.reflect.ConstructorMapper;
import org.jdbi.v3.core.statement.StatementContext;
import org.jdbi.v3.jackson2.Jackson2Plugin;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;


public class Database {
    private static final Jdbi JDBI;

    static {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setUsername(Config.dbUser);
        hikariConfig.setPassword(Config.dbPassword);
        hikariConfig.setJdbcUrl(Config.dbUrl);

        // Registrar los mapeadores de filas para las clases de entidades
        JDBI = Jdbi.create(new HikariDataSource(hikariConfig));
        JDBI.registerRowMapper(ConstructorMapper.factory(User.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Song.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Album.class));
        JDBI.registerRowMapper(ConstructorMapper.factory(Play.class));

        // Configurar el mapeo de la columna para tipos Date
        JDBI.getConfig(ColumnMappers.class).register(Date.class, (rs, columnNumber, ctx) -> {
            Timestamp timestamp = rs.getTimestamp(columnNumber);
            return timestamp != null ? new Date(timestamp.getTime()) : null;
        });

        // Configurar el mapeo de la columna para tipos List<Long>

        JDBI.registerColumnMapper(new ColumnMapper<List<Long>>() {
            @Override
            public List<Long> map(ResultSet rs, int columnNumber, StatementContext ctx) throws SQLException {
                String json = rs.getString(columnNumber);
                try {
                    return new ObjectMapper().readValue(json, new TypeReference<List<Long>>() {});
                } catch (Exception e) {
                    throw new SQLException("Failed to map JSON to List<Long>", e);
                }
            }
        });

        // Instalar el plugin de Jackson2 para permitir la conversión automática de objetos a JSON y viceversa
        JDBI.installPlugin(new Jackson2Plugin());
    }

    // Obtener la instancia de Jdbi
    public static Jdbi getJdbi() {
        return JDBI;
    }
}