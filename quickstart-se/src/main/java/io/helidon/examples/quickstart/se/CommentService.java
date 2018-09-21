package io.helidon.examples.quickstart.se;

import io.helidon.webserver.*;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.net.URI;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class CommentService implements Service{
    private final static Logger LOGGER = Logger.getLogger(CommentService.class.getName());
    private final CommentRepository comments;

    public CommentService(CommentRepository commentRepository) {
        this.comments = commentRepository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllComments)
            .post("/", Handler.of(JsonObject.class, this::saveComment, this::errorHandler));
    }

    private void errorHandler(ServerRequest serverRequest, ServerResponse serverResponse, Throwable throwable) {
        if (throwable instanceof CommentBodyCanNotBeEmptyException) {
            serverResponse.status(400).send();
        } else {
            serverRequest.next(throwable);
        }
    }

    private void getAllComments(ServerRequest serverRequest, ServerResponse serverResponse) {
        String postId = serverRequest.path().param("id");
        LOGGER.info("comments of post id::" + postId);
        serverResponse.send(this.toJsonArray(this.comments.allByPostId(postId)));
    }

    private void saveComment(ServerRequest serverRequest, ServerResponse serverResponse, JsonObject content) {

        String postId = serverRequest.path().param("id");
        String body = content.get("content") == null ? null : content.getString("content");

        if (body == null) {
            serverRequest.next(new CommentBodyCanNotBeEmptyException());
        }

        CompletableFuture.completedFuture(content)
            .thenApply(c -> Comment.of(postId, body))
            .thenApply(this.comments::save)
            .thenCompose(
                c -> {
                    serverResponse.status(201)
                        .headers()
                        .location(URI.create("/posts/" + postId + "/comments/" + c.getId()));
                    return serverResponse.send();
                }
            );
    }


    private JsonObject toJsonObject(Comment comment) {
        return Json.createObjectBuilder()
            .add("id", comment.getId())
            .add("post", comment.getPost())
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
