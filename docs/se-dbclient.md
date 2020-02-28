# Accessing Database with DbClient

In my previous article of [introducing Helidon](https://medium.com/@hantsy/a-quick-glance-at-helidon-project-ca8bee8ad34b), I have demonstrated how to create a simple CRUD with RESTful APIs using the functional programming approach provided Helidon SE. But unluckily there is no means to connect to a database using the Helidon SE stack. But things will be changed soon in the upcoming Helidon 2.0. 

[The first milestone of Helidon 2.0](https://medium.com/helidon/where-helidon-flies-809007221f1f) is on board, it shipped with a new **Db Client** in Helidon SE for database operations, currently it supports Jdbc and Mongo. More info about the breaking changes of Helidon 2.0, check the [Changelog](https://github.com/oracle/helidon/blob/2.0.0-M1/CHANGELOG.md).

In this post, we will add **DbClient** to [our former sample application](https://github.com/hantsy/helidon-sandbox/tree/master/se-start), and replace the dummy codes in our **Repository** with true database operations.

[toc]

## Preparing the Project Skeleton



The newest Helidon reorganized the structure of its architypes. If you work on the existing codes, follow these steps to upgrade the existing codes to the new Helidon.  

First of all, use the new  `helidon-se` BOM as the parent in the project *pom.xml*. 

```xml
<parent>
    <groupId>io.helidon.applications</groupId>
    <artifactId>helidon-se</artifactId>
    <version>2.0.0-M1</version>
    <relativePath/>
</parent>
```

Add the following dependencies. All of them are managed in  `helidon-se` .

```xml
 <dependencies>
     <dependency>
         <groupId>io.helidon.bundles</groupId>
         <artifactId>helidon-bundles-webserver</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.config</groupId>
         <artifactId>helidon-config-yaml</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.health</groupId>
         <artifactId>helidon-health</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.health</groupId>
         <artifactId>helidon-health-checks</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.metrics</groupId>
         <artifactId>helidon-metrics</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.media.jsonp</groupId>
         <artifactId>helidon-media-jsonp-server</artifactId>
     </dependency>
     <dependency>
         <groupId>io.helidon.media.jsonb</groupId>
         <artifactId>helidon-media-jsonb-server</artifactId>
     </dependency>
     
     ...
</dependencies>     
```

* The `helidon-bundles-webserver` is the infrastructure of Helidon SE `WebServer`.  

* The `helidon-config-yaml` enable YAML format in the application configuration.
* The `helidon-health`, `helidon-health-checks` and `helidon-metrics` add  health check and metrics support for service observability at runtime.
* The `helidon-media-jsonp-server` adds JSON processing support and HTTP message encode and decodes.  Besides JSON-P, it also support JSON-B and Jackson by adding `helidon-media-jsonb-server` and `helidon-media-jacksone-server`.

Remove the old  Helidon declared in **dependencyManagement** . 

In the  **build** element,  replace the configuration of `maven-dependency-plugin` with the following .

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>copy-libs</id>
        </execution>
    </executions>
</plugin>
```

In the *properties* , only needs to declare the **mainClass** properties.

```xml
<mainClass>demo.Main</mainClass>
```

Alternatively, you can follow the newest [QuickStart Guide](https://helidon.io/docs/2.0.0-M1/#/guides/02_quickstart-se) to generate a new project skeleton through  `helidon-quickstart-se` maven architype.

```bash
mvn archetype:generate -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-quickstart-se \
    -DarchetypeVersion=2.0.0-M1 \
    -DgroupId=io.helidon.examples \
    -DartifactId=helidon-quickstart-se \
    -Dpackage=io.helidon.examples.quickstart.se
```

Then copy [the existing codes](https://github.com/hantsy/helidon-sandbox/tree/master/se-start) to this project folder.

## Configuring Db Client

Let's start to contribute database operations using  **DbClient**.

Add the **Db Client** related dependencies.

```xml
<dependency>
    <groupId>io.helidon.dbclient</groupId>
    <artifactId>helidon-dbclient-jdbc</artifactId>
</dependency>
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <version>42.2.10</version>
</dependency>
```

* The `helidon-dbclient-jdbc` is required for DbClient Jdbc support.
* The `HikariCP` is use for creating Jdbc connection pool.
* Add the latest `postgresql` Jdbc driver, we will connect a  PostgreSQL server in our application. 

Configure db in the *application.yaml*.

```yaml
db:
  source: jdbc
  connection:
    url: jdbc:postgresql://127.0.0.1:5432/test
    username: user
    password: password
    poolName: hikariPool
    initializationFailTimeout: -1
    connectionTimeout: 2000
```

DbClient uses a mapper class to map the data between database table rows and Java POJOs. 

### Registering a DbMapper

Let's start with the `Post` POJO.

```java
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

```

A custom `DbMapper` must implements `DbMapper` interface with a POJO class as parameterized type, there are 3 methods needed to be implemented.

* The ` T read(DbRow dbRow)` method is used for reading database table rows and copy the data to the mapped POJOs.
* The `toNamedParameters`  and `toIndexedParameters` are used for setting the values of a POJO class to the named params and indexed params in the SQL query string, esp. when performing inserting and updating operations there are a couple of params are to be bound before executing queries.

Create a  `DbMapperProvider` to register a `DbMapper`.

```java
@Priority(1000)
public class PostMapperProvider implements DbMapperProvider {
    private static final PostMapper MAPPER = new PostMapper();

    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Post.class)) {
            return Optional.of((DbMapper<T>) MAPPER);
        }
        return Optional.empty();
    }
}
```

Helidon uses Java built-in **Service Locater** to find these `DbMapperProvider` s as the application is starting.

Create  a file named *io.helidon.dbclient.spi.DbMapperProvider* in the folder *src/main/resources/META-INF/services/*.

Add the FQN of the `PostMapperProvider` class into this file.

```bash
demo.PostMapperProvider
```

Let's refactor the `PostRepository` class.

## Refactoring  PostRepository

Add a constructor to accept a `DbClient` argument.

```java
public class PostRepository {

