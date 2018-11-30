
# Building an application with Helidon MP

In the last post, we have discussed [building an application with Helidon SE](./GS-SE). In this post, we will use implements the same RESTful APIs, but use **Helidon MP** feature instead. 

## Kick start a Helidon application

Like the former steps, we can use the official Helidon archetypes to generate a Helidon application skeleton in seconds.

### Generate project skeleton

Open your terminal, run the following command to generate a project from Helidon MP archetype.

```bash
mvn archetype:generate -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-quickstart-mp \
    -DarchetypeVersion=0.10.5 \
    -DgroupId=io.helidon.examples \
    -DartifactId=quickstart-mp \
    -Dpackage=io.helidon.examples.quickstart.mp
```

After it is done, a new folder named **quickstart-mp** will be created in the current folder, which contains the skeleton codes of this project.

### Build 

In your terminal, switch to the newly created **quickstart-se** folder, run the following command to build the project.

```
mvn clean package
```

When it is finished, you will see there is a jar **quickstart-mp.jar** generated in the *target* folder.

### Run 

Run the application via:

```
$ java -jar target/quickstart-mp.jar
2018.11.30 20:11:16 INFO org.jboss.weld.Version Thread[main,5,main]: WELD-000900: 3.0.3 (Final)
2018.11.30 20:11:17 INFO org.jboss.weld.Bootstrap Thread[main,5,main]: WELD-ENV-000020: Using jandex for bean discovery
2018.11.30 20:11:18 INFO org.jboss.weld.Bootstrap Thread[main,5,main]: WELD-000101: Transactional services not available. Injection of @Inject UserTransaction not available. Transactional observers will be invoked synchronously.
2018.11.30 20:11:18 INFO org.jboss.weld.Event Thread[main,5,main]: WELD-000411: Observer method [BackedAnnotatedMethod] private org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider.processAnnotatedType(@Observes ProcessAnnotatedType) receives events for all annotated types. Consider restricting events using @WithAnnotations or a generic type with bounds.
2018.11.30 20:11:18 WARN org.jboss.weld.Bootstrap Thread[main,5,main]: WELD-000146: BeforeBeanDiscovery.addAnnotatedType(AnnotatedType<?>) used for class org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider$JaxRsParamProducer is deprecated from CDI 1.1!
2018.11.30 20:11:19 INFO org.jboss.weld.Bootstrap Thread[main,5,main]: WELD-ENV-002003: Weld SE container 2858f5c4-db37-457a-9206-86d05f55a627 initialized
2018.11.30 20:11:19 WARNING org.glassfish.jersey.internal.Errors Thread[main,5,main]: The following warnings have been detected: WARNING: The (sub)resource method getDefaultMessage in io.helidon.examples.quickstart.mp.GreetResource contains empty path annotation.

2018.11.30 20:11:19 INFO io.netty.util.internal.PlatformDependent Thread[main,5,main]: Your platform does not provide complete low-level API for accessing direct buffers reliably. Unless explicitly requested, heap buffer will always be preferred to avoid potential system instability.
2018.11.30 20:11:22 INFO io.helidon.webserver.netty.NettyWebServer Thread[nioEventLoopGroup-2-1,10,main]: Channel '@default' started: [id: 0x3ceb5109, L:/0:0:0:0:0:0:0:0:8080]
2018.11.30 20:11:22 INFO io.helidon.microprofile.server.ServerImpl Thread[nioEventLoopGroup-2-1,10,main]: Server started on http://localhost:8080 (and all other host addresses) in 2111 milliseconds.
```

As you see, different from Helidon SE, a JBoss Weld SE container was bootstrapped in Helidon MP for dependency injection.

Let's test the sample API using `curl` command. By default, the generated codes provide similar sample APIs as the Helidon SE one.

```
curl http://localhost:8080/greet
{"message":"Hello World!"}
```

```
curl http://localhost:8080/greet/Hantsy
{"message":"Hello Hantsy!"}
```


### Explore the source codes

Import the source codes into your favorite IDE(Intellij IDEA, Eclipse, Apache NetBeans etc), expands all nodes, the project structure looks like this. 

```
.
├── pom.xml
├── README.md
└── src
    ├── main
    │   ├── docker
    │   │   └── Dockerfile
    │   ├── java
    │   │   └── io
    │   │       └── helidon
    │   │           └── examples
    │   │               └── quickstart
    │   │                   └── mp
    │   │                       ├── GreetResource.java
    │   │                       ├── Main.java
    │   │                       └── package-info.java
    │   ├── k8s
    │   │   └── app.yaml
    │   └── resources
    │       ├── application.yaml
    │       └── logging.properties
    └── test
        └── java
            └── io
                └── helidon
                    └── examples
                        └── quickstart
                            └── mp
                                └── MainTest.java
```

In the pom.xml file, it defines a `maven-dependency-plugin` plugin to copy all its dependencies into *target/libs* folder, and package this project into a single **thin** jar file by `maven-jar-plugin`. When you ran `java -jar quickstart-se.jar`, it will search classpaths in the *libs* folder. 

