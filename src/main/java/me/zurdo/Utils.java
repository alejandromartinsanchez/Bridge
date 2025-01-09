package me.zurdo;

import io.javalin.http.Context;

public class Utils {
    public static long getIdFromParam(Context ctx) {
        try {
            return Long.parseLong(ctx.queryParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid id format");
            return -1;
        }
    }

    public static long getIdFromPath(Context ctx) {
        try {
            return Long.parseLong(ctx.pathParam("id"));
        } catch (NumberFormatException e) {
            ctx.status(400).result("Invalid id format");
            return -1;
        }
    }

    public static String getTokenFromHeader(Context ctx) {
        String authorization = ctx.header("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            ctx.status(401).result("Unauthorized");
            return null;
        }
        return authorization.substring(7);
    }
}
