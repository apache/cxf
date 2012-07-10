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

/**
 * Represents the current search expression.
 */
public interface SearchContext {
    
    /**
     * Returns the typed search condition representing 
     * the search expression which is extracted from 
     * the request URI
     * 
     * @param cls the type of the bean(s) the new search condition will 
     *        attempt to match
     * @return the search condition
     */
    <T> SearchCondition<T> getCondition(Class<T> cls);
    
    
    /**
     * Returns the typed search condition representing 
     * the provided search expression
     * 
     * @param expression the search expression
     * @param cls the type of the bean(s) the new search condition will 
     *        attempt to match
     * @return the search condition
     */
    <T> SearchCondition<T> getCondition(String expression, Class<T> cls);
    
    
    /**
     * Returns the search expression
     * @return the expression which is extracted from 
     *         the request URI, can be null
     */
    String getSearchExpression();
}
