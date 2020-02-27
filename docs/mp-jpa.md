
# Data Persistence with JPA

Helidon 1.3 brings plenty of new features, there are some highlights:

* Add support of Microprofile 3.0
*  Stabilizes JPA support in Helidon  MP
*  etc.

In this post, we will continue the work we have done in the post [Getting Started with Helidon MP](./GS-MP.md), and add  JPA support for performing CRUD functionality. 

The source code is available on [Github](https://github.com/hantsy/helidon-sample).

## Create a Helidon MP project

At the moment I wrote down this post, Helidon still does not provide an interactive interface like [Spring Intializr](https://start.spring.io) or [Quarkus Coding](https://code.quarkus.io)  for developers to generate the project skeleton. 

You can reuse the code base of the last post. Or like what we have done in the former post, generate a new project from the existing Helidon Maven archetype.

```bash
mvn archetype:generate \
    -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-mp \
    -DarchetypeVersion=1.3.1 \
    -DgroupId=com.example \
    -DartifactId=mp-jpa \
    -Dpackage=com.example \
    -DrestResourceName=PostResource \
    -DapplicationName=JaxrsActivator
```

> Note,  there was an issue in version 1.3.0 which blocked a MP application to run successfully under Windows, it is fixed in version 1.3.1,  see [#1038](https://github.com/oracle/helidon/issues/1038). 



Import the codes into your IDE, such as Intellij IDEA, Apache NetBeans IDE or Eclipse JEE bundle . 

## Enabling JPA Support 

There are some steps required to contribute JPA support into an existing Helidon MP project.

First of all, we need to add a Jdbc driver in dependencies for the database we are using in this project. 

```xml
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<version>42.2.7</version>
</dependency>
```

As an example, we use PostgreSQL for test purpose.

Next,  make sure there is a running PostgresSQL server when you are running this application. 

You can install a PostgreSQL server into your local system, start it.  Or run a PostgreSQL server in docker container. 

There is a docker-compose file available in  [the project repository](https://github.com/hantsy/helidon-sample) which allow you run a PostgreSQl in Docker container in seconds.

```yaml
version: '3.7' # specify docker-compose version

services:
  blogdb:
    image: postgres
    ports:
      - "5432:5432"
    restart: always
    environment:
      POSTGRES_PASSWORD: password
      POSTGRES_DB: blogdb
      POSTGRES_USER: user
    volumes:
      - ./data:/var/lib/postgresql
```

Try to execute `docker-compose up` to bootstrap a PostgreSQL server .

Helidon provides a `DataSource` extension to produce `javax.sql.DataSource` CDI bean  at runtime. And for optimizing database connection, there is another Hikari Connection Pool Extension available.

Let's add the Hikari Connection Pool Extension into the project dependencies, it will includes the `DataSource` extension transitively. 

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-datasource-ikaricp</artifactId>
    <scope>runtime</scope>
</dependency>
```
Then configure `DataSource` properties  in the *application.yaml* file like this. 

```yaml
javax:
  sql:
    DataSource:
      blogDataSource:
        dataSourceClassName: org.postgresql.ds.PGSimpleDataSource
        dataSource:
		  #  https://github.com/brettwooldridge/HikariCP
          url: jdbc:postgresql://localhost:5432/blogdb
          user: user
          password: password
```

The  `DataSource` properties is grouped under the `javax.sql.DataSource`  node.  `blogDataSource`  is the qualifier name of a  `DataSource` .  `dataSourceClassName` is required by [HikariCP](https://github.com/brettwooldridge/HikariCP). The properties under `dataSource` node are general properties required to define a  `DataSource`. 

You can declare  multi `DataSource`s like the following:

```yaml
javax:
  sql:
    DataSource:
      blogDataSource:
      ...
      userDataSource:
      ...

```

And in your codes,  use `@Inject` with `@Named` to identify different `DataSource`s.

```java
@Inject
@Named("blogDataSource")
DataSource blogDataSource;

@Inject
@Named("userDataSource")
DataSource userDataSource;

```
To enable CDI aware `@Transactional`, add the JTA Extension into the dependencies.

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-jta-weld</artifactId>
    <scope>runtime</scope>
</dependency>
```
The following code snippets shows an example of `@Trasactional`.

```java
@Trasactional
public void placeOrder(){}
```

Next, we will add JPA  support into the project.

Add the Provider-Independent Helidon JPA Extension into dependencies.

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-jpa</artifactId>
    <scope>runtime</scope>
</dependency>
```
There are a few JPA providers available, such as EclipseLink, Hibernate, OpenJPA etc.  Helidon provides outbox support of EclipesLink and Hibernate.  

Let's use EclipseLink  as an example. Add the EclipseLink JPA Extension into dependencies.

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-eclipselink</artifactId>
    <scope>runtime</scope>
</dependency>
```
And create a `persistence.xml` under *src/resources/META-INF/* to configure EclipseLink and JPA properties.

```properties
<?xml version="1.0" encoding="UTF-8"?>
<persistence version="2.2"
             xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
                                 http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd">
    <persistence-unit name="greeting" transaction-type="JTA">
        <description>A persistence unit for the greeting example.</description>
        <jta-data-source>blogDataSource</jta-data-source>
        <properties>
            <property name="eclipselink.deploy-on-startup" value="true"/>
            <property name="eclipselink.jdbc.native-sql" value="true"/>
            <property name="eclipselink.logging.logger" value="JavaLogger"/>
            <property name="eclipselink.logging.parameters" value="true"/>
            <property name="eclipselink.target-database" value="org.eclipse.persistence.platform.database.PostgreSQLPlatform"/>
            <property name="eclipselink.target-server" value="io.helidon.integrations.cdi.eclipselink.CDISEPlatform"/>
            <property name="eclipselink.weaving" value="false"/>
        </properties>
    </persistence-unit>
</persistence>
```

`<jta-data-source/>` specifies which DataSource will be used.  Note here, the value of `eclipselink.target-server`  property is configured as `io.helidon.integrations.cdi.eclipselink.CDISEPlatform` to use a Helidon specific implementation here. 

Now,  in your codes use `@PersistenceContex` to inject `EntityManager` like we are doing in the Java EE applications.

```java
@PersistenceContext
EntityManager entityManager;
```

We declares these dependencies as *runtime* scope,  you need add  the JTA and JPA Dependencies to the *provided* scope to use those APIs freely.

```xml
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <version>2.2.2</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>javax.transaction</groupId>
    <artifactId>javax.transaction-api</artifactId>
    <version>1.2</version>
    <scope>provided</scope>
</dependency>
```

Finally, add `eclipselink-maven-plugin` to instrument JPA codes at compile time and also generate JPA metadata classes for type safe queries.

```xml
<plugin>
    <groupId>com.ethlo.persistence.tools</groupId>
    <artifactId>eclipselink-maven-plugin</artifactId>
    <version>2.7.4</version>
    <dependencies>
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>javax.annotation-api</artifactId>
            <version>1.3.1</version>
        </dependency>
        <dependency>
            <groupId>javax.xml.bind</groupId>
            <artifactId>jaxb-api</artifactId>
            <version>2.3.0</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <id>weave</id>
            <phase>process-classes</phase>
            <goals>
                <goal>weave</goal>
            </goals>
        </execution>
        <execution>
            <id>modelgen</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>modelgen</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

If you are using IDEA, to get the generated metadata classes instantly at you are coding, add the following dependency.

```xml
<!-- https://mvnrepository.com/artifact/org.eclipse.persistence/org.eclipse.persistence.jpa.modelgen.processor -->
<dependency>
    <groupId>org.eclipse.persistence</groupId>
    <artifactId>org.eclipse.persistence.jpa.modelgen.processor</artifactId>
    <version>2.7.4</version>
    <scope>provided</scope>
</dependency>
```

And  go to File->Settings, expand *Build, Execution, Deployment*->  *Compiler*,  select *Annotation processors*, enable *Annotation processing* by click the checkbox, see the [IDEA docs](https://www.jetbrains.com/help/idea/configuring-annotation-processing.html) for more info.

Great, the configuration work is done, let's migrate our codes to use JPA for performing CRUD functionality.

## Migrating Codebase to JPA

Firstly have a look at `Post` , which is annotated with an `@Entity` annotation and indicates it is a JPA entity class.

```java
@Entity
public class Post implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator(name = "UUID")
    String id;
    String title;
    String content;
    LocalDateTime createdAt;
    // getters and setters, equals, hashCode, toString
}    
```

In the `Post` entity, a  `@Id`  property is used to specify the identifier of an Entity,  and the `@GeneratedValue` annotation is to declare the Id generating rule, here we use a generator named **UUID**, it is defined by `@UuidGenerator`  which is a custom UUID generator from EclipseLink.

Next let's deal with the `PostRepository` class.

```java
@ApplicationScoped
public class PostRepository {

    @PersistenceContext
    EntityManager entityManager;

    public List<Post> findAll() {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create query
        CriteriaQuery<Post> query = cb.createQuery(Post.class);
        // set the root class
        Root<Post> root = query.from(Post.class);
        //perform query
        return this.entityManager.createQuery(query).getResultList();
    }

    public Optional<Post> findById(String id) {
        Post post = null;
        try {
            post = this.entityManager.find(Post.class, id);
        } catch (NoResultException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(post);
    }

    @Transactional
    public Post save(Post post) {
        if (post.getId() == null) {
            this.entityManager.persist(post);
            return post;
        } else {
            return this.entityManager.merge(post);
        }
    }
    
    @Transactional
    public int updateStatus(String id, Post.Status status) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create update
        CriteriaUpdate<Post> delete = cb.createCriteriaUpdate(Post.class);
        // set the root class
        Root<Post> root = delete.from(Post.class);
        // set where clause
        delete.set(root.get(Post_.status), status);
        delete.where(cb.equal(root.get(Post_.id), id));
        // perform update
        return this.entityManager.createQuery(delete).executeUpdate();
    }


    @Transactional
    public int deleteById(String id) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create delete
        CriteriaDelete<Post> delete = cb.createCriteriaDelete(Post.class);
        // set the root class
        Root<Post> root = delete.from(Post.class);
        // set where clause
        delete.where(cb.equal(root.get(Post_.id), id));
        // perform update
        return this.entityManager.createQuery(delete).executeUpdate();
    }
}

```

In the above code snippets, we use the type-safe JPA Criteria APIs to perform CRUD operations. 

Similarly, refactor `Comment` and `CommentRepository`  codes.

```java
@Entity
public class Comment implements Serializable {

    @Id
    @GeneratedValue(generator = "UUID")
    @UuidGenerator(name = "UUID")
    private String id;

    @Embedded
    @AttributeOverride(
            name = "id",
            column = @Column(name = "post_id")
    )
    private PostId post;
    private String content;
    private LocalDateTime createdAt;
    // getters and setters, equals, hashCode, toString
}   
```

The `PostId` is a reference  to a `Post`, which follows the DDD modeling.

```java 
@Embeddable
public class PostId implements Serializable {
    private String id;
    // getters and setters, equals, hashCode, toString
}
```

And the `CommentRepository` is changed to the following.

```java
@ApplicationScoped
public class CommentRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public Comment save(Comment comment) {
        if (comment.getId() == null) {
            this.entityManager.persist(comment);
            return comment;
        } else {
            return this.entityManager.merge(comment);
        }
    }

    @Transactional
    public void deleteById(String id) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create delete
        CriteriaDelete<Comment> delete = cb.createCriteriaDelete(Comment.class);
        // set the root class
        Root<Comment> root = delete.from(Comment.class);
        // set where clause
        delete.where(cb.equal(root.get(Comment_.id), id));
        // perform update
        this.entityManager.createQuery(delete).executeUpdate();
    }

    public List<Comment> findByPostId(String id) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create query
        CriteriaQuery<Comment> query = cb.createQuery(Comment.class);
        // set the root class
        Root<Comment> root = query.from(Comment.class);
        query.where(cb.equal(root.get(Comment_.post).get(PostId_.id), id));
        //perform query
        return this.entityManager.createQuery(query).getResultList();
    }
}

```

Almost done, one last thing, do not forget to declare the `Entity` and `Embeddable` classes in the JPA *persistence.xml*.  It is required when using JPA in a Java SE application, and Helidon also do not handle these for developers. 

```xml
<persistence ...>
    <persistence-unit ...>
        <class>com.example.Post</class>
        <class>com.example.PostId</class>
        <class>com.example.Comment</class>
        //...
    </persistence-unit>
</persistence>
```

Make sure the database is running, and now you can run the application.

```bash
mvn clean package
java -jar target/mp-jpa.jar
```



## Initializing sample data

Create an  `ApplicationScoped` bean to listen CDI `@Initialized(ApplicationScoped.class)` event.  Inject `PostRepository` bean, and insert some data at the application initialization stage.

```java
@ApplicationScoped
public class AppInitializer {
    private final static Logger LOGGER = Logger.getLogger(AppInitializer.class.getName());

    @Inject
    private PostRepository posts;

    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object init) {
        LOGGER.info("The application is starting...");
        Post first = Post.of("Hello Helidon", "My first post of Helidon");
        Post second = Post.of("Hello Again, Helidon", "My second post of Helidon");

        this.posts.save(first);
        this.posts.save(second);

        this.posts.findAll().forEach(p -> System.out.println("Post:" + p));
    }

    void onStop(@Observes @Destroyed(ApplicationScoped.class) Object init) {
        LOGGER.info("The application is stopping...");
    }
}
```

Run the application again, try to access the `/posts` endpoints by `curl`.

```bash
> curl http://localhost:8080/posts
[{"content":"My first post of Helidon","createdAt":"2019-10-14T08:37:40.560054","id":"7B7479F5-9773-47A2-845A-524D151A73E5","title":"Hello Helidon"},{"content":"My second post of Helidon","createdAt":"2019-10-14T08:37:40.560054","id":"2BC4C17B-303A-4799-A229-11ED20A8D67F","title":"Hello Again, Helidon"}]
```



## Bonus 

If you have some experience of [Spring Data](https://spring.io/projects/spring-data) and [Apache DeltaSpike](https://deltaspike.apache.org), you may be heavily impressed by their `Repository` which drastically simplifies the Repository codes.  Let's have a look at the codes of `PostRepository`  and `CommentRepository` , maybe you have realized some code snippets are very similar.  

Try to extract a general purpose `Respository` and allow all `Repositories` inherit from it. 

```java
public interface Repository<E, ID> {

    abstract EntityManager entityManager();

    private Class<E> entityClazz() {
        return (Class<E>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    public default List<E> findAll() {
        CriteriaBuilder cb = this.entityManager().getCriteriaBuilder();
        // create query
        CriteriaQuery<E> query = cb.createQuery(this.entityClazz());
        // set the root class
        Root<E> root = query.from(this.entityClazz());
        //perform query
        return this.entityManager().createQuery(query).getResultList();
    }

    public default E findById(ID id) {
        E entity = null;
        try {
            entity = this.entityManager().find(this.entityClazz(), id);
        } catch (NoResultException e) {
            e.printStackTrace();
        }
        return entity;
    }

    public default Optional<E> findOptionalById(ID id) {
        E entity = null;
        try {
            entity = this.entityManager().find(this.entityClazz(), id);
        } catch (NoResultException e) {
            e.printStackTrace();
        }
        return Optional.ofNullable(entity);
    }

    @Transactional
    public default E save(E entity) {
        if (this.entityManager().contains(entity)) {
            return this.entityManager().merge(entity);
        } else {
            this.entityManager().persist(entity);
            return entity;
        }
    }

    @Transactional
    public default void deleteById(ID id) {
        E entity = this.findById(id);
        this.entityManager().remove(entity);
    }
}
```

Ensure your `Repository`  is a sub class of this `Repository`.  The following is an example.

```java
@ApplicationScoped
public class OrderRepository implements Repository<Order, Long>{
    @PersistenceContext
    EntityManager entityManager;
    
    @override
    public EntityManager entityManager(){
        return this.entityManager;
    }
}
```

Hope Helidon will provide a mature solution to simplify the Repository codes. 