    private DbClient dbClient;

    public PostRepository(DbClient dbClient) {
        this.dbClient = dbClient;
    }
	...
}
```

> No worries how to find a `DbClient` dependency, and we do not use any Dependency Injection  frameworks at all. We will assemble the dependencies manually in the `Main` class.

 Let's start with the `all` method which is used for fetching all posts.

```java
public CompletionStage<List<Post>> all() {
    return this.dbClient
        .execute(dbExecute -> dbExecute.createQuery("SELECT * FROM posts")
                 .execute()
                )
        .thenCompose(dbRowDbRows -> dbRowDbRows.map(Post.class).collect());
}
```

The `execute` method of `DbClient` accept `Function` which uses a `DbExecute`  as source.  `DbExecute` provides a collection of methods to simplify the CRUD operations on database.

* The `createQuery`, `createInsert`, `createUpdate` and `createDelete` are used for perform generic queries(mainly `SELECT`), `INSERT`, `UPDATE`, `DELETE`  SQL clauses .
* The `createNamedQuery`, `createNamedInsert`, `createNamedUpdate` and `createNamedDelete` are similar with the above, but the SQL statements are defined in the *application.yaml*, use the name to refer them in the methods .
* The `query`, `insert`, `update` and `delete` methods are a simple form, and they accept index based params as the second argument.

Besides these, `createGet`(and `createdNamedGet`, `get`) is used for a query result contains one or zero result, `createDml`(and `createdNamedDml`, `dml`) is use for DML operations, and `createStatement`(and `createdNamedStatement`, `statement`) is for executing general purpose DML or DQL clauses.

 The `createQuery.execture`  wraps the query result(`DbRows<DbRow>`) into a stream(`CompletionStage`),  and `DbRows.map` method will apply the rule we defined in `PostMapper.read` and convert a `DbRow` to a `Post`,  the `collect` to transform the stream into a new stream(`CompletionStage<List<T>>`).

Let's move to `getById` method.

```java
public CompletionStage<Post> getById(UUID id) {
    return this.dbClient
        .execute(
        dbExecute -> dbExecute.createGet("SELECT * FROM posts WHERE id=?")
        .addParam(id)
        .execute()
    )
        .thenApply(
        rowOptional -> rowOptional.map(dbRow -> dbRow.as(Post.class)).orElseThrow(() -> new PostNotFoundException(id))
    );
}
```

The `createGet`  will return an `Optional<DbRow>` in the stream, finally we convert it to `CompletionStage<Post>`. And if is not present, throw an predefined `PostNotFoundException` instead, the existing error handler can handle this exception.

You have to notice `.addParam`  of a `DbStatement`, there are several options to bind data to the params in query clauses.

* The  `params(List<?>)`, `addParam(Object)`and `indexedParam(Object)` are use for binding position-based params.

* The  `params(Map<?>)`, `addParam(String, Object)`and `namedParam(Object)` are use for binding name-based params.

>Note,  `indexedParam(Object)` and  `namedParam(Object)` will apply the rules through `toIndexedParameters` and `toNamedParameters` methods defined in our `DbMapper`.

Let's have a look at `save` method.

```java
public CompletionStage<UUID> save(Post post) {
    return this.dbClient
        .execute(
        dbExecute -> dbExecute
        .query("INSERT INTO posts(title, content) VALUES (?, ?) RETURNING id", post.getTitle(), post.getContent())

    )
        .thenCompose(DbRows::collect)
        .thenApply(data -> data.isEmpty() ? null : data.get(0).column("id").as(UUID.class));
}
```

Generally, the insert, update, delete queries will return the number of affected records.

Here, we want to return the id of the inserted record, and we use a generic query instead, and unwrap the returning id manually.

OK, let's go to the update method.

```java
    public CompletionStage<Long> update(UUID id, Post post) {
        return this.dbClient
                .inTransaction(
                        tx -> tx.createGet("SELECT * FROM posts WHERE id=? FOR UPDATE")
                                .addParam(id)
                                .execute()
                                .thenApply(
                                        rowOptional -> rowOptional.map(dbRow -> dbRow.as(Post.class)).orElseThrow(() -> new PostNotFoundException(id))
                                )
                                .thenApply(p ->
                                        Map.of("title", post.getTitle(), "content", post.getContent(), "id", id)
                                )
                                .thenCompose(
                                        map -> tx.createUpdate("UPDATE posts SET title=:title, content=:content WHERE id=:id")
                                                .params(map)
                                                .execute()
                                )

                );
    }
