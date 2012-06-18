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

import java.util.Collection;
import java.util.List;

//CHECKSTYLE:OFF
/**
 * Can be used to build plain or complex/composite search conditions.
 * <p>
 * Google Collections <a href="http://google-collections.googlecode.com/svn/trunk/javadoc/com/google/common/base/Predicate.html">Predicate</a>
 * might've been used instead, but it is a too generic and its apply method is not quite needed here
 * </p>
 *  
 * @param <T> Type of the object which will be checked by SearchCondition instance
 *  
 */
//CHECKSTYLE:ON
public interface SearchCondition<T> {
    
    /**
     * Checks if the given pojo instance meets this search condition
     * 
     * @param pojo the object which will be checked
     * @return true if the pojo meets this search condition, false - otherwise
     */
    boolean isMet(T pojo);
    
    /**
     * Returns a list of pojos matching the condition
     * @param pojos list of pojos
     * @return list of the matching pojos or null if none have been found
     */
    List<T> findAll(Collection<T> pojos);
    
    /**
     * Some SearchConditions may use instance of T to capture the actual search criteria
     * thus making it simpler to implement isMet(T). In some cases, the code which is given
     * SearchCondition may find it more efficient to directly deal with the captured state
     * for a more efficient lookup of matching data/records as opposed to calling
     * SearchCondition.isMet for every instance of T it knows about.
     * 
     *  
     * @return T the captured search criteria, can be null 
     */
    T getCondition();

    
    /**
     * Primitive statement such a > b, i < 5, etc
     * this condition may represent. Complex conditions will return null.  
     *  
     * @return primitive search statement, can be null 
     */
    PrimitiveStatement getStatement();
    
    /**
     * List of conditions this SearchCondition may represent.
     * Composite SearchConditions will return a list of conditions they are
     * composed from, primitive ones will return null  
     * @return list of conditions, can be null
     */
    List<SearchCondition<T>> getSearchConditions();
    
    /**
     * Returns the type of the condition this SearchCondition represents
     * @return condition type
     */
    ConditionType getConditionType();

    /**
     * Provides a visitor which will convert this SearchCondition into
     * a custom expression, for example, into the SQL statement, etc 
     * @param visitor
     */
    void accept(SearchConditionVisitor<T> visitor);
    
}
