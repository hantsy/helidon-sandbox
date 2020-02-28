package demo;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class EntityUtils {

    public static JsonObject toJsonObject(Post post) {
        return Json.createObjectBuilder()
            .add("id", post.getId().toString())
            .add("title", post.getTitle())
            .add("content", post.getContent())
            .add("createdAt", post.getCreatedAt().toString())
            .build();
    }

    public static JsonArray toJsonArray(List<Post> posts) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        posts.forEach(p -> {
            jsonArrayBuilder.add(toJsonObject(p));
        });

        return jsonArrayBuilder.build();
    }



    public static Post fromJsonObject(JsonObject json) {
        Post post = new Post();
        post.setId(json.get("id") == null ? null : UUID.fromString(json.getString("id")));
        post.setTitle(json.get("title") == null ? null : json.getString("title"));
        post.setContent(json.get("content") == null ? null : json.getString("content"));
        post.setCreatedAt(json.get("createdAt") == null ? null : LocalDateTime.parse(json.getString("createdAt")));

        return post;
    }
}
