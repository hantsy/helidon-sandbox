package com.example;

import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.repository.CrudRepository;

import java.util.UUID;

@JdbcRepository
public interface PostRepository extends CrudRepository<Post, UUID> {

}
