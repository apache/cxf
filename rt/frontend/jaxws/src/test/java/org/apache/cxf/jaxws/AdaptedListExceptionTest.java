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
package org.apache.cxf.jaxws;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import jakarta.jws.WebService;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.transport.local.LocalTransportFactory;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * CXF-6821: a member-level {@code @XmlJavaTypeAdapter} on a collection or array member of a
 * custom exception class must be applied to each item, in the schema and on the wire.
 */
public class AdaptedListExceptionTest extends AbstractJaxWsTest {

    private static final String ADDRESS = "local://localhost/adaptedListException";

    @Test
    public void testSchema() throws Exception {
        Document doc = getWSDLDocument(createServer());

        String fieldSeq = "//xsd:complexType[@name='FieldListException']/xsd:sequence/";
        assertValid(fieldSeq + "xsd:element[@name='list'][@maxOccurs='unbounded'][@type='tns:myClass2']", doc);
        assertValid(fieldSeq + "xsd:element[@name='array'][@maxOccurs='unbounded'][@type='tns:myClass2']", doc);
        assertValid(fieldSeq + "xsd:element[@name='single'][@type='tns:myClass2']", doc);
        assertInvalid(fieldSeq + "xsd:element[@name='single'][@maxOccurs]", doc);
        assertValid(fieldSeq + "xsd:element[@name='joined'][@type='xs:string']", doc);
        assertInvalid(fieldSeq + "xsd:element[@name='joined'][@maxOccurs]", doc);

        String getterSeq = "//xsd:complexType[@name='GetterListException']/xsd:sequence/";
        assertValid(getterSeq + "xsd:element[@name='list'][@maxOccurs='unbounded'][@type='tns:myClass2']", doc);
        assertValid(getterSeq + "xsd:element[@name='array'][@maxOccurs='unbounded'][@type='tns:myClass2']", doc);
        assertValid(getterSeq + "xsd:element[@name='single'][@type='tns:myClass2']", doc);
        assertInvalid(getterSeq + "xsd:element[@name='single'][@maxOccurs]", doc);
    }

    @Test
    public void testFaultDetailOnTheWire() throws Exception {
        createServer();
        addNamespace("ns", "http://jaxws.cxf.apache.org/");

        Node res = invoke(ADDRESS, LocalTransportFactory.TRANSPORT_ID, "throwFieldFault.xml");
        String detail = "//s:Fault/detail/ns:FieldListException/";
        assertValid(detail + "list[1]/value[text()='item0']", res);
        assertValid(detail + "list[2]/value[text()='item1']", res);
        assertValid(detail + "array[2]/value[text()='item1']", res);
        assertValid(detail + "single/value[text()='single']", res);
        assertValid(detail + "joined[text()='item0,item1']", res);

        res = invoke(ADDRESS, LocalTransportFactory.TRANSPORT_ID, "throwGetterFault.xml");
        detail = "//s:Fault/detail/ns:GetterListException/";
        assertValid(detail + "list[1]/value[text()='item0']", res);
        assertValid(detail + "list[2]/value[text()='item1']", res);
        assertValid(detail + "array[2]/value[text()='item1']", res);
        assertValid(detail + "single/value[text()='single']", res);
    }

    @Test
    public void testFieldRoundTrip() throws Exception {
        createServer();
        try {
            createClient().throwFieldFault(3);
            fail("Expected FieldListException");
        } catch (FieldListException ex) {
            assertEquals(List.of("item0", "item1", "item2"), values(ex.list));
            assertEquals(List.of("item0", "item1", "item2"), values(List.of(ex.array)));
            assertEquals("single", ex.single.getValue());
            assertEquals(List.of("item0", "item1", "item2"), ex.joined);
        }
    }

    @Test
    public void testGetterRoundTrip() throws Exception {
        createServer();
        try {
            createClient().throwGetterFault(2);
            fail("Expected GetterListException");
        } catch (GetterListException ex) {
            assertEquals(List.of("item0", "item1"), values(ex.getList()));
            assertEquals(List.of("item0", "item1"), values(List.of(ex.getArray())));
            assertEquals("single", ex.getSingle().getValue());
        }
    }

