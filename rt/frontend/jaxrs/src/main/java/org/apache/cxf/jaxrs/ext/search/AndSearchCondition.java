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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite 'and' search condition   
 */
public class AndSearchCondition<T> implements SearchCondition<T> {

    private List<SearchCondition<T>> conditions;
    
    public AndSearchCondition() {
        
    }
    
    public AndSearchCondition(List<SearchCondition<T>> conditions) {
        this.conditions = conditions;    
    }
    
    public void setConditions(List<SearchCondition<T>> conditions) {
        this.conditions = conditions;
    }
    
    public boolean isMet(T pojo) {
        for (SearchCondition<T> sc : conditions) {
            if (!sc.isMet(pojo)) {
                return false;
            }
        }
        return true;
    }

    public T getCondition() {
        return null;
    }

    public ConditionType getConditionType() {
        return ConditionType.AND;
    }

    public List<SearchCondition<T>> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public List<T> findAll(List<T> pojos) {
        List<T> result = new ArrayList<T>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }
}
