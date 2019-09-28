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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.validation.constraints.NotNull;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.migesok.jaxb.adapter.javatime.LocalDateXmlAdapter;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;

import org.easymock.EasyMock;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class InjectionUtilsTest {


    @Test
    public void testHandleParameterWithXmlAdapterOnInterface() throws Exception {
        // Arrange
        String value = "1.1";

        // Act
        Id id = InjectionUtils.handleParameter(value,
                                                   true,
                                                   Id.class,
                                                   Id.class,
                                                   new Annotation[] {},
                                                   ParameterType.PATH,
                                                   createMessage());

        // Assert
        assertEquals(value, id.getId());
    }

    @Test
    public void testCollectionTypeFromArray() {
        assertNull(InjectionUtils.getCollectionType(String[].class));
    }

    @Test
    public void testCollectionType() {
        assertEquals(ArrayList.class, InjectionUtils.getCollectionType(Collection.class)); //NOPMD
        assertEquals(ArrayList.class, InjectionUtils.getCollectionType(List.class)); //NOPMD
        assertEquals(HashSet.class, InjectionUtils.getCollectionType(Set.class)); //NOPMD
        assertEquals(TreeSet.class, InjectionUtils.getCollectionType(SortedSet.class)); //NOPMD
    }

    @Test
    public void testSupportedCollectionType() {
        assertFalse(InjectionUtils.isSupportedCollectionOrArray(Map.class));
        assertTrue(InjectionUtils.isSupportedCollectionOrArray(String[].class));
        assertTrue(InjectionUtils.isSupportedCollectionOrArray(List.class));
        assertTrue(InjectionUtils.isSupportedCollectionOrArray(Collection.class));
        assertTrue(InjectionUtils.isSupportedCollectionOrArray(Set.class));
        assertTrue(InjectionUtils.isSupportedCollectionOrArray(SortedSet.class));
    }


    @Test
    public void testExtractValuesFromBean() {
        CustomerBean1 bean1 = new CustomerBean1();
        bean1.setA("aValue");
        bean1.setB(1L);
        List<String> values = new ArrayList<>();
        values.add("lv1");
        values.add("lv2");
        bean1.setC(values);
        CustomerBean2 bean2 = new CustomerBean2();
        bean2.setA("aaValue");
        bean2.setB(2L);
        values = new ArrayList<>();
        values.add("lv11");
        values.add("lv22");
        bean2.setC(values);
        Set<String> set = new HashSet<>();
        set.add("set1");
        set.add("set2");
        bean2.setS(set);

        bean1.setD(bean2);


        MultivaluedMap<String, Object> map = InjectionUtils.extractValuesFromBean(bean1, "");
        assertEquals("Size is wrong", 7, map.size());
        assertEquals(1, map.get("a").size());
        assertEquals("aValue", map.getFirst("a"));
        assertEquals(1, map.get("b").size());
        assertEquals(1L, map.getFirst("b"));
        assertEquals(2, map.get("c").size());
        assertEquals("lv1", map.get("c").get(0));
        assertEquals("lv2", map.get("c").get(1));

        assertEquals(1, map.get("d.a").size());
        assertEquals("aaValue", map.getFirst("d.a"));
        assertEquals(1, map.get("d.b").size());
        assertEquals(2L, map.getFirst("d.b"));
        assertEquals(2, map.get("d.c").size());
        assertEquals("lv11", map.get("d.c").get(0));
        assertEquals("lv22", map.get("d.c").get(1));

        assertEquals(2, map.get("d.s").size());
        assertTrue(map.get("d.s").contains("set1"));
        assertTrue(map.get("d.s").contains("set2"));
    }

    @Test
    public void testInstantiateJAXBEnum() {
        CarType carType = InjectionUtils.handleParameter("AUDI", false, CarType.class,
                                                         CarType.class, null,
                                                         ParameterType.QUERY, null);
        assertEquals("Type is wrong", CarType.AUDI, carType);
    }

    @Test
    public void testInstantiateIntegerInQuery() {
        Integer integer = InjectionUtils.handleParameter("", false, Integer.class,
                Integer.class, null,
                ParameterType.QUERY, null);
        assertNull("Integer is not null", integer);
    }

    @Test
    public void testInstantiateFloatInQuery() {
        Float f = InjectionUtils.handleParameter("", false, float.class,
                float.class, null,
                ParameterType.QUERY, null);
        assertEquals("Float is not 0", Float.valueOf(0F), f);
    }

    @Test
    public void testGenericInterfaceType() throws NoSuchMethodException {
        Type str = InjectionUtils.getGenericResponseType(GenericInterface.class.getMethod("get"),
                       TestService.class, "", String.class, new ExchangeImpl());
        assertEquals(String.class, str);
        ParameterizedType list = (ParameterizedType) InjectionUtils.getGenericResponseType(
            GenericInterface.class.getMethod("list"), TestService.class,
            new ArrayList<>(), ArrayList.class, new ExchangeImpl()); //NOPMD
        assertEquals(String.class, list.getActualTypeArguments()[0]);
    }

    @Test(expected = InternalServerErrorException.class)
    public void testJsr310DateExceptionHandling() {
        Field field = CustomerDetailsWithAdapter.class.getDeclaredFields()[0];
        Annotation[] paramAnns = field.getDeclaredAnnotations();
        InjectionUtils.createParameterObject(Collections.singletonList("wrongDate"), LocalDate.class,
                LocalDate.class, paramAnns, null, false, ParameterType.QUERY, createMessage());
    }

    static class CustomerBean1 {
        private String a;
        private Long b;
        private List<String> c;
        private CustomerBean2 d;
        private String e;

        public void setA(String aString) {
            this.a = aString;
        }
        public void setB(Long bLong) {
            this.b = bLong;
        }
        public void setC(List<String> cStringList) {
            this.c = cStringList;
        }
        public void setD(CustomerBean2 dCustomerBean) {
            this.d = dCustomerBean;
        }
        public String getA() {
            return a;
        }
        public Long getB() {
            return b;
        }
        public List<String> getC() {
            return c;
        }
        public CustomerBean2 getD() {
            return d;
        }

        public void setE(String ee) {
            this.e = ee;
        }
        public String getE() {
            return e;
        }

    }

    static class CustomerBean2 {
        private String a;
        private Long b;
        private List<String> c;
        private Set<String> s;

        public void setA(String aString) {
            this.a = aString;
        }
        public void setB(Long bLong) {
            this.b = bLong;
        }
        public void setC(List<String> cStringList) {
            this.c = cStringList;
        }
        public String getA() {
            return a;
        }
        public Long getB() {
            return b;
        }
        public List<String> getC() {
            return c;
        }

        public void setS(Set<String> set) {
            this.s = set;
        }

        public Set<String> getS() {
            return this.s;
        }
    }

    private static Message createMessage() {
        ProviderFactory factory = ServerProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.mock(Endpoint.class);
        EasyMock.expect(endpoint.getEndpointInfo()).andReturn(null).anyTimes();
        EasyMock.expect(endpoint.get(Application.class.getName())).andReturn(null);
        EasyMock.expect(endpoint.size()).andReturn(0).anyTimes();
        EasyMock.expect(endpoint.isEmpty()).andReturn(true).anyTimes();
        EasyMock.expect(endpoint.get(ServerProviderFactory.class.getName())).andReturn(factory).anyTimes();
        EasyMock.replay(endpoint);
        e.put(Endpoint.class, endpoint);
        return m;
    }

    public static class Adapter extends XmlAdapter<String, Id> {

        @Override
        public String marshal(final Id id) throws Exception {
            return id.getId();
        }

        @Override
        public Id unmarshal(final String idStr) throws Exception {
            Id id = new DelegatingId();
            id.setId(idStr);
            return id;
        }
    }

    @XmlJavaTypeAdapter(Adapter.class)
    public interface Id {
        String getId();

        void setId(String id);
    }

    public static class DelegatingId implements Id {

        private String id;

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

    }

    public enum CarType {

        AUDI("Audi"),
        GOLF("Golf"),
        BMW("BMW");
        private final String value;

        CarType(String v) {
            value = v;
        }

        public String value() {
            return value;
        }

        public static CarType fromValue(String v) {
            for (CarType c: CarType.values()) {
                if (c.value.equals(v)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(v);
        }

    }
    interface GenericInterface<A> {
        A get();
        List<A> list();
    }
    interface ServiceInterface extends Serializable, GenericInterface<String> {
    }
    public static class TestService implements Serializable, ServiceInterface {
        private static final long serialVersionUID = 1L;
        @Override
        public String get() {
            return "";
        }
        @Override
        public List<String> list() {
            return new ArrayList<>();
        }
    }

    public class CustomerDetailsWithAdapter {
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
}