`maven-jar-plugin` specifies `io.helidon.examples.quickstart.mp.Main` as main class, which is responsible for bootstrapping the application. 

In the  `io.helidon.examples.quickstart.mp.Main` file, there is a `main` method, which handles the following things:

* Configure logging with jdk logging framework.
* Create a `Server` instance via `Server.create`.
* By default, it can pick up a `microprofile-config.properties` in the project classpath if it exists.
* Then `start` the web server.

If you have some experience of JAX-RS, it is easy to understand the generated skeleton codes.

* `JaxrsActivator` declares a JAX-RS `application`, it activates JAX-RS in your application, and as the entrance of JAX-RS facility.
* `GreetResource` is a generic JAX-RS resource class.


## Build your REST APIs

Next we try to convert the former APIs to use Helidon MP.


### Cook your first APIs

Let's start with cooking the `Post` APIs, the expected APIs are listed below.

URI|request|response|description
---|---|---|---
/posts|GET|200, [{id:'1', title:'title'}, {id:'2', title:'title 2'}]| Get all posts
/posts|POST {title:'title',content:'content'} |201, set new created entity url in Location header| Create a new post
/posts/{id}|GET|{id:'1', title:'title',content:'content'}| Get a post by id
/posts/{id}|PUT {title:'title',content:'content'} |204, no content| Update specific post by id
/posts/{id}|DELETE|204, no content| Delete a post by id

Firstly, create a `Post` class which represents the post entries. 

```java
public class Post implements Serializable {

    String id;
    String title;
    String content;
    LocalDateTime createdAt;

    public static Post of(String title, String content){
        Post post = new Post();
        post.setId(UUID.randomUUID().toString());
        post.setCreatedAt(LocalDateTime.now());
        post.setTitle(title);
        post.setContent(content);

        return post;
    }

    // omits setters and getters, toString 
}
```

The `of` method provides a factory to create a new post quickly.

Create a dummy `PostRepository` for retrieving from and saving into database, currently I used a `ConcurrentHashMap` instead of a real world data storage.

```java
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
```

Create a `PostResource` to handle Post requests.

```java
@Path("/posts")
@RequestScoped
public class PostResource {
    private final static Logger LOGGER = Logger.getLogger(PostResource.class.getName());

    private final PostRepository posts;

    @Context
    ResourceContext resourceContext;

    @Context
    UriInfo uriInfo;

    @Inject
    public PostResource(PostRepository posts) {
        this.posts = posts;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllPosts() {
        return ok(this.posts.all()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response savePost(@Valid Post post) {
        Post saved = this.posts.save(Post.of(post.getTitle(), post.getContent()));
        return created(
            uriInfo.getBaseUriBuilder()
                .path("/posts/{id}")
                .build(saved.getId())
        ).build();
    }

    @Path("{id}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPostById(@PathParam("id") final String id) {
        Post post = this.posts.getById(id);
        if (post == null) {
            throw new PostNotFoundException(id);
        }
        return ok(post).build();
    }

    @Path("{id}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePost(@PathParam("id") final String id, @Valid Post post) {
        Post existed = this.posts.getById(id);
        existed.setTitle(post.getTitle());
        existed.setContent(post.getContent());

        Post saved = this.posts.save(existed);
        return noContent().build();
    }


    @Path("{id}")
    @DELETE
    public Response deletePost(@PathParam("id") final String id) {
        this.posts.deleteById(id);
        return noContent().build();
    }

    @Path("{id}/comments")
    public CommentResource postResource() {
        return resourceContext.getResource(CommentResource.class);
    }
}
```

The good part of JAX-RS is it is easy to append subresource in the current resource, such as `CommentResource`.

### Create comment APIs

Imagine a user can comment on a certain post, we can add the following comment APIs.


| Uri                    | Http Method | Request                                  | Response                                 | Description                              |
| ---------------------- | ----------- | ---------------------------------------- | ---------------------------------------- | ---------------------------------------- |
| /posts/{id}/comments | GET         |                                          | 200, [{'id':1, 'content':'comment content'},{}] | Get all comments of the certain post     |
| /posts/{id}/comments | POST        | {'content':'test content'}               | 201                                      | Create a new comment of the certain post |

Create a resource class for Comment.

```java
@RequestScoped
public class CommentResource {
    private final static Logger LOGGER = Logger.getLogger(CommentResource.class.getName());
    private final CommentRepository comments;

    @Context
    UriInfo uriInfo;

    @Context
    ResourceContext resourceContext;


    @PathParam("id")
    String postId;

    @Inject
    public CommentResource(CommentRepository commentRepository) {
        this.comments = commentRepository;
    }

    @GET
    public Response getAllComments() {
        return ok(this.comments.allByPostId(this.postId)).build();
    }

    @POST
    public Response saveComment(Comment commentForm) {
        Comment saved = this.comments.save(Comment.of(this.postId, commentForm.getContent()));
        return created(
            uriInfo.getBaseUriBuilder().path("/posts/{id}/comments/{commentId}")
                .build(this.postId, saved.getId())
        ).build();
    }
}
```

