package io.helidon.examples.quickstart.mp;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class PostRepository {

    static Map<String, Post> data = new ConcurrentHashMap<>();

    static {
        Post first = Post.of("Hello Helidon", "My first post of Helidon");
        Post second = Post.of("Hello Again, Helidon", "My second post of Helidon");
        data.put(first.getId(), first);
        data.put(second.getId(), second);
    }

    public List<Post> all() {
        return new ArrayList<>(data.values());
    }

    public Post getById(String id) {
        return data.get(id);
    }

    public Post save(Post post) {
        data.put(post.getId(), post);
        return post;
    }

    public void deleteById(String id) {
        data.remove(id);
    }
}
