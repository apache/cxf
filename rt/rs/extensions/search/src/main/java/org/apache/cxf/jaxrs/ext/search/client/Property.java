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

import org.apache.cxf.jaxrs.ext.search.ConditionType;

/**
 * Part of fluent interface of {@link SearchConditionBuilder}.
 */
public interface Property {

    /** Is textual property equal to given literal or matching given pattern? */
    CompleteCondition equalTo(String value, String ...moreValues);

    /** Is numeric property equal to given double number? */
    CompleteCondition equalTo(Double number, Double... moreValues);

    /** Is numeric property equal to given long number? */
    CompleteCondition equalTo(Long number, Long... moreValues);

    /** Is numeric property equal to given long number? */
    CompleteCondition equalTo(Integer number, Integer... moreValues);

    /** Is date property same as given date? */
    CompleteCondition equalTo(Date date, Date... moreValues);

    /** Is date property same as date distant from now by given period of time? */
    CompleteCondition equalTo(Duration distanceFromNow, Duration... moreValues);

    /** Is textual property different than given literal or not matching given pattern? */
    CompleteCondition notEqualTo(String literalOrPattern);

    /** Is numeric property different than given double number? */
    CompleteCondition notEqualTo(Double number);

    /** Is numeric property different than given long number? */
    CompleteCondition notEqualTo(Long number);

    /** Is numeric property different than given int number? */
    CompleteCondition notEqualTo(Integer number);

    /** Is date property different than given date? */
    CompleteCondition notEqualTo(Date date);

    /** Is date property different than date distant from now by given period of time? */
    CompleteCondition notEqualTo(Duration distanceFromNow);

    /** Is numeric property greater than given number? */
    CompleteCondition greaterThan(Double number);

    /** Is numeric property greater than given number? */
    CompleteCondition greaterThan(Long number);

    /** Is numeric property greater than given number? */
    CompleteCondition greaterThan(Integer number);

    /** Is numeric property less than given number? */
    CompleteCondition lessThan(Double number);

    /** Is numeric property less than given number? */
    CompleteCondition lessThan(Long number);

    /** Is numeric property less than given number? */
    CompleteCondition lessThan(Integer number);

    /** Is numeric property greater or equal to given number? */
    CompleteCondition greaterOrEqualTo(Double number);

    /** Is numeric property greater or equal to given number? */
    CompleteCondition greaterOrEqualTo(Long number);

    /** Is numeric property greater or equal to given number? */
    CompleteCondition greaterOrEqualTo(Integer number);

    /** Is numeric property less or equal to given number? */
    CompleteCondition lessOrEqualTo(Double number);

    /** Is numeric property less or equal to given number? */
    CompleteCondition lessOrEqualTo(Long number);

    /** Is numeric property less or equal to given number? */
    CompleteCondition lessOrEqualTo(Integer number);

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

    /**
     * Is date property not before (greater or equal to) date distant from now by given period of time?
     */
    CompleteCondition notBefore(Duration distanceFromNow);

    /** Is textual property lexically after (greater than) given literal? */
    CompleteCondition lexicalAfter(String literal);

    /** Is textual property lexically before (less than) given literal? */
    CompleteCondition lexicalBefore(String literal);

    /** Is textual property lexically not before (greater or equal to) given literal? */
    CompleteCondition lexicalNotBefore(String literal);

    /** Is textual property lexically not after (less or equal to) given literal? */
    CompleteCondition lexicalNotAfter(String literal);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, String value);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, Double value);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, Long value);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, Integer value);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, Date value);

    /** Generic */
    CompleteCondition comparesTo(ConditionType op, Duration value);

}
