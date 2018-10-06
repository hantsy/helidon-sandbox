# A Quick Glance at Helidon project

Oracle open-sourced a project named [Helidon](https://helidon.io/) which is a collection of libraries for building Microservice based applications.

Helidon provides two programming models for developers.

* **helidon-se** embraces [Reactive Streams](https://www.reactive-steams.org) specification, and provides functional style programming experience.

* For those familiar with Java EE/MicroProfile specifications, **helidon-mp** provides MicroProfile compatibility, it is easy to update yourself to Helidon.  

For the above cases, Helidon provides two Maven archetypes for generating the project skeleton in seconds. Follow the [Getting Started](https://helidon.io/docs/latest/#/getting-started) guide of the official docs, it is easy to create a Helidon project via these Maven archetypes in seconds.

Firstly, make sure you have installed all items mentioned in the [prerequisites](https://helidon.io/docs/latest/#/getting-started/01_prerequisites), next let's try to create your first Helidon project.

## Kick start a Helidon application

In this post, we will create *Helidon SE* project for demonstration purpose. In comparison, I will introduce the usage of **Helidon MP** in another post.

### Generate project skeleton

Open your terminal, run the following command to generate a project from Helidon SE archetype.

```bash
mvn archetype:generate -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-quickstart-se \
    -DarchetypeVersion=0.10.0 \
    -DgroupId=io.helidon.examples \
    -DartifactId=quickstart-se \
    -Dpackage=io.helidon.examples.quickstart.se
```

After it is done, a new folder named **quickstart-se** will be created in the current folder, which contains the skeleton codes of this project.

### Build 

In your terminal, switch to the newly created **quickstart-se** folder, run the following command to build the project.

```
mvn clean package
```

When it is finished, you will see there is a jar **quickstart-se.jar** generated in the *target* folder.

### Run 

Run the application via:

```
$ java -jar target/quickstart-se.jar
[DEBUG] (main) Using Console logging
2018.09.19 15:40:34 INFO io.netty.util.internal.PlatformDependent Thread[main,5,main]: Your platform does not provide complete low-level API for accessing direct buffers reliably. Unless explicitly requested, heap buffer will always be preferred to avoid potential system instability.
2018.09.19 15:40:36 INFO io.helidon.webserver.netty.NettyWebServer Thread[nioEventLoopGroup-2-1,10,main]: Channel '@default' started: [id: 0xdb9f4bd1, L:/0:0:0:0:0:0:0:0:8080]
WEB server is up! http://localhost:8080
```

Let's test the sample API using `curl` command.

```
curl http://localhost:8080/greet
{"message":"Hello World!"}
```

```
curl http://localhost:8080/greet/Hantsy
{"message":"Hello Hantsy!"}
```

```
curl -X PUT http://localhost:8080/greet/greeting/Hantsy
{"greeting":"Hantsy"}
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
    │   │                   └── se
    │   │                       ├── GreetService.java
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
                            └── se
                                └── MainTest.java
```

In the pom.xml file, it defines a `maven-dependency-plugin` plugin to copy all its dependencies into *target/libs* folder, and package this project into a single **thin** jar file by `maven-jar-plugin`. When you ran `java -jar quickstart-se.jar`, it will search classpaths in the *libs* folder. 

`maven-jar-plugin` specifies `io.helidon.examples.quickstart.se.Main` as main class, which is responsible for bootstrapping the application. 

In the  `io.helidon.examples.quickstart.se.Main` file, there is a `main` method, which handles the following things:

* Configure logging with jdk logging framework.
* Load the server configuration via helidon Config APIs.
* Create a `WebServer` instance via `WebServer.create`, which accepts a `ServerConfiguration` and a `Routing` instance as arguments.
* Then `start` the web server, setup a hook when it is started.
* Set up a `shutdown` hook to listen the web server when it is stopped.

The `createRouting` method configures the routing rules, here it connects `/greet` uri prefix to `GreetService`. The `GreetService` is an implementation of `io.helidon.webserver.Service`, its `update` method defines its own routine rules.


## Build your REST APIs

As an example, we reuse the blog application concept which I have used to demonstrate different technologies, check the sample codes for Spring and Java EE from [My github account](https://github.com/hantsy). 


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

Create a `PostService` to handle Post requests.

```java
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
            .delete("/{id}", this::deletePostById);
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
        Post post = this.posts.getById(serverRequest.path().param("id"));
        serverResponse.status(200).send(EntityUtils.toJsonObject(post));
    }

    private void getAllPosts(ServerRequest serverRequest, ServerResponse serverResponse) {
        serverResponse.send(EntityUtils.toJsonArray(this.posts.all()));
    }

}
```

>Note: Currently Helidon only supports JSON-P for transforming JSON data automatically. As you see in the above codes, you have to convert your data into a JSON-P specific `JsonObject` or `JsonArray`.

Register it in the `Main` class. 

```java
private static Routing createRouting() {
	//...
		.register("/posts", new PostService(new PostRepository()))
	//    .build();
}
```

Run the application by run Main class in your IDE. Let's have a try with our new created post APIs.

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
< Date: Fri, 21 Sep 2018 21:27:15 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"id":"7150f8de-4377-4bf7-8c6f-049d5b822c1c","title":"Hello Helidon","content":"My first post of Helidon","createdAt":"2018-09-21T21:24:12.965"},{"id":"1c60fbb1-785c-4463-9e27-a0dcf771d66e","title":"Hello Again, Helidon","content":"My second post of Helidon","createdAt":"2018-09-21T21:24:12.965"}]* Connection #0 to host localhost left intact
```

Get post by id.

```
curl -v -X GET http://localhost:8080/posts/7150f8de-4377-4bf7-8c6f-049d5b822c1c
Note: Unnecessary use of -X or --request, GET is already inferred.
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> GET /posts/7150f8de-4377-4bf7-8c6f-049d5b822c1c HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 200 OK
< Content-Type: application/json
< Date: Fri, 21 Sep 2018 21:27:55 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
{"id":"7150f8de-4377-4bf7-8c6f-049d5b822c1c","title":"Hello Helidon","content":"My first post of Helidon","createdAt":"2018-09-21T21:24:12.965"}* Connection #0 to host localhost left intact
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
< Date: Fri, 21 Sep 2018 21:29:28 +0800
< Location: /posts/cec239d7-f6da-48ff-ab42-9e6a4416d7f0
< transfer-encoding: chunked
< connection: keep-alive
<
* Connection #0 to host localhost left intact
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
< Date: Fri, 21 Sep 2018 21:29:58 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"id":"cec239d7-f6da-48ff-ab42-9e6a4416d7f0","title":"My test post","content":"Content of my test post","createdAt":"2018-09-21T21:29:28.285"},{"id":"7150f8de-4377-4bf7-8c6f-049d5b822c1c","title":"Hello Helidon","content":"My first post of Helidon","createdAt":"2018-09-21T21:24:12.965"},{"id":"1c60fbb1-785c-4463-9e27-a0dcf771d66e","title":"Hello Again, Helidon","content":"My second post of Helidon","createdAt":"2018-09-21T21:24:12.965"}]* Connection #0 to host localhost left intact
```

Delete a post by id.


```
curl -v -X DELETE http://localhost:8080/posts/cec239d7-f6da-48ff-ab42-9e6a4416d7f0
*   Trying ::1...
* TCP_NODELAY set
* Connected to localhost (::1) port 8080 (#0)
> DELETE /posts/cec239d7-f6da-48ff-ab42-9e6a4416d7f0 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.55.1
> Accept: */*
>
< HTTP/1.1 204 No Content
< Date: Fri, 21 Sep 2018 21:30:46 +0800
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
< Date: Fri, 21 Sep 2018 21:31:24 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
[{"id":"7150f8de-4377-4bf7-8c6f-049d5b822c1c","title":"Hello Helidon","content":"My first post of Helidon","createdAt":"2018-09-21T21:24:12.965"},{"id":"1c60fbb1-785c-4463-9e27-a0dcf771d66e","title":"Hello Again, Helidon","content":"My second post of Helidon","createdAt":"2018-09-21T21:24:12.965"}]* Connection #0 to host localhost left intact
```

### Create comment APIs

Imagine a user can comment on a certain post, we can add the following comment APIs.


| Uri                    | Http Method | Request                                  | Response                                 | Description                              |
| ---------------------- | ----------- | ---------------------------------------- | ---------------------------------------- | ---------------------------------------- |
| /posts/{id}/comments | GET         |                                          | 200, [{'id':1, 'content':'comment content'},{}] | Get all comments of the certain post     |
| /posts/{id}/comments | POST        | {'content':'test content'}               | 201                                      | Create a new comment of the certain post |

Like building the Post APIs, create a `Comment` class for presenting the comment entity.

```java
public class Comment implements Serializable {
    private String id;
    private String post;
    private String content;
    private LocalDateTime createdAt;

    public static Comment of(String postId, String content) {
        Comment comment = new Comment();

        comment.setId(UUID.randomUUID().toString());
        comment.setContent(content);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setPost(postId);

        return comment;
    }
	
	// omits setters and getters, etc
}
```

Create a `CommentRepository` for data operation.

```java
public class CommentRepository {
    static Map<String, Comment> data = new ConcurrentHashMap<>();

    public List<Comment> all() {
        return new ArrayList<>(data.values());
    }

    public Comment getById(String id) {
        return data.get(id);
    }

    public Comment save(Comment comment) {
        data.put(comment.getId(), comment);
        return comment;
    }

    public void deleteById(String id) {
        data.remove(id);
    }

    public List<Comment> allByPostId(String id) {
        return data.values().stream().filter(c -> c.getPost().equals(id)).collect(toList());
    }
}
```

And a `CommentService` class for handling requests.

```java
public class CommentService implements Service{
    private final static Logger LOGGER = Logger.getLogger(CommentService.class.getName());
    private final CommentRepository comments;

    public CommentService(CommentRepository commentRepository) {
        this.comments = commentRepository;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllComments)
            .post("/", Handler.of(JsonObject.class, this::saveComment, this::errorHandler));
    }

    private void errorHandler(ServerRequest serverRequest, ServerResponse serverResponse, Throwable throwable) {
        if (throwable instanceof CommentBodyCanNotBeEmptyException) {
            serverResponse.status(400).send();
        } else {
            serverRequest.next(throwable);
        }
    }

    private void getAllComments(ServerRequest serverRequest, ServerResponse serverResponse) {
        String postId = serverRequest.path().absolute().param("id");
        LOGGER.info("comments of post id::" + postId);
        serverResponse.send(this.toJsonArray(this.comments.allByPostId(postId)));
    }

    private void saveComment(ServerRequest serverRequest, ServerResponse serverResponse, JsonObject content) {

        String postId = serverRequest.path().absolute().param("id");
        String body = content.get("content") == null ? null : content.getString("content");

        if (body == null) {
            serverRequest.next(new CommentBodyCanNotBeEmptyException());
        }

        CompletableFuture.completedFuture(content)
            .thenApply(c -> Comment.of(postId, body))
            .thenApply(this.comments::save)
            .thenCompose(
                c -> {
                    serverResponse.status(201)
                        .headers()
                        .location(URI.create("/posts/" + postId + "/comments/" + c.getId()));
                    return serverResponse.send();
                }
            );
    }


    private JsonObject toJsonObject(Comment comment) {
        return Json.createObjectBuilder()
            .add("id", comment.getId())
            .add("post", comment.getPost())
            .add("content", comment.getContent())
            .add("createdAt", comment.getCreatedAt().toString())
            .build();
    }

    private JsonArray toJsonArray(List<Comment> comments) {
        JsonArrayBuilder jsonArrayBuilder = Json.createArrayBuilder();
        comments.forEach(p -> {
            jsonArrayBuilder.add(toJsonObject(p));
        });

        return jsonArrayBuilder.build();
    }


}
```

Register it as a sub service under `PostService`.

```java
@Override
public void update(Routing.Rules rules) {
    //...
		.register("/{id}/comments", new CommentService(new CommentRepository()));
}
```

>Note: In the `CommentService`, we have to use `path().absolute().param()`  to get the param in the parent path, `path().param()` will return null when you try to get post id in the parent path. 

### Handle error in Routing

In your `Service`, you can send error to client directly via `WebResponse.send` if there is an exception or an error occurred. 

And you can also use `WebRequest.next` to pass the exception to the downstream to handle it later. 

In this example, we defined an exception named `PostNotFoundException`.

```java
public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(String id) {
        super("Post:" + id + " was not found!");
    }
}
```

When a post was not found by id, try to throw an exception.

```java
String id = serverRequest.path().param("id");
Post post = this.posts.getById(id);
if (post == null) {
	serverRequest.next(new PostNotFoundException(id));
}
serverResponse.status(200).send(EntityUtils.toJsonObject(post));
```

In `Main` class, handles the exception in the `error` method. 

```java
private static Routing createRouting() {
	return Routing.builder()
		//...
		.error(Throwable.class, handleErrors())
		.build();
}

private static ErrorHandler<Throwable> handleErrors() {
	return (req, res, t) -> {
		if (t instanceof PostNotFoundException) {
			res.status(404).send(((PostNotFoundException) t).getMessage());
		} else {
			req.next(t);
		}
	};
}
``` 


Run this application, try to use curl to test if the error handling worked as expected.

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
< Content-Type: text/plain;charset=UTF-8
< Date: Sat, 6 Oct 2018 14:53:56 +0800
< transfer-encoding: chunked
< connection: keep-alive
<
Post:noneExisting was not found!* Connection #0 to host localhost left intact
```

Get the [source codes](https://github.com/hantsy/helidon-sample) from my github, and play it yourself.




