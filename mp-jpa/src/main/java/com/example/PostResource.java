package com.example;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.logging.Logger;

import static javax.ws.rs.core.Response.*;

@Path("/posts")
@RequestScoped
public class PostResource {
    private final static Logger LOGGER = Logger.getLogger(PostResource.class.getName());

    private final PostRepository posts;

    @Context
    ResourceContext resourceContext;

    @Inject
    public PostResource(PostRepository posts) {
        this.posts = posts;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response findAll() {
        return ok(this.posts.findAll()).build();
    }

    @GET
    @Path("search")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
            summary = "Get all Posts",
            description = "Get all posts"
    )
    @APIResponse(
            responseCode = "200",
            name = "Post list",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = Post.class
                    )
            )
    )
    public Response searchByKeyword(
            @Parameter(name = "q", in = ParameterIn.QUERY, description = "keyword match tile or content")
            @QueryParam("q") String q,
            @Parameter(name = "offset", in = ParameterIn.QUERY, description = "pagination offset")
            @QueryParam("offset") @DefaultValue("0") int offset,
            @Parameter(name = "limit", in = ParameterIn.QUERY, description = "pagination limit")
            @QueryParam("limit") @DefaultValue("10") int limit

    ) {
        return ok(this.posts.findByKeyword(q, offset, limit)).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePost(@Valid Post post, @Context UriInfo uriInfo) {
        var saved = this.posts.save(Post.of(post.getTitle(), post.getContent()));
        var createdUri = uriInfo.getBaseUriBuilder()
                .path("/posts/{id}")
                .build(saved.getId());
        return created(createdUri).build();
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPostById(@PathParam("id") final String id) {
        return this.posts.findById(id)
                .map(post -> ok(post).build())
                .orElseThrow(
                        () -> new PostNotFoundException(id)
                );
    }

    @Path("{id}/status")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePostStatus(@PathParam("id") final String id, @Valid UpdatePostStatusRequest req) {
        this.posts.updateStatus(id, Post.Status.valueOf(req.getStatus()));
        return noContent().build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePost(@PathParam("id") final String id, @Valid Post post) {
        return this.posts.findById(id)
                .map(existed -> {
                    existed.setTitle(post.getTitle());
                    existed.setContent(post.getContent());

                    Post saved = this.posts.save(existed);
                    return noContent().build();
                })
                .orElseThrow(
                        () -> new PostNotFoundException(id)
                );
    }


    @Path("{id}")
    @DELETE
    public Response deletePost(@PathParam("id") final String id) {
        var deleted = this.posts.deleteById(id);
        if (deleted == 0) {
            throw new PostNotFoundException(id);
        }
        return noContent().build();
    }

    @Path("{id}/comments")
    public CommentResource postResource() {
        return resourceContext.getResource(CommentResource.class);
    }
}
