package demo;

import io.helidon.dbclient.DbMapper;
import io.helidon.dbclient.spi.DbMapperProvider;

import javax.annotation.Priority;
import java.util.Optional;

@Priority(1000)
public class CommentMapperProvider implements DbMapperProvider {
    private static final CommentMapper MAPPER = new CommentMapper();

    @Override
    public <T> Optional<DbMapper<T>> mapper(Class<T> type) {
        if (type.equals(Comment.class)) {
            return Optional.of((DbMapper<T>) MAPPER);
        }
        return Optional.empty();
    }
}
