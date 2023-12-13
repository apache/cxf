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

package org.apache.cxf.jaxrs.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.QueryParam;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.junit.Assert;
import org.junit.Test;

public class JAXBUtilsTest {

    @Test
    public void simpleXmlJavaTypeAdapter() {
        correctValueType(CustomerDetailsWithSimpleAdapter.class);
    }

    @Test
    public void extendedXmlJavaTypeAdapter() {
        correctValueType(CustomerDetailsWithExtendedAdapter.class);
    }

    private void correctValueType(Class<?> clazz) {
        Field field = clazz.getDeclaredFields()[0];
        Annotation[] paramAnns = field.getDeclaredAnnotations();
        Class<?> valueType = JAXBUtils.getValueTypeFromAdapter(LocalDate.class, LocalDate.class, paramAnns);
        Assert.assertEquals(String.class, valueType);
    }

    public static class CustomerDetailsWithExtendedAdapter {
        @NotNull
        @QueryParam("birthDate")
        @XmlJavaTypeAdapter(LocalDateXmlAdapter.class)
        private LocalDate birthDate;

        public LocalDate getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
        }
    }

    public static class CustomerDetailsWithSimpleAdapter {
        @NotNull
        @QueryParam("birthDate")
        @XmlJavaTypeAdapter(LocalDateAdapter.class)
        private LocalDate birthDate;

        public LocalDate getBirthDate() {
            return birthDate;
        }

        public void setBirthDate(LocalDate birthDate) {
            this.birthDate = birthDate;
        }
    }

    public class LocalDateAdapter extends XmlAdapter<String, LocalDate> {
        @Override
        public LocalDate unmarshal(String dateInput) {
            return LocalDate.parse(dateInput);
        }

        @Override
        public String marshal(LocalDate localDate) {
            return DateTimeFormatter.ISO_DATE.format(localDate);
        }
    }
}
