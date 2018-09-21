# A Quick Glance at Helidon project

Oracle open-sourced a project named [Helidon](https://helidon.io/) which is a collection of libraries for building Microservice based applications.

Helidon provides two programming models for developers.

* **helidon-se** embraces [Reactive Streams](https://www.reactive-steams.org) specification, and provides functional style programming capability.

* For those familiar with Java EE/MicroProfile specifications, **helidon-mp** provides MicroProfile compatibility, it is easy to update yourself to Helidon.  

For the above cases, Helidon provides two Maven archetypes for generating the project skeleton in seconds. Follow the [Getting Started](https://helidon.io/docs/latest/#/getting-started) section of the official docs, it is easy to create a Helidon project via these Maven archetypes.

Make sure you have installed all [prerequisites](https://helidon.io/docs/latest/#/getting-started/01_prerequisites), let's create your first Helidon project.

## Kick start your first helidon application

### Generate project skeleton

Run the following command to generate a project from Helidon SE archetype.

```bash
mvn archetype:generate -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-quickstart-se \
    -DarchetypeVersion=0.10.0 \
    -DgroupId=io.helidon.examples \
    -DartifactId=quickstart-se \
    -Dpackage=io.helidon.examples.quickstart.se
```

After it is done, a new folder named **quickstart-se** will be created in the current folder, which contains the source codes of the project.

### Build 

Open a terminal tool, enter the **quickstart-se** folder, run the following command to build the project.

```
mvn clean package
```

After it is done, there is a jar **quickstart-se.jar** generated in the *target* folder.

### Run 

Run the application via:

```
$ java -jar target/quickstart-se.jar
[DEBUG] (main) Using Console logging
2018.09.19 15:40:34 INFO io.netty.util.internal.PlatformDependent Thread[main,5,main]: Your platform does not provide complete low-level API for accessing direct buffers reliably. Unless explicitly requested, heap buffer will always be preferred to avoid potential system instability.
2018.09.19 15:40:36 INFO io.helidon.webserver.netty.NettyWebServer Thread[nioEventLoopGroup-2-1,10,main]: Channel '@default' started: [id: 0xdb9f4bd1, L:/0:0:0:0:0:0:0:0:8080]
WEB server is up! http://localhost:8080
```

Let's test the sample API by `curl` command.

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

Import the source codes into your favorite IDE. 

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

`maven-jar-plugin` specifies `io.helidon.examples.quickstart.se.Main` as main class, which is responsible for bootstrapping the application. In the  `io.helidon.examples.quickstart.se.Main` file, there is a `main` method, which handles the following things:

* Configure logging with jdk logging framework.
* Load the server configuration via helidon Config APIs.
* Create a `WebServer` instance via `WebServer.create`, which accepts a `ServerConfiguration` and a `Routing` instance as arguments.
* Then `start` the web server, setup a hook when it is started.
* Set up a `shutdown` hook to the web server.

The `createRouting` method configures the routing rules, here it connects `/greet` uri prefix to `GreetService`. The `GreetService` is an implementation of `io.helidon.webserver.Service`, its `update` method defines its own routine rules.


## Build your first REST APIs

Let's create your own REST APIs, as an example, I reuse the blog application concept which I have used to demonstrate different technologies, check the sample codes for Spring and Java EE from My github account. 

Firstly, create a `Post` class which represents the post entries. The `of` method provides a factory to create a new post quickly.

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

Create a dummy `PostRepository` for retrieving from and saving into database, currently I used a `ConcurrentHashMap` instead of a real data storage.

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

Create a Post.

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

Verify if it is created.

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

Verify the post is deleted.

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




  
