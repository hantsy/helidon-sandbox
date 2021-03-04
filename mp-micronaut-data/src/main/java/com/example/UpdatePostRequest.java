package com.example;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

public class UpdatePostRequest implements Serializable {
    @NotBlank
    String title;

    @NotBlank
    String content;

    public static UpdatePostRequest of(String title, String content) {
        UpdatePostRequest post = new UpdatePostRequest();
        post.setTitle(title);
        post.setContent(content);
        return post;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "UpdatePostRequest{" +
                "title='" + title + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}
