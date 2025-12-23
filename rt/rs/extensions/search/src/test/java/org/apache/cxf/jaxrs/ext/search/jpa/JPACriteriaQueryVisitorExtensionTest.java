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
