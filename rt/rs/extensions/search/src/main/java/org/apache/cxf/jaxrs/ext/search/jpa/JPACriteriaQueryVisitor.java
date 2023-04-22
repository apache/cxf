/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cxf.jaxrs.ext.search.jpa;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CompoundSelection;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Selection;
import jakarta.persistence.metamodel.SingularAttribute;

public class JPACriteriaQueryVisitor<T, E> extends AbstractJPATypedQueryVisitor<T, E, CriteriaQuery<E>> {

    public JPACriteriaQueryVisitor(EntityManager em,
                                   Class<T> tClass,
                                   Class<E> queryClass) {
        this(em, tClass, queryClass, null, null);
    }

    public JPACriteriaQueryVisitor(EntityManager em,
                                   Class<T> tClass,
                                   Class<E> queryClass,
                                   List<String> joinProps) {
        this(em, tClass, queryClass, null, joinProps);
    }

    public JPACriteriaQueryVisitor(EntityManager em,
                                   Class<T> tClass,
                                   Class<E> queryClass,
                                   Map<String, String> fieldMap) {
        super(em, tClass, queryClass, fieldMap);
    }

    public JPACriteriaQueryVisitor(EntityManager em,
                                   Class<T> tClass,
                                   Class<E> queryClass,
                                   Map<String, String> fieldMap,
                                   List<String> joinProps) {
        super(em, tClass, queryClass, fieldMap, joinProps);
    }

    public CriteriaQuery<E> getQuery() {
        return getCriteriaQuery();
    }

    public Long count() {
        if (super.getQueryClass() != Long.class) {
            throw new IllegalStateException("Query class needs to be of type Long");
        }
        @SuppressWarnings("unchecked")
        CriteriaQuery<Long> countQuery = (CriteriaQuery<Long>)getCriteriaQuery();
        countQuery.select(getCriteriaBuilder().count(getRoot()));
        return super.getEntityManager().createQuery(countQuery).getSingleResult();
    }

    public TypedQuery<E> getOrderedTypedQuery(List<SingularAttribute<T, ?>> attributes, boolean asc) {
        CriteriaQuery<E> cQuery = orderBy(attributes, asc);
        return getTypedQuery(cQuery);
    }

    public CriteriaQuery<E> orderBy(List<SingularAttribute<T, ?>> attributes, boolean asc) {
        CriteriaBuilder cb = getCriteriaBuilder();

        List<Order> orders = new ArrayList<>();
        for (SingularAttribute<T, ?> attribute : attributes) {
            Path<?> selection = getRoot().get(attribute);
            Order order = asc ? cb.asc(selection) : cb.desc(selection);
            orders.add(order);
        }
        return getCriteriaQuery().orderBy(orders);
    }

    public TypedQuery<E> getArrayTypedQuery(List<SingularAttribute<T, ?>> attributes) {
        CriteriaQuery<E> cQuery = selectArraySelections(toSelectionsArray(toSelectionsList(attributes, false)));
        return getTypedQuery(cQuery);
    }

    public CriteriaQuery<E> selectArray(List<SingularAttribute<T, ?>> attributes) {
        return selectArraySelections(toSelectionsArray(toSelectionsList(attributes, false)));
    }

    private CriteriaQuery<E> selectArraySelections(Selection<?>... selections) {
        @SuppressWarnings("unchecked")
        CompoundSelection<E> selection = (CompoundSelection<E>)getCriteriaBuilder().array(selections);
        getQuery().select(selection);
        return getQuery();
    }

    public CriteriaQuery<E> selectConstruct(List<SingularAttribute<T, ?>> attributes) {
        return selectConstructSelections(toSelectionsArray(toSelectionsList(attributes, false)));
    }

    public TypedQuery<E> getConstructTypedQuery(List<SingularAttribute<T, ?>> attributes) {
        CriteriaQuery<E> cQuery = selectConstructSelections(toSelectionsArray(toSelectionsList(attributes, false)));
        return getTypedQuery(cQuery);
    }

    private CriteriaQuery<E> selectConstructSelections(Selection<?>... selections) {
        getQuery().select(getCriteriaBuilder().construct(getQueryClass(), selections));
        return getQuery();
    }

    public CriteriaQuery<E> selectTuple(List<SingularAttribute<T, ?>> attributes) {
        return selectTupleSelections(toSelectionsArray(toSelectionsList(attributes, true)));
    }

    public TypedQuery<E> getTupleTypedQuery(List<SingularAttribute<T, ?>> attributes) {
        CriteriaQuery<E> cQuery = selectTupleSelections(toSelectionsArray(toSelectionsList(attributes, true)));
        return getTypedQuery(cQuery);
    }

    private CriteriaQuery<E> selectTupleSelections(Selection<?>... selections) {
        @SuppressWarnings("unchecked")
        CompoundSelection<E> selection =
            (CompoundSelection<E>)getCriteriaBuilder().tuple(selections);
        getQuery().select(selection);
        return getQuery();
    }

    private List<Selection<?>> toSelectionsList(List<SingularAttribute<T, ?>> attributes, boolean setAlias) {
        List<Selection<?>> selections = new ArrayList<>(attributes.size());
        for (SingularAttribute<T, ?> attr : attributes) {
            Path<?> path = getRoot().get(attr);
            path.alias(attr.getName());
            selections.add(path);
        }
        return selections;
    }

    private static Selection<?>[] toSelectionsArray(List<Selection<?>> selections) {
        return selections.toArray(new Selection[0]);
    }

    private TypedQuery<E> getTypedQuery(CriteriaQuery<E> theCriteriaQuery) {
        return super.getEntityManager().createQuery(theCriteriaQuery);
    }
}
