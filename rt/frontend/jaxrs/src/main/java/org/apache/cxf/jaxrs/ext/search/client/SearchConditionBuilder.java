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
package org.apache.cxf.jaxrs.ext.search.client;

import java.util.Date;

import javax.xml.datatype.Duration;

/**
 * Builds client-side search condition string using `fluent interface' style. It helps build create part of
 * URL that will be parsed by server-side counterpart e.g. FiqlSearchConditionBuilder has FiqlParser.
 */
public interface SearchConditionBuilder {

    /** Creates unconstrained query (no conditions) */
    PartialCondition query();

    /** Finalize condition construction and build search condition. */
    String build();

    public interface Property {
        /** Is textual property equal to given literal or matching given pattern? */
        CompleteCondition equalTo(String literalOrPattern);

        /** Is numeric property equal to given number? */
        CompleteCondition equalTo(double number);

        /** Is date property same as given date? */
        CompleteCondition equalTo(Date date);

        /** Is date property same as date distant from now by given period of time? */
        CompleteCondition equalTo(Duration distanceFromNow);

        /** Is textual property different than given literal or not matching given pattern? */
        CompleteCondition notEqualTo(String literalOrPattern);

        /** Is numeric property different than given number? */
        CompleteCondition notEqualTo(double number);

        /** Is date property different than given date? */
        CompleteCondition notEqualTo(Date date);

        /** Is date property different than date distant from now by given period of time? */
        CompleteCondition notEqualTo(Duration distanceFromNow);

        /** Is numeric property greater than given number? */
        CompleteCondition greaterThan(double number);

        /** Is numeric property less than given number? */
        CompleteCondition lessThan(double number);

        /** Is numeric property greater or equal to given number? */
        CompleteCondition greaterOrEqualTo(double number);

        /** Is numeric property less or equal to given number? */
        CompleteCondition lessOrEqualTo(double number);

        /** Is date property after (greater than) given date? */
        CompleteCondition after(Date date);

        /** Is date property before (less than) given date? */
        CompleteCondition before(Date date);

        /** Is date property not before (greater or equal to) given date? */
        CompleteCondition notBefore(Date date);

        /** Is date property not after (less or equal to) given date? */
        CompleteCondition notAfter(Date date);

        /** Is date property after (greater than) date distant from now by given period of time? */
        CompleteCondition after(Duration distanceFromNow);

        /** Is date property before (less than) date distant from now by given period of time? */
        CompleteCondition before(Duration distanceFromNow);

        /** Is date property not after (less or equal to) date distant from now by given period of time? */
        CompleteCondition notAfter(Duration distanceFromNow);

        /** Is date property not before (greater or equal to) date distant from now by given 
         * period of time? */
        CompleteCondition notBefore(Duration distanceFromNow);

        /** Is textual property lexically after (greater than) given literal? */
        CompleteCondition lexicalAfter(String literal);

        /** Is textual property lexically before (less than) given literal? */
        CompleteCondition lexicalBefore(String literal);

        /** Is textual property lexically not before (greater or equal to) given literal? */
        CompleteCondition lexicalNotBefore(String literal);

        /** Is textual property lexically not after (less or equal to) given literal? */
        CompleteCondition lexicalNotAfter(String literal);
    }
    
    public interface PartialCondition {
        /** Get property of inspected entity type */
        Property is(String property);

        /** Conjunct multiple expressions */
        CompleteCondition and(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn);

        /** Disjunct multiple expressions */
        CompleteCondition or(CompleteCondition c1, CompleteCondition c2, CompleteCondition... cn);
    }
    
    public interface CompleteCondition /*extends PartialCondition*/ {
        /** Conjunct current expression with another */
        PartialCondition and();

        /** Disjunct current expression with another */
        PartialCondition or();

        /** Finalize condition construction and build search condition. */
        String build();
    }
}
