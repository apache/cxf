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

import java.util.List;

public class SimpleSearchCondition<T> implements SearchCondition<T> {

    private ConditionType cType;
    private T condition;
    
    public SimpleSearchCondition(ConditionType cType, T condition) {
        this.cType = cType;
        this.condition = condition;
    }
    
    public T getCondition() {
        return condition;
    }

    public ConditionType getConditionType() {
        return cType;
    }

    public List<SearchCondition<T>> getConditions() {
        return null;
    }

    public boolean isMet(T pojo) {
        // not implemented yet
        
        // we need to get all getters from the condition
        // and then depending on ConditionType do equals, compare, etc against
        // corresponding values returned from pojo getters
        
        return false;
    }

    public List<T> findAll(List<T> pojos) {
        // TODO Auto-generated method stub
        return null;
    }

}
