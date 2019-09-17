```bash
mvn archetype:generate \
    -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-mp \
    -DarchetypeVersion=1.3.0 \
    -DgroupId=com.example \
    -DartifactId=mp-jpa \
    -Dpackage=com.example \
    -DrestResourceName=PostResource \
    -DapplicationName=Application
```



Add Jdbc driver,



Add the Hikari Connection Pool Extension 

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-datasource-ikaricp</artifactId>
    <scope>runtime</scope>
</dependency>
```
Add the JTA Extension

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-jta-weld</artifactId>
    <scope>runtime</scope>
</dependency>
```
Add the Provider-Independent Helidon JPA Extension
```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-jpa</artifactId>
    <scope>runtime</scope>
</dependency>
```
Add the EclipseLink JPA Extension

```xml
<dependency>
    <groupId>io.helidon.integrations.cdi</groupId>
    <artifactId>helidon-integrations-cdi-eclipselink</artifactId>
    <scope>runtime</scope>
</dependency>
```
Add the JTA and JPA Dependencies to the Provided Classpath
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