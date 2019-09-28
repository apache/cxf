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
package org.apache.cxf.jaxrs.ext.search.visitor;

import java.util.Map;

public abstract class AbstractUntypedSearchConditionVisitor<T, E> extends AbstractSearchConditionVisitor<T, String> {

    private VisitorState<StringBuilder> state = new LocalVisitorState<>();

    protected AbstractUntypedSearchConditionVisitor(Map<String, String> fieldMap) {
        super(fieldMap);
    }

    public void setVisitorState(VisitorState<StringBuilder> s) {
        this.state = s;
    }

    public VisitorState<StringBuilder> getVisitorState() {
        return this.state;
    }

    protected String getPropertyValue(String name, Object value) {
        return value.toString();
    }

    protected StringBuilder getStringBuilder() {
        return getVisitorState().get();
    }

    protected StringBuilder removeStringBuilder() {
        return getVisitorState().remove();
    }

    protected void saveStringBuilder(StringBuilder sb) {
        getVisitorState().set(sb);
    }

    public String getQuery() {
        StringBuilder sb = removeStringBuilder();
        return sb == null ? null : sb.toString();
    }
}
