package com.example;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class PostRepository {

    @PersistenceContext
    EntityManager entityManager;

    public List<Post> findAll() {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create query
        CriteriaQuery<Post> query = cb.createQuery(Post.class);
        // set the root class
        Root root = query.from(Post.class);
        //perform query
        return this.entityManager.createQuery(query).getResultList();
    }


    public List<Post> findByKeyword(String q, int offset, int limit) {

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create query
        CriteriaQuery<Post> query = cb.createQuery(Post.class);
        // set the root class
        Root root = query.from(Post.class);

        if (q != null && !q.trim().isEmpty()) {
            query.where(
                    cb.or(
                            cb.like(root.get(Post_.title), q),
                            cb.like(root.get(Post_.content), q)
                    )
            );
        }
        //perform query
        return this.entityManager.createQuery(query)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public Optional<Post> getById(String id) {
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
    public void deleteById(String id) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        // create delete
        CriteriaDelete<Post> delete = cb.createCriteriaDelete(Post.class);
        // set the root class
        Root root = delete.from(Post.class);
        // set where clause
        delete.where(cb.equal(root.get(Post_.id), id));
        // perform update
        this.entityManager.createQuery(delete).executeUpdate();
    }
}