```

Instead of using `dbClient.execute`, we use `dbClient.inTransaction` here to execute a series of SQL in a transaction.

For updating an existing record, using **SELECT FOR UPDATE** mode is reasonable, query it firstly for a update lock and then perform a update it as expected.

Lastly, let's look at the `deleteById` method.

```java
public CompletionStage<Long> deleteById(UUID id) {
    return this.dbClient.execute(
        dbExecute -> dbExecute.createDelete("DELETE FROM posts WHERE id = :id")
        .addParam("id", id)
        .execute()
    );
}
```

All methods of the `PostRepository` are updated to using Java 8' s `CompletionStage` as return type, so we have to refresh our former ` PostService`.

```java
public class PostService implements Service {
    private final static Logger LOGGER = Logger.getLogger(PostService.class.getName());

    private final PostRepository posts;
    private final CommentRepository comments;

    public PostService(PostRepository posts, CommentRepository comments) {
        this.posts = posts;
        this.comments = comments;
    }

    @Override
    public void update(Routing.Rules rules) {
        rules.get("/", this::getAllPosts)
                .post("/", this::savePost)
                .get("/{id}", this::getPostById)
                .put("/{id}", this::updatePost)
                .delete("/{id}", this::deletePostById)
                .register("/{id}/comments", new CommentService(comments));
    }

