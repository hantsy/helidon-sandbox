package io.helidon.examples.quickstart.mp;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String id) {
        super("Post:" + id + " was not found!");
    }
}
