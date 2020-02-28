package demo;

import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRows;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

public class PostRepository {

    private DbClient dbClient;

    public PostRepository(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public CompletionStage<List<Post>> all() {
        return this.dbClient
                .execute(dbExecute -> dbExecute.createQuery("SELECT * FROM posts")
                        .execute()
                )
                .thenCompose(dbRowDbRows -> dbRowDbRows.map(Post.class).collect());
    }
/*
    public CompletionStage<Post> getById(UUID id) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute.createQuery("SELECT * FROM posts WHERE id=?")
                                .addParam(id)
                                .execute()
                )
                .thenCompose(dbRowDbRows -> dbRowDbRows.map(Post.class).collect())
                .thenApply(data -> {
                    if (data.isEmpty()) {
                        throw new PostNotFoundException(id);
                    }
                    return data.get(0);
                });
    }
    */

    public CompletionStage<Post> getById(UUID id) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute.createGet("SELECT * FROM posts WHERE id=?")
                                .addParam(id)
                                .execute()
                )
                .thenApply(
                        rowOptional -> rowOptional.map(dbRow -> dbRow.as(Post.class)).orElseThrow(() -> new PostNotFoundException(id))
                );
    }

    public CompletionStage<Long> update(UUID id, Post post) {
        return this.dbClient
                .inTransaction(
                        tx -> tx.createGet("SELECT * FROM posts WHERE id=? FOR UPDATE")
                                .addParam(id)
                                .execute()
                                .thenApply(
                                        rowOptional -> rowOptional.map(dbRow -> dbRow.as(Post.class)).orElseThrow(() -> new PostNotFoundException(id))
                                )
                                .thenApply(p ->
                                        Map.of("title", post.getTitle(), "content", post.getContent(), "id", id)
                                )
                                .thenCompose(
                                        map -> tx.createUpdate("UPDATE posts SET title=:title, content=:content WHERE id=:id")
                                                .params(map)
                                                .execute()
                                )

                );
    }

    public CompletionStage<UUID> save(Post post) {
        return this.dbClient
                .execute(
                        dbExecute -> dbExecute
                                .query("INSERT INTO posts(title, content) VALUES (?, ?) RETURNING id", post.getTitle(), post.getContent())

                )
                .thenCompose(DbRows::collect)
                .thenApply(data -> data.isEmpty() ? null : data.get(0).column("id").as(UUID.class));
    }

    public CompletionStage<Long> deleteById(UUID id) {
        return this.dbClient.execute(
                dbExecute -> dbExecute.createDelete("DELETE FROM posts WHERE id = :id")
                        .addParam("id", id)
                        .execute()
        );
    }
}
