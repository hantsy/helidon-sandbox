package com.example;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Set;

@ApplicationScoped
@ApplicationPath("/")
public class JaxrsActivator extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Set.of(
                GreetResource.class,
                PostResource.class,
                CommentResource.class,
                PostNotFoundExceptionMapper.class,
                ConstraintViolationExceptionMapper.class
        );
    }
}
