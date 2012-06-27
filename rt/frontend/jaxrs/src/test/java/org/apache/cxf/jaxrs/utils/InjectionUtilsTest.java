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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MultivaluedMap;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ParameterType;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class InjectionUtilsTest extends Assert {
    
    
    @Test
    public void testHandleParameterWithXmlAdapterOnInterface() throws Exception {
        // Arrange
        String value = "1.1";

        // Act
        Object id = InjectionUtils.handleParameter(value, 
                                                   true, 
                                                   Id.class, 
                                                   new Annotation[] {}, 
                                                   ParameterType.PATH,  
                                                   createMessage());

        // Assert
        assertTrue(id instanceof Id);
        assertEquals(value, ((Id)id).getId());
    }
    
    public void testCollectionTypeFromArray() {
        assertNull(InjectionUtils.getCollectionType(String[].class));
    }
    
    @Test
    public void testCollectionType() {
        assertEquals(ArrayList.class, InjectionUtils.getCollectionType(Collection.class));
        assertEquals(ArrayList.class, InjectionUtils.getCollectionType(List.class));
        assertEquals(HashSet.class, InjectionUtils.getCollectionType(Set.class));
        assertEquals(TreeSet.class, InjectionUtils.getCollectionType(SortedSet.class));
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
        List<String> values = new ArrayList<String>();
        values.add("lv1");
        values.add("lv2");
        bean1.setC(values);
        CustomerBean2 bean2 = new CustomerBean2();
        bean2.setA("aaValue");
        bean2.setB(2L);
        values = new ArrayList<String>();
        values.add("lv11");
        values.add("lv22");
        bean2.setC(values);
        Set<String> set = new HashSet<String>();
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
        
        public void setE(String e) {
            this.e = e;
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
    
    private Message createMessage() {
        ProviderFactory factory = ProviderFactory.getInstance();
        Message m = new MessageImpl();
        m.put("org.apache.cxf.http.case_insensitive_queries", false);
        Exchange e = new ExchangeImpl();
        m.setExchange(e);
        e.setInMessage(m);
        Endpoint endpoint = EasyMock.createMock(Endpoint.class);
        endpoint.getEndpointInfo();
        EasyMock.expectLastCall().andReturn(null).anyTimes();
        endpoint.get(Application.class.getName());
        EasyMock.expectLastCall().andReturn(null);
        endpoint.size();
        EasyMock.expectLastCall().andReturn(0).anyTimes();
        endpoint.isEmpty();
        EasyMock.expectLastCall().andReturn(true).anyTimes();
        endpoint.get(ProviderFactory.class.getName());
        EasyMock.expectLastCall().andReturn(factory).anyTimes();
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
}
