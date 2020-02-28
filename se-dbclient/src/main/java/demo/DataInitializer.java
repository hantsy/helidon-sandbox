package demo;

import io.helidon.dbclient.DbClient;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataInitializer {
    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    public static void init(DbClient dbClient) {
        dbClient.inTransaction(
                tx -> tx.createDelete("DELETE FROM comments").execute()
                        .thenAccept(
                                count -> LOGGER.log(Level.INFO, "{0} comments deleted.", count)
                        )
                        .thenCompose(
                                v -> tx.createDelete("DELETE FROM posts").execute()
                                        .thenAccept(count2 -> LOGGER.log(Level.INFO, "{0} posts deleted.", count2))
                        )
                        .thenCompose(
                                v2 -> tx.createInsert("INSERT INTO posts(title, content) VALUES(?, ?), (?, ?)")
                                        .params(List.of("My first post of Helidon", "The content of my first post", "My second post of Helidon", "The content of my second post"))
                                        .execute()
                                        .thenAccept(count3 -> LOGGER.log(Level.INFO, "{0} posts inserted.", count3))
                        )
                        .thenCompose(
                                v2 -> tx.createQuery("SELECT * FROM posts")
                                        .execute()
                                        .thenCompose(dbRowDbRows -> dbRowDbRows.collect())
                                        .thenAccept(rows -> LOGGER.log(Level.INFO, "found posts: {0}.", rows))
                        )

                        .exceptionally(throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to initialize data", throwable);
                            return null;
                        })
        );
    }

    private DataInitializer() {
    }
}
