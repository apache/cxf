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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.databinding.XFireCompatibilityServiceConfiguration;
import org.apache.cxf.aegis.type.AegisType;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.collection.CollectionType;
import org.apache.cxf.aegis.type.collection.MapType;
import org.apache.cxf.aegis.type.java5.dto.CollectionDTO;
import org.apache.cxf.aegis.type.java5.dto.DTOService;
import org.apache.cxf.aegis.type.java5.dto.ObjectDTO;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.frontend.ClientProxyFactoryBean;

import org.junit.Before;
import org.junit.Test;



public class CollectionTest extends AbstractAegisTest {
    private DefaultTypeMapping tm;
    private Java5TypeCreator creator;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        tm = new DefaultTypeMapping(SOAPConstants.XSD);
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testType() throws Exception {
        Method m = CollectionService.class.getMethod("getStrings", new Class[0]);

        AegisType type = creator.createType(m, -1);
        tm.register(type);
        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;
        QName componentName = colType.getSchemaType();

        assertEquals("ArrayOfString", componentName.getLocalPart());
        assertEquals("ArrayOfString", componentName.getLocalPart());

        type = colType.getComponentType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRecursiveCollections() throws Exception {
        Method m = CollectionService.class.getMethod("getStringCollections", new Class[0]);

        AegisType type = creator.createType(m, -1);
        tm.register(type);
        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;
        QName componentName = colType.getSchemaType();

        assertEquals("ArrayOfArrayOfString", componentName.getLocalPart());

        type = colType.getComponentType();
        assertNotNull(type);
        assertTrue(type instanceof CollectionType);

        CollectionType colType2 = (CollectionType)type;
        componentName = colType2.getSchemaType();

        assertEquals("ArrayOfString", componentName.getLocalPart());

        type = colType2.getComponentType();
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPDType() throws Exception {
        PropertyDescriptor pd = Introspector.getBeanInfo(CollectionDTO.class, Object.class)
            .getPropertyDescriptors()[0];
        AegisType type = creator.createType(pd);
        tm.register(type);
        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;

        type = colType.getComponentType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));
    }

    @Test
    public void testCollectionDTO() {
        tm = new DefaultTypeMapping(SOAPConstants.XSD);
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);

        AegisType dto = creator.createType(CollectionDTO.class);
        Set deps = dto.getDependencies();

        AegisType type = (AegisType)deps.iterator().next();

        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;

        deps = dto.getDependencies();
        assertEquals(1, deps.size());

