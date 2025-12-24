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

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.collections.CollectionCheckInfo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

public class CustomJPACriteriaVisitor extends JPACriteriaQueryVisitor<Book, Book> {

	boolean customPredicateUsed = false;

	CustomJPACriteriaVisitor(EntityManager em) {
		super(em, Book.class, Book.class);
	}

	@Override
	protected Predicate doBuildPredicate(ConditionType ct, Path<?> path, Class<?> valueClazz, Object value) {

		if ("bookTitle".equals(path.getAlias())) {
			return getCriteriaBuilder().like(path.as(String.class), "%" + value + "%");
		}

		return super.doBuildPredicate(ct, path, valueClazz, value);
	}

	@Override
	protected Predicate doBuildCollectionPredicate(ConditionType ct, Path<?> path, CollectionCheckInfo collInfo) {
		return getCriteriaBuilder().disjunction();
	}
}
