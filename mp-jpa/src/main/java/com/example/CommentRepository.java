package com.example;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaDelete;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.List;

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
