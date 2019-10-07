package com.example;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Optional;

public interface Repository<E, ID> {

    EntityManager entityManager();

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

