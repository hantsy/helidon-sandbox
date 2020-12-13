package demo;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;

import java.util.Map;
import java.util.UUID;

public class PostRepository {

    private DbClient dbClient;

    public PostRepository(DbClient dbClient) {
        this.dbClient = dbClient;
    }

    public Multi<Post> all() {
        return this.dbClient
                .execute(exec -> exec
                        .createQuery("SELECT * FROM posts")
                        .execute()
                )
                .map(dbRow -> dbRow.as(Post.class));
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

    public Single<Post> getById(UUID id) {
        return this.dbClient
                .execute(exec -> exec
                        .createGet("SELECT * FROM posts WHERE id=?")
                        .addParam(id)
                        .execute()
                )
                .map(rowOptional -> rowOptional
                        .map(dbRow -> dbRow.as(Post.class)).orElseThrow(() -> new PostNotFoundException(id))
                );
    }

    public Single<Long> update(UUID id, Post post) {
        return this.dbClient
                .execute(exec -> exec
                        .createUpdate("UPDATE posts SET title=:title, content=:content WHERE id=:id")
                        .params(Map.of("title", post.getTitle(), "content", post.getContent(), "id", post.getId()))
                        .execute()
                );
    }

    public Single<UUID> save(Post post) {
        return this.dbClient
                .execute(exec -> exec
                        .query("INSERT INTO posts(title, content) VALUES (?, ?) RETURNING id", post.getTitle(), post.getContent())

                )
                .first()
                .map(data -> data.column("id").as(UUID.class));
    }

    public Single<Long> deleteById(UUID id) {
        return this.dbClient.execute(exec -> exec
                .createDelete("DELETE FROM posts WHERE id = :id")
                .addParam("id", id)
                .execute()
        );
    }
}
