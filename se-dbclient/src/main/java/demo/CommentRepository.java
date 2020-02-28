package demo;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static java.util.stream.Collectors.toList;

public class CommentRepository {
    private static final Logger LOGGER = Logger.getLogger(CommentRepository.class.getName());

    private final DbClient dbClient;

    public CommentRepository(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public CompletionStage<UUID> save(Comment comment) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute
                                .query("INSERT INTO comments(post_id, content) VALUES (?, ?) RETURNING id", comment.getPost(), comment.getContent())

                )
                .thenCompose(DbRows::collect)
                .thenApply(data -> data.isEmpty() ? null : data.get(0).column("id").as(UUID.class));
    }

    public CompletionStage<List<Comment>> allByPostId(UUID id) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute.createQuery("SELECT * FROM comments WHERE post_id=?")
                                .addParam(id)
                                .execute()
                )
                .thenCompose(dbRowDbRows -> dbRowDbRows.map(Comment.class).collect());
    }
}
