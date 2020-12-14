package demo;

import io.helidon.dbclient.DbClient;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataInitializer {
    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    public static void init(DbClient dbClient) {
        LOGGER.info("Data initialization is starting...");
        dbClient
                .inTransaction(tx -> tx
                        .createDelete("DELETE FROM comments").execute()
                        .flatMap(v -> tx
                                .createDelete("DELETE FROM posts")
                                .execute()

                        )
                        .log()
                        .flatMap(v2 -> tx
                                .createInsert("INSERT INTO posts(title, content) VALUES(?, ?), (?, ?)")
                                .params(List.of("My first post of Helidon", "The content of my first post", "My second post of Helidon", "The content of my second post"))
                                .execute()
                        )
                        .log()
                        .flatMap(v3 -> tx
                                .createQuery("SELECT * FROM posts")
                                .execute()
                        )

                )
                .subscribe(
                        data -> LOGGER.log(Level.INFO, "data:{0}", data),
                        error -> LOGGER.warning("error: " + error),
                        () -> LOGGER.info("Data Initialization is done.")
                );
    }

    private DataInitializer() {
    }
}
