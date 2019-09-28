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
package org.apache.cxf.jaxrs.ext.search.odata;

import java.util.Collections;

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ODataParserTest {
    private ODataParser<Person> parser;

    public static class Person {
        private String firstName;
        private String lastName;
        private int age;
        private float height;
        private double hourlyRate;
        private Long ssn;

        public Person() {
        }

        public Person(final String firstName, final String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }

        public double getHourlyRate() {
            return hourlyRate;
        }

        public void setHourlyRate(double hourlyRate) {
            this.hourlyRate = hourlyRate;
        }

        public Long getSsn() {
            return ssn;
        }

        public void setSsn(Long ssn) {
            this.ssn = ssn;
        }

        Person withAge(int newAge) {
            setAge(newAge);
            return this;
        }

        Person withHeight(float newHeight) {
            setHeight(newHeight);
            return this;
        }

        Person withHourlyRate(double newHourlyRate) {
            setHourlyRate(newHourlyRate);
            return this;
        }

        Person withSsn(Long newSsn) {
            setSsn(newSsn);
            return this;
        }
    }

    @Before
    public void setUp() {
        parser = new ODataParser<>(Person.class, Collections.<String, String>emptyMap(),
            Collections.singletonMap("thename", "FirstName"));
    }

    @Test
    public void testFilterByFirstNameEqualsValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("FirstName eq 'Tom'");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil")));
        assertFalse(filter.isMet(new Person("Peter", "Bombadil")));
    }

    @Test
    public void testFilterByFirstNameEqualsValueNonMatchingProperty() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("thename eq 'Tom'");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil")));
        assertFalse(filter.isMet(new Person("Peter", "Bombadil")));
    }

    @Test
    public void testFilterByFirstAndLastNameEqualValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("FirstName eq 'Tom' and LastName eq 'Bombadil'");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil")));
        assertFalse(filter.isMet(new Person("Peter", "Bombadil")));
    }

    @Test
    public void testFilterByFirstOrLastNameEqualValue() throws SearchParseException {
        SearchCondition< Person > filter =
            parser.parse("FirstName eq 'Tom' or FirstName eq 'Peter' and LastName eq 'Bombadil'");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil")));
        assertTrue(filter.isMet(new Person("Peter", "Bombadil")));
        assertFalse(filter.isMet(new Person("Barry", "Bombadil")));
    }

    @Test
    public void testFilterByFirstAndLastNameEqualValueWithAlternative() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse(
            "(FirstName eq 'Tom' and LastName eq 'Tommyknocker')"
            + " or (FirstName eq 'Peter' and LastName eq 'Bombadil')");
        assertTrue(filter.isMet(new Person("Tom", "Tommyknocker")));
        assertTrue(filter.isMet(new Person("Peter", "Bombadil")));
        assertFalse(filter.isMet(new Person("Tom", "Bombadil")));
    }

    @Test
    public void testFilterByValueEqualsFirstName() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("'Tom' eq FirstName");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil")));
    }

    @Test
    public void testFilterByAgeGreatThanValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("Age gt 17");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil").withAge(18)));
        assertFalse(filter.isMet(new Person("Tom", "Bombadil").withAge(16)));
    }

    @Test
    public void testFilterByHeightGreatOrEqualValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("Height ge 179.5f or Height le 159.5d");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil").withHeight(185.6f)));
        assertFalse(filter.isMet(new Person("Tom", "Bombadil").withHeight(166.7f)));
    }

    @Test
    public void testFilterByHourlyRateGreatThanValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("HourlyRate ge 30.50d or HourlyRate lt 20.50f");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil").withHourlyRate(45.6)));
        assertFalse(filter.isMet(new Person("Tom", "Bombadil").withHourlyRate(26.7)));
    }

    @Test
    public void testFilterBySsnNotEqualsToValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("Ssn ne 748232221");
        assertTrue(filter.isMet(new Person("Tom", "Bombadil").withSsn(553232222L)));
        assertFalse(filter.isMet(new Person("Tom", "Bombadil").withHourlyRate(748232221L)));
    }
}