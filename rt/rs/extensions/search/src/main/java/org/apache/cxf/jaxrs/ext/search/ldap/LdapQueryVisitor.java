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
package org.apache.cxf.jaxrs.ext.search.ldap;

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.jaxrs.ext.search.ConditionType;
import org.apache.cxf.jaxrs.ext.search.PrimitiveStatement;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.visitor.AbstractUntypedSearchConditionVisitor;
/**
 * Initial Implementation of http://tools.ietf.org/html/rfc4515
 */
public class LdapQueryVisitor<T> extends AbstractUntypedSearchConditionVisitor<T, String> {

    private boolean encodeQueryValues = true;

    public LdapQueryVisitor() {
        this(Collections.<String, String>emptyMap());
    }

    public LdapQueryVisitor(Map<String, String> fieldMap) {
        super(fieldMap);
    }

    public void visit(SearchCondition<T> sc) {

        StringBuilder sb = getStringBuilder();
        if (sb == null) {
            sb = new StringBuilder();
        }

        PrimitiveStatement statement = sc.getStatement();
        if (statement != null) {
            if (statement.getProperty() != null) {
                String name = getRealPropertyName(statement.getProperty());
                String rvalStr = getPropertyValue(name, statement.getValue());
                validatePropertyValue(name, rvalStr);

                sb.append('(');
                if (sc.getConditionType() == ConditionType.NOT_EQUALS) {
                    sb.append('!');
                }

                String ldapOperator = conditionTypeToLdapOperator(sc.getConditionType());
                String encodedRValStr = encodeQueryValues ? Util.doRFC2254Encoding(rvalStr) : rvalStr;
                sb.append(name).append(ldapOperator).append(encodedRValStr);

                sb.append(')');
            }
        } else {
            sb.append('(');
            if (sc.getConditionType() == ConditionType.AND) {
                sb.append('&');
            } else {
                sb.append('|');
            }

            for (SearchCondition<T> condition : sc.getSearchConditions()) {
                saveStringBuilder(sb);
                condition.accept(this);
                sb = getStringBuilder();
            }
            sb.append(')');
        }
        saveStringBuilder(sb);
    }


    public static String conditionTypeToLdapOperator(ConditionType ct) {
        String op;
        switch (ct) {
        case EQUALS:
        case NOT_EQUALS:
            op = "=";
            break;
        case GREATER_THAN:
        case GREATER_OR_EQUALS:
            op = ">=";
            break;
        case LESS_THAN:
        case LESS_OR_EQUALS:
            op = "<=";
            break;
        default:
            String msg = String.format("Condition type %s is not supported", ct.name());
            throw new RuntimeException(msg);
        }
        return op;
    }

    public boolean isEncodeQueryValues() {
        return encodeQueryValues;
    }

    public void setEncodeQueryValues(boolean encodeQueryValues) {
        this.encodeQueryValues = encodeQueryValues;
    }
}