        AegisType comType = colType.getComponentType();
        assertEquals(String.class, comType.getTypeClass());
    }

    @Test
    public void testObjectDTO() {
        tm = new DefaultTypeMapping(SOAPConstants.XSD);
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);

        AegisType dto = creator.createType(ObjectDTO.class);
        Set deps = dto.getDependencies();

        assertFalse(deps.isEmpty());

        AegisType type = (AegisType)deps.iterator().next();

        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;

        deps = dto.getDependencies();
        assertEquals(1, deps.size());

        AegisType comType = colType.getComponentType();
        assertEquals(Object.class, comType.getTypeClass());
    }

    @Test
    public void testCollectionDTOService() throws Exception {
        createService(DTOService.class);
        invoke("DTOService", "/org/apache/cxf/aegis/type/java5/dto/GetDTO.xml");
    }

    @Test
    public void testCollectionServiceWSDL() throws Exception {
        
        createService(CollectionServiceInterface.class, new CollectionService(), null);

        Document wsdl = getWSDLDocument("CollectionServiceInterface");
        assertValid("//xsd:element[@name='return'][@type='tns:ArrayOfString']", wsdl);
    }

    @Test
    public void testUnannotatedStrings() throws Exception {        
        createService(CollectionServiceInterface.class, new CollectionService(), null);

        Document doc = getWSDLDocument("CollectionServiceInterface");
        // printNode(doc);
        assertValid(
                    "//xsd:complexType[@name='getUnannotatedStringsResponse']"
                    + "/xsd:sequence/xsd:element[@type='tns:ArrayOfString']",
                    doc);
    }
    
    @Test
    public void testDoubleList() throws Exception {
        createService(CollectionServiceInterface.class, new CollectionService(), null);
        Document doc = getWSDLDocument("CollectionServiceInterface");
        assertValid(
                    "//xsd:complexType[@name='ArrayOfDouble']"
                    + "/xsd:sequence/xsd:element[@type='xsd:double']",
                    doc);
        
    }
    
    /**
     * CXF-1833 complained of a bizarre schema when @@WebParaming a parameter of List<String>. This regression
     * test captures the fact that we don't, in fact, have this problem with correct use of JAX-WS.
     * @throws Exception
     */
    @Test
    public void webMethodOnListParam() throws Exception {
        createJaxwsService(CollectionService.class, new CollectionService(), null, null);
        Document doc = getWSDLDocument("CollectionServiceService");
        // what we do not want is <xsd:schema targetNamespace="http://util.java" ... />
        assertInvalid("//xsd:schema[@targetNamespace='http://util.java']",
                      doc);
    }
    
    @Test
    public void testListTypes() throws Exception {
        createService(CollectionServiceInterface.class, new CollectionService(), null);
        
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.getServiceFactory().getServiceConfigurations().add(0, 
                                                              new XFireCompatibilityServiceConfiguration());
        proxyFac.setServiceClass(CollectionServiceInterface.class);
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://CollectionServiceInterface");
        proxyFac.setBus(getBus());

        CollectionServiceInterface csi = (CollectionServiceInterface)proxyFac.create();
        SortedSet<String> strings = new TreeSet<String>();
        strings.add("Able");
        strings.add("Baker");
        String first = csi.takeSortedStrings(strings);
        assertEquals("Able", first);
        
        //CHECKSTYLE:OFF
        HashSet<String> hashedSet = new HashSet<String>();
        hashedSet.addAll(strings);
        String countString = csi.takeUnsortedSet(hashedSet);
        assertEquals("2", countString);
        //CHECKSTYLE:ON
    }
    
    @Test
    public void testNestedMapType() throws Exception {
        Method m = CollectionService.class.getMethod("mapOfMapWithStringAndPojo", 
                                                     new Class[] {Map.class});
        AegisType type = creator.createType(m, 0);
        tm.register(type);
        assertTrue(type instanceof MapType);
        MapType mapType = (MapType) type;
        AegisType valueType = mapType.getValueType();
        assertFalse(valueType.getSchemaType().getLocalPart().contains("any"));
    }
    
    /**
     * CXF-2017
     * @throws Exception
     */
    @Test
    public void testNestedMap() throws Exception {
        CollectionService impl = new CollectionService();
        createService(CollectionServiceInterface.class, impl, null);
        
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.getServiceFactory().getServiceConfigurations().add(0, 
                                                              new XFireCompatibilityServiceConfiguration());
        proxyFac.setServiceClass(CollectionServiceInterface.class);
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://CollectionServiceInterface");
        proxyFac.setBus(getBus());

        CollectionServiceInterface csi = (CollectionServiceInterface)proxyFac.create();
        
        Map<String, Map<String, BeanWithGregorianDate>> complexMap;
        complexMap = new HashMap<String, Map<String, BeanWithGregorianDate>>();
        Map<String, BeanWithGregorianDate> innerMap = new HashMap<String, BeanWithGregorianDate>();
        BeanWithGregorianDate bean = new BeanWithGregorianDate();
        bean.setName("shem bean");
        bean.setId(42);
        innerMap.put("firstBean", bean);
        complexMap.put("firstKey", innerMap);
        csi.mapOfMapWithStringAndPojo(complexMap);
        
        Map<String, Map<String, BeanWithGregorianDate>> gotMap = impl.getLastComplexMap();
        assertTrue(gotMap.containsKey("firstKey"));
        Map<String, BeanWithGregorianDate> v = gotMap.get("firstKey");
        BeanWithGregorianDate b = v.get("firstBean");
        assertNotNull(b);
        
    }

    public class CollectionService implements CollectionServiceInterface {
        
        private Map<String, Map<String, BeanWithGregorianDate>> lastComplexMap;
        
        /** {@inheritDoc}*/
        public Collection<String> getStrings() {
            return null;
        }

        /** {@inheritDoc}*/
        public void setLongs(Collection<Long> longs) {
        }

        /** {@inheritDoc}*/
        public Collection getUnannotatedStrings() {
            return null;
        }

        /** {@inheritDoc}*/
        public Collection<Collection<String>> getStringCollections() {
            return null;
        }
        
        /** {@inheritDoc}*/
        public void takeDoubleList(List<Double> doublesList) {
        }
        
        /** {@inheritDoc}*/
        public String takeSortedStrings(SortedSet<String> strings) {
            return strings.first();
        }

        public void method1(List<String> headers1) {
            // do nothing, this is purely for schema issues.
        }

        public String takeStack(Stack<String> strings) {
            return strings.firstElement();
        }

        //CHECKSTYLE:OFF
        public String takeUnsortedSet(HashSet<String> strings) {
            return Integer.toString(strings.size());
        }

        public String takeArrayList(ArrayList<String> strings) {
            return strings.get(0);
        }
        //CHECKSTYLE:ON

        public void mapOfMapWithStringAndPojo(Map<String, Map<String, BeanWithGregorianDate>> bigParam) {
            lastComplexMap = bigParam;
        }

        protected Map<String, Map<String, BeanWithGregorianDate>> getLastComplexMap() {
            return lastComplexMap;
        }

    }
}
