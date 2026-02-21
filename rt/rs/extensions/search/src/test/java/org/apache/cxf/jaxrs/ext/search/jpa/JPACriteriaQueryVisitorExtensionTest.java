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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchConditionParser;
import org.apache.cxf.jaxrs.ext.search.fiql.FiqlParser;
import org.junit.Test;

import jakarta.persistence.criteria.CriteriaQuery;

public class JPACriteriaQueryVisitorExtensionTest extends AbstractJPATypedQueryVisitorTest {

	@Test
	public void testCustomPredicateExtensionIsUsed() throws Exception {
		SearchCondition<Book> filter = getParser().parse("bookTitle==NUM9");

		JPACriteriaQueryVisitor<Book, Book> visitor = new CustomJPACriteriaVisitor(getEntityManager());

		filter.accept(visitor);

		List<Book> result = getEntityManager().createQuery(visitor.getQuery()).getResultList();

		assertEquals(1, result.size());
	}

	@Test
	public void testCollectionPredicateOverrideIsUsed() throws Exception {
		SearchCondition<Book> filter = getParser().parse("authors==John");

		JPACriteriaQueryVisitor<Book, Book> visitor = new CustomJPACriteriaVisitor(getEntityManager());

		filter.accept(visitor);

		CriteriaQuery<Book> query = visitor.getQuery();
		List<Book> results = getEntityManager().createQuery(query).getResultList();

		// Without override -> 2 results
		// With override -> 0 results
		assertTrue(results.isEmpty());
	}

	@Override
	protected SearchConditionParser<Book> getParser() {
		return new FiqlParser<>(Book.class);
	}

	@Override
	protected SearchConditionParser<Book> getParser(Map<String, String> visitorProps,
			Map<String, String> parserBinProps) {
		return new FiqlParser<>(Book.class, parserBinProps);
	}

}
