package demo;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import javax.json.JsonObject;
import java.net.URI;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PostService implements Service {
    private final static Logger LOGGER = Logger.getLogger(PostService.class.getName());

    private final PostRepository posts;
    private final CommentRepository comments;

    public PostService(PostRepository posts, CommentRepository comments) {
        this.posts = posts;
        this.comments = comments;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllPosts)
                .post("/", this::savePost)
                .get("/{id}", this::getPostById)
                .put("/{id}", this::updatePost)
                .delete("/{id}", this::deletePostById)
                .register("/{id}/comments", new CommentService(comments));
    }

    private void deletePostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        this.posts.deleteById(id)
                .thenCompose(
                        count -> {
                            LOGGER.log(Level.INFO, "{0} posts deleted.", count);
                            return serverResponse.status(204).send();
                        }
                );

    }

    private UUID extractIdFromPathParams(ServerRequest serverRequest) {
        try {
            return UUID.fromString(serverRequest.path().param("id"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse `id` from request param...", e);
            serverRequest.next(e);
        }
        return null;
    }

    private void updatePost(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        serverRequest.content().as(JsonObject.class)
                .thenApply(EntityUtils::fromJsonObject)
                .thenCompose(data -> this.posts.update(id, data))
                .thenCompose(
                        p -> serverResponse.status(204).send()
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to updatePost", throwable);
                    serverRequest.next(throwable);
                    return null;
                });

    }


    private void savePost(ServerRequest serverRequest, ServerResponse serverResponse) {

        serverRequest.content().as(JsonObject.class)
                .thenApply(EntityUtils::fromJsonObject)
                .thenApply(p ->
                        Post.of(p.getTitle(), p.getContent())
                )
                .thenCompose(this.posts::save)
                .thenCompose(
                        p -> {
                            serverResponse.status(201)
                                    .headers()
                                    .location(URI.create("/posts/" + p));
                            return serverResponse.send();
                        }
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to savePost", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

    private void getPostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        this.posts.getById(id)
                .thenCompose(post -> serverResponse.status(200).send(EntityUtils.toJsonObject(post)))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to getPostById", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

    private void getAllPosts(ServerRequest serverRequest, ServerResponse serverResponse) {
        this.posts.all()
                .collectList()
                .map(EntityUtils::toJsonArray)
                .flatMap(serverResponse::send)
                .onError(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to getAllPosts", throwable);
                    serverRequest.next(throwable);
                });
    }

}