    private void deletePostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        this.posts.deleteById(id)
                .thenCompose(
                        count -> {
                            LOGGER.log(Level.INFO, "{0} posts deleted.", count);
                            return serverResponse.status(204).send();
                        }
                );

    }

    private UUID extractIdFromPathParams(ServerRequest serverRequest) {
        try {
            return UUID.fromString(serverRequest.path().param("id"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse `id` from request param...", e);
            serverRequest.next(e);
        }
        return null;
    }

    private void updatePost(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        serverRequest.content().as(JsonObject.class)
                .thenApply(EntityUtils::fromJsonObject)
                .thenCompose(data -> this.posts.update(id, data))
                .thenCompose(
                        p -> serverResponse.status(204).send()
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to updatePost", throwable);
                    serverRequest.next(throwable);
                    return null;
                });

    }


    private void savePost(ServerRequest serverRequest, ServerResponse serverResponse) {

        serverRequest.content().as(JsonObject.class)
                .thenApply(EntityUtils::fromJsonObject)
                .thenApply(p ->
                        Post.of(p.getTitle(), p.getContent())
                )
                .thenCompose(this.posts::save)
                .thenCompose(
                        p -> {
                            serverResponse.status(201)
                                    .headers()
                                    .location(URI.create("/posts/" + p));
                            return serverResponse.send();
                        }
                )
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to savePost", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

    private void getPostById(ServerRequest serverRequest, ServerResponse serverResponse) {
        var id = extractIdFromPathParams(serverRequest);
        this.posts.getById(id)
                .thenCompose(post -> serverResponse.status(200).send(EntityUtils.toJsonObject(post)))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to getPostById", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

    private void getAllPosts(ServerRequest serverRequest, ServerResponse serverResponse) {
        this.posts.all()
                .thenApply(EntityUtils::toJsonArray)
                .thenCompose(data -> serverResponse.send(data))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to getAllPosts", throwable);
                    serverRequest.next(throwable);
                    return null;
                });
    }

}

```

In the former codes, we used `Routing.builder()...error()` in the `Main` class to handle global exceptions. Here we have to improve it slightly. Because when throwing an exception a `CompletionStage` stream, it wraps it into a  `CompletionException`.

```java
private static ErrorHandler<Throwable> handleErrors() {
    return (req, res, t) -> {
        Throwable root = t;

        while (!(root instanceof PostNotFoundException) && root.getCause() != null) {
            root = root.getCause();
        }

        if (root instanceof PostNotFoundException) {
            res.status(404).send(root.getMessage());
        } else {
            req.next(t);
        }
    };
}
```



Let's assemble the dependencies in the `Main` class. 

```java
private static Routing createRouting(Config config) {
    Config dbConfig = config.get("db");

    // Interceptors added through a service loader
    DbClient dbClient = DbClient.builder(dbConfig)
        .build();

    HealthSupport health = HealthSupport.builder()
        .addLiveness(DbClientHealthCheck.create(dbClient))
        .addLiveness(HealthChecks.healthChecks())
        .build();
    MetricsSupport metrics = MetricsSupport.create();

    var greetService = new GreetService(config);
    var posts = new PostRepository(dbClient);
    var comments = new CommentRepository(dbClient);
    var postService = new PostService(posts, comments);

    // initializing data...
    DataInitializer.init(dbClient);

    return Routing.builder()
        .register(JsonSupport.create())
        .register(health)                   // Health at "/health"
        .register(metrics)                  // Metrics at "/metrics"
        .register("/greet", greetService)
        .register("/posts", postService)
        .error(Throwable.class, handleErrors())
        .build();
}
```

The `DataInitializer ` is use for initializing sample data into database.

```java
public class DataInitializer {
    private static final Logger LOGGER = Logger.getLogger(DataInitializer.class.getName());

    public static void init(DbClient dbClient) {
        dbClient.inTransaction(
                tx -> tx.createDelete("DELETE FROM comments").execute()
                        .thenAccept(
                                count -> LOGGER.log(Level.INFO, "{0} comments deleted.", count)
                        )
                        .thenCompose(
                                v -> tx.createDelete("DELETE FROM posts").execute()
                                        .thenAccept(count2 -> LOGGER.log(Level.INFO, "{0} posts deleted.", count2))
                        )
                        .thenCompose(
                                v2 -> tx.createInsert("INSERT INTO posts(title, content) VALUES(?, ?), (?, ?)")
                                        .params(List.of("My first post of Helidon", "The content of my first post", "My second post of Helidon", "The content of my second post"))
                                        .execute()
                                        .thenAccept(count3 -> LOGGER.log(Level.INFO, "{0} posts inserted.", count3))
                        )
                        .thenCompose(
                                v2 -> tx.createQuery("SELECT * FROM posts")
                                        .execute()
                                        .thenCompose(dbRowDbRows -> dbRowDbRows.collect())
                                        .thenAccept(rows -> LOGGER.log(Level.INFO, "found posts: {0}.", rows))
                        )

                        .exceptionally(throwable -> {
                            LOGGER.log(Level.WARNING, "Failed to initialize data", throwable);
                            return null;
                        })
        );
    }