    @Test
    public void testEmptyAndNullRoundTrip() throws Exception {
        createServer();
        try {
            createClient().throwFieldFault(-1);
            fail("Expected FieldListException");
        } catch (FieldListException ex) {
            assertNull(ex.list);
            assertNull(ex.single);
        }
    }

    private Server createServer() {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setBus(getBus());
        sf.setServiceClass(AdaptedFaultService.class);
        sf.setServiceBean(new AdaptedFaultServiceImpl());
        sf.setAddress(ADDRESS);
        return sf.create();
    }

    private AdaptedFaultService createClient() {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setBus(getBus());
        cf.setServiceClass(AdaptedFaultService.class);
        cf.setAddress(ADDRESS);
        return cf.create(AdaptedFaultService.class);
    }

    private static List<String> values(List<MyClass1> list) {
        List<String> values = new ArrayList<>();
        for (MyClass1 c : list) {
            values.add(c.getValue());
        }
        return values;
    }

    private static List<MyClass1> items(int count) {
        if (count < 0) {
            return null;
        }
        List<MyClass1> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            items.add(new MyClass1("item" + i));
        }
        return items;
    }

    /**
     * Not a JAXB-bindable class (no no-arg constructor), so it can only be written via the adapter.
     */
    public static final class MyClass1 {
        private final String value;

        public MyClass1(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class MyClass2 {
        private String value;

        public MyClass2() {
        }

        public MyClass2(String value) {
            this.value = value;
        }
    }

    public static class Cl1ToCl2Adapter extends XmlAdapter<MyClass2, MyClass1> {
        @Override
        public MyClass1 unmarshal(MyClass2 v) {
            return v == null ? null : new MyClass1(v.value);
        }

        @Override
        public MyClass2 marshal(MyClass1 v) {
            return v == null ? null : new MyClass2(v.getValue());
        }
    }

    /**
     * Adapts the whole collection, so the member is single-valued.
     */
    public static class JoinAdapter extends XmlAdapter<String, List<String>> {
        @Override
        public List<String> unmarshal(String v) {
            return v == null ? null : new ArrayList<>(List.of(v.split(",")));
        }

        @Override
        public String marshal(List<String> v) {
            return v == null ? null : String.join(",", v);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class FieldListException extends Exception {
        private static final long serialVersionUID = 1L;

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        private List<MyClass1> list;

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        private MyClass1[] array;

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        private MyClass1 single;

        @XmlJavaTypeAdapter(JoinAdapter.class)
        private List<String> joined;

        public FieldListException() {
        }

        public FieldListException(String message) {
            super(message);
        }
    }

    public static class GetterListException extends Exception {
        private static final long serialVersionUID = 1L;

        private List<MyClass1> list;
        private MyClass1[] array;
        private MyClass1 single;

        public GetterListException() {
        }

        public GetterListException(String message) {
            super(message);
        }

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        public List<MyClass1> getList() {
            return list;
        }

        public void setList(List<MyClass1> list) {
            this.list = list;
        }

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        public MyClass1[] getArray() {
            return array;
        }

        public void setArray(MyClass1[] array) {
            this.array = array;
        }

        @XmlJavaTypeAdapter(Cl1ToCl2Adapter.class)
        public MyClass1 getSingle() {
            return single;
        }

        public void setSingle(MyClass1 single) {
            this.single = single;
        }
    }

    @WebService
    public interface AdaptedFaultService {
        String throwFieldFault(int count) throws FieldListException;

        String throwGetterFault(int count) throws GetterListException;
    }

    @WebService(endpointInterface = "org.apache.cxf.jaxws.AdaptedListExceptionTest$AdaptedFaultService")
    public static class AdaptedFaultServiceImpl implements AdaptedFaultService {
        public String throwFieldFault(int count) throws FieldListException {
            FieldListException ex = new FieldListException("field fault");
            ex.list = items(count);
            if (count >= 0) {
                ex.array = ex.list.toArray(new MyClass1[0]);
                ex.single = new MyClass1("single");
                ex.joined = values(ex.list);
            }
            throw ex;
        }

        public String throwGetterFault(int count) throws GetterListException {
            GetterListException ex = new GetterListException("getter fault");
            ex.setList(items(count));
            ex.setArray(ex.getList().toArray(new MyClass1[0]));
            ex.setSingle(new MyClass1("single"));
            throw ex;
        }
    }
}
