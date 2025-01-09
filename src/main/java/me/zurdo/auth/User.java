package me.zurdo.auth;

import java.util.Date;

public record User(long id, String username, String email, Role role, Date creation) {
    enum Role {
        LISTENER,
        ARTIST
    }
}
