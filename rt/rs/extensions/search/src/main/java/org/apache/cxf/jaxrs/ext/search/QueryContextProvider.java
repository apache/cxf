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
package org.apache.cxf.jaxrs.ext.search;

import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.message.Message;

@Provider
public class QueryContextProvider implements ContextProvider<QueryContext> {

    public QueryContext createContext(Message message) {
        return new QueryContextImpl(message);
    }

    private static class QueryContextImpl implements QueryContext {

        private SearchContext searchContext;
        private Message message;
        QueryContextImpl(Message message) {
            this.searchContext = new SearchContextImpl(message);
            this.message = message;
        }

        @Override
        public <T> String getConvertedExpression(Class<T> beanClass) {
            return getConvertedExpression((String)null, beanClass);
        }

        @Override
        public <T, E> E getConvertedExpression(Class<T> beanClass, Class<E> queryClass) {
            return getConvertedExpression((String)null, beanClass, queryClass);
        }

        @Override
        public <T> String getConvertedExpression(String originalExpression, Class<T> beanClass) {
            return getConvertedExpression(originalExpression, beanClass, String.class);

        }

        @Override
        public <T, E> E getConvertedExpression(String originalExpression,
                                               Class<T> beanClass,
                                               Class<E> queryClass) {
            SearchConditionVisitor<T, E> visitor = getVisitor();
            if (visitor == null) {
                return null;
            }

            SearchCondition<T> cond = searchContext.getCondition(originalExpression, beanClass);
            if (cond == null) {
                return null;
            }
            cond.accept(visitor);
            return queryClass.cast(visitor.getQuery());

        }

        @SuppressWarnings("unchecked")
        private <T, Y> SearchConditionVisitor<T, Y> getVisitor() {
            Object visitor = message.getContextualProperty(SearchUtils.SEARCH_VISITOR_PROPERTY);
            if (visitor == null) {
                return null;
            }
            //TODO: consider introducing SearchConditionVisitor.getBeanClass &&
            //      SearchConditionVisitor.getQueryClass to avoid such casts
            return (SearchConditionVisitor<T, Y>)visitor;
        }


    }
}
