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

import javax.persistence.EntityManager;
import javax.persistence.criteria.CompoundSelection;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Selection;
import javax.persistence.metamodel.SingularAttribute;

public class JPACriteriaQueryVisitor<T, E> extends AbstractJPATypedQueryVisitor<T, E, CriteriaQuery<E>> {

    public JPACriteriaQueryVisitor(EntityManager em, 
                                   Class<T> tClass,
                                   Class<E> queryClass) {
        this(em, tClass, queryClass, null);
    }
    
    public JPACriteriaQueryVisitor(EntityManager em, 
                                   Class<T> tClass,
                                   Class<E> queryClass,
                                   Map<String, String> fieldMap) {
        super(em, tClass, queryClass, fieldMap);
    }
    
    public CriteriaQuery<E> getQuery() {
        return getCriteriaQuery();
    }
        
    public CriteriaQuery<E> selectArray(List<SingularAttribute<T, ?>> attributes) {
        return selectArraySelections(toSelectionsArray(toSelectionsList(attributes)));
    }
    
    private CriteriaQuery<E> selectArraySelections(Selection<?>... selections) {
        @SuppressWarnings("unchecked")
        CompoundSelection<E> selection = (CompoundSelection<E>)getCriteriaBuilder().array(selections);
        getQuery().select(selection);
        return getQuery();
    }
    
    public CriteriaQuery<E> selectConstruct(List<SingularAttribute<T, ?>> attributes) {
        return selectConstructSelections(toSelectionsArray(toSelectionsList(attributes)));
    }
    
    private CriteriaQuery<E> selectConstructSelections(Selection<?>... selections) {
        getQuery().select(getCriteriaBuilder().construct(getQueryClass(), selections));
        return getQuery();
    }
    
    public CriteriaQuery<E> selectTuple(List<SingularAttribute<T, ?>> attributes) {
        return selectTupleSelections(toSelectionsArray(toSelectionsList(attributes)));
    }
    
    private CriteriaQuery<E> selectTupleSelections(Selection<?>... selections) {
        @SuppressWarnings("unchecked")
        CompoundSelection<E> selection = 
            (CompoundSelection<E>)getCriteriaBuilder().tuple(selections);
        getQuery().select(selection);
        return getQuery();
    }
    
    private List<Selection<?>> toSelectionsList(List<SingularAttribute<T, ?>> attributes) {
        List<Selection<?>> selections = new ArrayList<Selection<?>>(attributes.size());
        for (SingularAttribute<T, ?> attr : attributes) {
            selections.add(getRoot().get(attr));
        }
        return selections;
    }
    
    private static Selection<?>[] toSelectionsArray(List<Selection<?>> selections) {
        return selections.toArray(new Selection[]{});
    }
    
}
