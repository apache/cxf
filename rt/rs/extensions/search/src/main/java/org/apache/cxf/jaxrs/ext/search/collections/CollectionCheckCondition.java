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
package org.apache.cxf.jaxrs.ext.search.collections;

import java.lang.reflect.Type;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveSearchCondition;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;

public class CollectionCheckCondition<T> extends PrimitiveSearchCondition<T> {

    private CollectionCheckInfo checkInfo;
    public CollectionCheckCondition(String propertyName,
                                    Object propertyValue,
                                    Type propertyType,
                                    ConditionType ct,
                                    T condition,
                                    CollectionCheckInfo checkInfo) {
        super(propertyName, propertyValue, propertyType, ct, condition);
        this.checkInfo = checkInfo;
    }
    public CollectionCheckInfo getCollectionCheckInfo() {
        return checkInfo;
    }
    public PrimitiveStatement getStatement() {
        return new CollectionCheckStatement(getPropertyName(),
                                            getPropertyValue(),
                                            getPropertyType(),
                                            getConditionType(),
                                            checkInfo);
    }

}
