package demo;

import io.helidon.webserver.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CommentService implements Service {
    private final static Logger LOGGER = Logger.getLogger(CommentService.class.getName());
    private final CommentRepository comments;

    public CommentService(CommentRepository commentRepository) {
        this.comments = commentRepository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllComments)
                .post("/", Handler.create(JsonObject.class, this::saveComment, this::errorHandler));
    }

    private void errorHandler(ServerRequest serverRequest, ServerResponse serverResponse, Throwable throwable) {
        if (throwable instanceof CommentBodyCanNotBeEmptyException) {
            serverResponse.status(400).send();
        } else {
            serverRequest.next(throwable);
        }
    }

    private void getAllComments(ServerRequest serverRequest, ServerResponse serverResponse) {
        UUID postId = extractPostIdFromPathParams(serverRequest);
        LOGGER.info("comments of post id::" + postId);
        this.comments.allByPostId(postId)
                .thenApply(this::toJsonArray)
                .thenCompose(data -> serverResponse.send(data))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to getAllComments", throwable);
                    serverRequest.next(throwable);
                    return null;
                });

    }

    private void saveComment(ServerRequest serverRequest, ServerResponse serverResponse, JsonObject content) {
        UUID postId = extractPostIdFromPathParams(serverRequest);
        String body = content.get("content") == null ? null : content.getString("content");

        if (body == null) {
            serverRequest.next(new CommentBodyCanNotBeEmptyException());
        }

        CompletableFuture.completedFuture(content)
                .thenApply(c -> Comment.of(postId, body))
                .thenCompose(this.comments::save)
                .thenCompose(
                        id -> {
                            serverResponse.status(201)
                                    .headers()
                                    .location(URI.create("/posts/" + postId + "/comments/" + id));
                            return serverResponse.send();
                        }
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to saveComment", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

    private UUID extractPostIdFromPathParams(ServerRequest serverRequest) {
        try {
            return UUID.fromString(serverRequest.path().absolute().param("id"));
        } catch (Exception e) {
            serverRequest.next(e);
        }
        return null;
    }

    private JsonObject toJsonObject(Comment comment) {
        return Json.createObjectBuilder()
                .add("id", comment.getId().toString())
                .add("post", comment.getPost().toString())
                .add("content", comment.getContent())
                .add("createdAt", comment.getCreatedAt().toString())
                .build();
    }

    private JsonArray toJsonArray(List<Comment> comments) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        comments.forEach(p -> {
            jsonArrayBuilder.add(toJsonObject(p));
        });

        return jsonArrayBuilder.build();
    }


}