Register it in the `JaxrsActiviator` class. 

```java
 @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> set = new HashSet<>();
        set.add(GreetResource.class);
        set.add(PostResource.class);
		set.add(CommentResource.class);
//...
    }
```

Run the application by run Main class in your IDE. Let's have a try with our new created post APIs.

### Test the APIs

Get all posts.

```
curl -v -X GET http://localhost:8080/posts
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Fri, 30 Nov 2018 20:45:58 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"content":"My second post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"5bce0ded-0c52-4c67-85b1-af336b4e8b6c","title":"Hello Again, Helidon"},{"content":"My first post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"53b82fcc-c705-4e15-90ef-b0f29449b2c9","title":"Hello Helidon"}]* Connection #0 to host localhost left intact
```

Get post by id.

```
curl -v -X GET http://localhost:8080/posts/5bce0ded-0c52-4c67-85b1-af336b4e8b6c
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts/5bce0ded-0c52-4c67-85b1-af336b4e8b6c HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Fri, 30 Nov 2018 20:46:31 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
{"content":"My second post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"5bce0ded-0c52-4c67-85b1-af336b4e8b6c","title":"Hello Again, Helidon"}* Connection #0 to host localhost left intact
```

Create a new Post.

```
curl -v -X POST http://localhost:8080/posts -d "{\"title\":\"My test post\", \"content\":\"Content of my test post\"}" -H "Content-Type:application/json"
Note: Unnecessary use of -X or --request, POST is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> POST /posts HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
> Content-Type:application/json
> Content-Length: 61
>
* upload completely sent off: 61 out of 61 bytes
< HTTP/1.1 201 Created
< Date: Fri, 30 Nov 2018 20:46:59 +0800
< Location: http://[0:0:0:0:0:0:0:1]:8080/posts/07b66870-9c2c-47d2-b4e4-f0b7c980c52a
< transfer-encoding: chunked
< connection: keep-alive
```

The new created Post can be fetched by URL specified in the response`Location` header.

Verify if it is created successfully.

```
curl -v -X GET http://localhost:8080/posts
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Fri, 30 Nov 2018 20:47:41 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"content":"My second post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"5bce0ded-0c52-4c67-85b1-af336b4e8b6c","title":"Hello Again, Helidon"},{"content":"My first post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"53b82fcc-c705-4e15-90ef-b0f29449b2c9","title":"Hello Helidon"},{"content":"Content of my test post","createdAt":"2018-11-30T20:46:59.955","id":"07b66870-9c2c-47d2-b4e4-f0b7c980c52a","title":"My test post"}]* Connection #0 to host localhost left intact
```

Delete a post by id.


```
curl -v -X DELETE http://localhost:8080/posts/07b66870-9c2c-47d2-b4e4-f0b7c980c52a
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> DELETE /posts/07b66870-9c2c-47d2-b4e4-f0b7c980c52a HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 204 No Content
< Date: Fri, 30 Nov 2018 20:48:25 +0800
< connection: keep-alive
<
* Connection #0 to host localhost left intact
```

Verify if the post is deleted.

```
curl -v -X GET http://localhost:8080/posts
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Fri, 30 Nov 2018 20:48:45 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"content":"My second post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"5bce0ded-0c52-4c67-85b1-af336b4e8b6c","title":"Hello Again, Helidon"},{"content":"My first post of Helidon","createdAt":"2018-11-30T20:45:58.077","id":"53b82fcc-c705-4e15-90ef-b0f29449b2c9","title":"Hello Helidon"}]* Connection #0 to host localhost left intact
```

### Handle exceptions

Like generic JAX-RS in a Java EE application, we can define a custom `ExceptionMapper` to handle the exceptions.

In this example, define an exception named `PostNotFoundException`.

```java
public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String id) {
        super("Post:" + id + " was not found!");
    }
}
```

Create an `ExceptionMapper` for `PostNotFoundException`.

```java
@Provider
public class PostNotFoundExceptionMapper implements ExceptionMapper<PostNotFoundException> {
    @Override
    public Response toResponse(PostNotFoundException exception) {
        return status(Response.Status.NOT_FOUND).entity(exception.getMessage()).build();
    }
}
```

When a post is not found, set response status as 404.

Do not forget to register it in the `JaxrsActivator`.

```java
 @Override
    public Set<Class<?>> getClasses() {
		//...
        set.add(PostNotFoundExceptionMapper.class);
        return Collections.unmodifiableSet(set);
    }
```

Restarts the application, use curl to check if the error handling is worked as expected.

```
curl -v http://localhost:8080/posts/noneExisting
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts/noneExisting HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 404 Not Found
< Date: Fri, 30 Nov 2018 20:56:20 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
* Connection #0 to host localhost left intact
```

Get the [source codes](https://github.com/hantsy/helidon-sample) from my github, and play it yourself.




