package demo;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class PostMapper implements DbMapper<Post> {
    private final static Logger LOGGER = Logger.getLogger(PostMapper.class.getName());

    @Override
    public Post read(DbRow dbRow) {
        var id = dbRow.column("id");
        var title = dbRow.column("title");
        var content = dbRow.column("content");
        var createdAt = dbRow.column("created_at");
        Post post = new Post();
        post.setId(id.as(UUID.class));
        post.setTitle(title.as(String.class));
        post.setContent(content.as(String.class));
        post.setCreatedAt(createdAt.as(Timestamp.class).toLocalDateTime());

        return post;
    }

    @Override
    public Map<String, ?> toNamedParameters(Post post) {
        var map = Map.of(
                "title", post.getTitle(),
                "content", post.getContent()
        );
//        if (post.getId() != null) {
//            map.put("id", post.getId());
//        }
//
//        if (post.getCreatedAt() != null) {
//            map.put("createdAt", post.getCreatedAt());
//        }
        return map;
    }

    @Override
    public List<?> toIndexedParameters(Post post) {
        return List.of(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt()
        );
    }
}
