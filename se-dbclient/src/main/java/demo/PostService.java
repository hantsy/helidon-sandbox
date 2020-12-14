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
                .subscribe(
                        count -> {
                            LOGGER.log(Level.INFO, "{0} posts deleted.", count);
                            serverResponse.status(204).send();
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
                .map(EntityUtils::fromJsonObject)
                .flatMap(data -> this.posts.update(id, data))
                .subscribe(
                        p -> serverResponse.status(204).send(),
                        throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to updatePost", throwable);
                            serverRequest.next(throwable);
                        }
                );

    }


    private void savePost(ServerRequest serverRequest, ServerResponse serverResponse) {

        serverRequest.content().as(JsonObject.class)
                .map(EntityUtils::fromJsonObject)
                .map(p ->
                        Post.of(p.getTitle(), p.getContent())
                )
                .flatMap(this.posts::save)
                .subscribe(
                        p -> {
                            serverResponse.status(201)
                                    .headers()
                                    .location(URI.create("/posts/" + p));
                            serverResponse.send();
                        },
                        throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to savePost", throwable);
                            serverRequest.next(throwable);
                        }
                );
    }

    private void getPostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        this.posts.getById(id)
                .subscribe(
                        post -> serverResponse.status(200).send(EntityUtils.toJsonObject(post)),
                        throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to getPostById", throwable);
                            serverRequest.next(throwable);
                        }
                );
    }

    private void getAllPosts(ServerRequest serverRequest, ServerResponse serverResponse) {
        this.posts.all()
                .collectList()
                .map(EntityUtils::toJsonArray)
                .subscribe(
                        serverResponse::send,
                        throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to getAllPosts", throwable);
                            serverRequest.next(throwable);
                        }
                );
    }

}
