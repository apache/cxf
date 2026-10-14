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
package org.apache.cxf.aegis.type.java5;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * A bean property whose type is a generic array of a type variable (e.g. T[])
 * used to cause a NullPointerException in TypeUtil.getTypeRelatedClass.
 */
public class GenericArrayTest extends AbstractAegisTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createService(GenericArrayService.class, "GenericArrayService");
    }

    @Test
    public void testGenericArrayProperty() throws Exception {
        Document doc = getWSDLDocument("GenericArrayService");
        assertNotNull(doc);
        assertValid("//xsd:complexType[@name='Page']", doc);
    }

    @Test
    public void testBoundedGenericArrayProperty() throws Exception {
        Document doc = getWSDLDocument("GenericArrayService");
        assertNotNull(doc);
        // T extends Score, so T[] should be mapped to an array of Score
        assertValid("//xsd:complexType[@name='Subject']"
                    + "//xsd:element[@name='scores' and @type='tns:ArrayOfScore']", doc);
        assertValid("//xsd:complexType[@name='ArrayOfScore']//xsd:element[@type='tns:Score']", doc);
    }

    public static class Page<T> {
        private T[] items;

        public T[] getItems() {
            return items;
        }

        public void setItems(T[] items) {
            this.items = items;
        }
    }

    public static class Score {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class Subject<T extends Score> {
        private T[] scores;

        public T[] getScores() {
            return scores;
        }

        public void setScores(T[] scores) {
            this.scores = scores;
        }
    }

    public static class GenericArrayService {
        public Page<String> getPage() {
            return new Page<>();
        }

        public Subject<Score> getSubject() {
            return new Subject<>();
        }
    }
}
