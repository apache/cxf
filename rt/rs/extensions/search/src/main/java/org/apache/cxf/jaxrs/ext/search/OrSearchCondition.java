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

/**
 * Composite 'or' search condition
 */
public class OrSearchCondition<T> extends AbstractComplexCondition<T> {

    public OrSearchCondition() {
        super(ConditionType.OR);
    }

    public OrSearchCondition(List<SearchCondition<T>> conditions) {
        super(conditions, ConditionType.OR);
    }


    public boolean isMet(T pojo) {
        for (SearchCondition<T> sc : conditions) {
            if (sc.isMet(pojo)) {
                return true;
            }
        }
        return false;
    }

}
