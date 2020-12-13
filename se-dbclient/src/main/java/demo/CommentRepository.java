package demo;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;

import java.util.UUID;
import java.util.logging.Logger;

public class CommentRepository {
    private static final Logger LOGGER = Logger.getLogger(CommentRepository.class.getName());

    private final DbClient dbClient;

    public CommentRepository(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Single<UUID> save(Comment comment) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute
                                .query("INSERT INTO comments(post_id, content) VALUES (?, ?) RETURNING id", comment.getPost(), comment.getContent())

                )
                .first().map(data -> data.column("id").as(UUID.class));
    }

    public Multi<Comment> allByPostId(UUID id) {
        return this.dbClient
                .execute(exec -> exec
                        .createQuery("SELECT * FROM comments WHERE post_id=?")
                        .addParam(id)
                        .execute()
                )
                .map(dbRow -> dbRow.as(Comment.class));
    }
}
