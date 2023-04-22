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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class JPATypedQueryVisitor<T> extends AbstractJPATypedQueryVisitor<T, T, TypedQuery<T>> {

    public JPATypedQueryVisitor(EntityManager em, Class<T> tClass) {
        this(em, tClass, null, null);
    }

    public JPATypedQueryVisitor(EntityManager em, Class<T> tClass, Map<String, String> fieldMap) {
        super(em, tClass, fieldMap);
    }

    public JPATypedQueryVisitor(EntityManager em,
                                Class<T> tClass,
                                List<String> joinProps) {
        super(em, tClass, null, joinProps);
    }

    public JPATypedQueryVisitor(EntityManager em,
                                Class<T> tClass,
                                String joinProp) {
        super(em, tClass, null, Collections.singletonList(joinProp));
    }

    public JPATypedQueryVisitor(EntityManager em,
                                Class<T> tClass,
                                Map<String, String> fieldMap,
                                List<String> joinProps) {
        super(em, tClass, fieldMap, joinProps);
    }

    public TypedQuery<T> getQuery() {
        return getTypedQuery();
    }

}
