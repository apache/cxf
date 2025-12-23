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
