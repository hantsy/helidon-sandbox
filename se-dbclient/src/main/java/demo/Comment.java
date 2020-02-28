package demo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public class Comment implements Serializable {
    private UUID id;
    private UUID post;
    private String content;
    private LocalDateTime createdAt;

    public static Comment of(UUID postId, String content) {
        Comment comment = new Comment();

        comment.setContent(content);
        comment.setPost(postId);
        return comment;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPost() {
        return post;
    }

    public void setPost(UUID post) {
        this.post = post;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Comment{" +
            "id='" + id + '\'' +
            ", post='" + post + '\'' +
            ", content='" + content + '\'' +
            ", createdAt=" + createdAt +
            '}';
    }
}