    private DataInitializer() {
    }
}

```

It performs the following tasks.

* Delete all comments
* Delete all posts
* Insert two sample posts
* Query and print all posts

BTW, the `Comment` related changes are similar, check out the source codes from my [Github](https://github.com/hantsy/helidon-sandbox/tree/master/se-dbclient) and explore it yourself.

## Running the Application

To run this application, I prepare a *docker-compose.yaml* to bootstrap a PostgreSQL in docker quickly.

```yaml
version: '3.7' # specify docker-compose version

services:
  postgres:
    image: postgres
    restart: always
    ports:
      - "5432:5432"
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: test
      POSTGRES_USER: user
    volumes:
      - ./data/postgresql:/var/lib/postgresql
      - ./pg-initdb.d:/docker-entrypoint-initdb.d
```

And the table schema is defined a script file in the *pg-initdb.d* folder. 

```sql
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS posts(
id UUID DEFAULT uuid_generate_v4() ,
title VARCHAR(255) NOT NULL,
content VARCHAR(255) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP ,
PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS comments(
id UUID DEFAULT uuid_generate_v4() ,
post_id UUID NOT NULL,
content VARCHAR(255) NOT NULL,
created_at TIMESTAMP NOT NULL DEFAULT LOCALTIMESTAMP ,
PRIMARY KEY (id),
FOREIGN KEY (post_id) REFERENCES posts(id)
);

```

Run the following command to start a PostgreSQL server.

```bash
docker-compose up postgres
```

Now you can build and run the application by command.

```bash
mvn clean package
java -jar ./target/se-dbclient.jar
```

Or run the `Main` as java application in your favorite IDEs.

When it is started, you will see the following info in the console.

```bash
2020.02.29 00:15:02 INFO demo.DataInitializer Thread[helidon-3,5,helidon-thread-pool-1]: 0 comments deleted.
2020.02.29 00:15:02 INFO io.helidon.common.HelidonFeatures Thread[main,5,main]: Helidon SE 2.0.0-M1 features: [Config, DbClient, Health, Metrics, WebServer]
2020.02.29 00:15:02 INFO io.helidon.common.HelidonFeatures Thread[main,5,main]: Detailed feature tree:
Config
  YAML
DbClient
  HealthCheck
  JDBC
  Metrics
  Tracing
Health
  Built-ins
Metrics
WebServer
  JSON-P
2020.02.29 00:15:02 INFO demo.DataInitializer Thread[helidon-3,5,helidon-thread-pool-1]: 2 posts deleted.
2020.02.29 00:15:02 INFO demo.DataInitializer Thread[helidon-3,5,helidon-thread-pool-1]: 2 posts inserted.
2020.02.29 00:15:02 INFO demo.DataInitializer Thread[helidon-9,5,helidon-thread-pool-1]: found posts: [{created_at:2020-02-29 00:15:02.059223,id:a6201629-bd3b-4c0f-a8df-a94a192f5a02,title:My first post of Helidon,content:The content of my first post}, {created_at:2020-02-29 00:15:02.059223,id:52cdba76-0e18-47e8-8722-453f23bed35a,title:My second post of Helidon,content:The content of my second post}].
2020.02.29 00:15:06 INFO io.helidon.webserver.NettyWebServer Thread[nioEventLoopGroup-2-1,10,main]: Channel '@default' started: [id: 0x9521b1ed, L:/0:0:0:0:0:0:0:0:8080]
WEB server is up! http://localhost:8080/greet

```

Open a terminal, try to test the `/posts` endpoint by `curl` command.

```bash
# curl http://localhost:8080/posts/
[{"id":"52cdba76-0e18-47e8-8722-453f23bed35a","title":"My second post of Helidon","content":"The content of my second post","createdAt":"2020-02-29T00:15:02.059223"},{"id":"a6201629-bd3b-4c0f-a8df-a94a192f5a02","title":"my test title","content":"my content","createdAt":"2020-02-29T00:15:02.059223"}]
```

Grab the source codes from my [Github](https://github.com/hantsy/helidon-sandbox/tree/master/se-dbclient).

