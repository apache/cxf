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

import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchParseException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OData2ParserTest extends Assert {
    private OData2Parser<Person> parser;

    public static class Person {
        private String firstName;
        private String lastName;
        private int age;

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
        
        Person withAge(int newAge) {
            setAge(newAge);
            return this;
        }
    }

    @Before
    public void setUp() {
        parser = new OData2Parser<Person>(Person.class);
    }

    @Test
    public void testFilterByFirstNameEqualsValue() throws SearchParseException {
        SearchCondition< Person > filter = parser.parse("FirstName eq 'Tom'");
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
}
