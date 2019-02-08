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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class AbstractComplexCondition<T> implements SearchCondition<T> {

    protected List<SearchCondition<T>> conditions;
    private ConditionType cType;

    protected AbstractComplexCondition(ConditionType cType) {
        this.cType = cType;
    }

    protected AbstractComplexCondition(List<SearchCondition<T>> conditions, ConditionType cType) {
        this.conditions = conditions;
        this.cType = cType;
    }

    public void setConditions(List<SearchCondition<T>> conditions) {
        this.conditions = conditions;
    }

    public List<T> findAll(Collection<T> pojos) {
        List<T> result = new ArrayList<>();
        for (T pojo : pojos) {
            if (isMet(pojo)) {
                result.add(pojo);
            }
        }
        return result;
    }

    public T getCondition() {
        return null;
    }

    public ConditionType getConditionType() {
        return cType;
    }

    public List<SearchCondition<T>> getSearchConditions() {
        return Collections.unmodifiableList(conditions);
    }

    public PrimitiveStatement getStatement() {
        return null;
    }

    public String toSQL(String table, String... columns) {
        return SearchUtils.toSQL(this, table, columns);
    }

    public void accept(SearchConditionVisitor<T, ?> visitor) {
        visitor.visit(this);
    }

}
