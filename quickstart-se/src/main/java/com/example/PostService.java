package com.example;

import io.helidon.webserver.Routing;
import io.helidon.webserver.ServerRequest;
import io.helidon.webserver.ServerResponse;
import io.helidon.webserver.Service;

import javax.json.JsonObject;
import java.net.URI;
import java.util.logging.Logger;

public class PostService implements Service {
    private final static Logger LOGGER = Logger.getLogger(PostService.class.getName());

    private final PostRepository posts;

    public PostService(PostRepository posts) {
        this.posts = posts;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllPosts)
            .post("/", this::savePost)
            .get("/{id}", this::getPostById)
            .put("/{id}", this::updatePost)
            .delete("/{id}", this::deletePostById)
            .register("/{id}/comments", new CommentService(new CommentRepository()));
    }

    private void deletePostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        this.posts.deleteById(serverRequest.path().param("id"));
        serverResponse.status(204).send();
    }

    private void updatePost(ServerRequest serverRequest, ServerResponse serverResponse) {
        Post post = this.posts.getById(serverRequest.path().param("id"));
        serverRequest.content().as(JsonObject.class)
            .thenApply(EntityUtils::fromJsonObject)
            .thenApply(p -> {
                post.setTitle(p.getTitle());
                post.setContent(p.getContent());
                return post;
            })
            .thenApply(this.posts::save)
            .thenCompose(
                p -> serverResponse.status(204).send()
            );
    }


    private void savePost(ServerRequest serverRequest, ServerResponse serverResponse) {

        serverRequest.content().as(JsonObject.class)
            .thenApply(EntityUtils::fromJsonObject)
            .thenApply(p ->
                Post.of(p.getTitle(), p.getContent())
            )
            .thenApply(this.posts::save)
            .thenCompose(
                p -> {
                    serverResponse.status(201)
                        .headers()
                        .location(URI.create("/posts/" + p.getId()));
                    return serverResponse.send();
                }
            );
    }

    private void getPostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        String id = serverRequest.path().param("id");
        Post post = this.posts.getById(id);
        if (post == null) {
            serverRequest.next(new PostNotFoundException(id));
        }
        serverResponse.status(200).send(EntityUtils.toJsonObject(post));
    }

    private void getAllPosts(ServerRequest serverRequest, ServerResponse serverResponse) {
        serverResponse.send(EntityUtils.toJsonArray(this.posts.all()));
    }

}
