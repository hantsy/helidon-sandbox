package demo;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.DbRow;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class CommentMapper implements DbMapper<Comment> {
    private final static Logger LOGGER = Logger.getLogger(CommentMapper.class.getName());

    @Override
    public Comment read(DbRow dbRow) {
        var id = dbRow.column("id");
        var post = dbRow.column("post_id");
        var content = dbRow.column("content");
        var createdAt = dbRow.column("created_at");
        Comment comment = new Comment();
        comment.setId(id.as(UUID.class));
        comment.setPost(post.as(UUID.class));
        comment.setContent(content.as(String.class));
        comment.setCreatedAt(createdAt.as(Timestamp.class).toLocalDateTime());

        return comment;
    }

    @Override
    public Map<String, ?> toNamedParameters(Comment comment) {
        var map = Map.of(
                "post_id", comment.getPost(),
                "content", comment.getContent()
        );
        return map;
    }

    @Override
    public List<?> toIndexedParameters(Comment comment) {
        return List.of(
                comment.getId(),
                comment.getPost(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
