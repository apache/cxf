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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.collection.CollectionType;
import org.apache.cxf.aegis.type.java5.dto.CollectionDTO;
import org.apache.cxf.aegis.type.java5.dto.DTOService;
import org.apache.cxf.aegis.type.java5.dto.ObjectDTO;
import org.apache.cxf.common.util.SOAPConstants;
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

        Type type = creator.createType(m, -1);
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

        Type type = creator.createType(m, -1);
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
        Type type = creator.createType(pd);
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

        Type dto = creator.createType(CollectionDTO.class);
        Set deps = dto.getDependencies();

        Type type = (Type)deps.iterator().next();

        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;

        deps = dto.getDependencies();
        assertEquals(1, deps.size());

        Type comType = colType.getComponentType();
        assertEquals(String.class, comType.getTypeClass());
    }

    @Test
    public void testObjectDTO() {
        tm = new DefaultTypeMapping(SOAPConstants.XSD);
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);

        Type dto = creator.createType(ObjectDTO.class);
        Set deps = dto.getDependencies();

        assertFalse(deps.isEmpty());

        Type type = (Type)deps.iterator().next();

        assertTrue(type instanceof CollectionType);

        CollectionType colType = (CollectionType)type;

        deps = dto.getDependencies();
        assertEquals(1, deps.size());

        Type comType = colType.getComponentType();
        assertEquals(Object.class, comType.getTypeClass());
    }

    @Test
    public void testCollectionDTOService() throws Exception {
        createService(DTOService.class);
        invoke("DTOService", "/org/apache/cxf/aegis/type/java5/dto/GetDTO.xml");
    }

    @Test
    public void testCollectionServiceWSDL() throws Exception {
        
        createService(CollectionService.class, new CollectionService(), null);

        Document wsdl = getWSDLDocument("CollectionService");
        assertValid("//xsd:element[@name='return'][@type='tns:ArrayOfString']", wsdl);
    }

    @Test
    public void testUnannotatedStrings() throws Exception {        
        createService(CollectionService.class, new CollectionService(), null);

        Document doc = getWSDLDocument("CollectionService");
        // printNode(doc);
        assertValid(
                    "//xsd:complexType[@name='getUnannotatedStringsResponse']"
                    + "/xsd:sequence/xsd:element[@type='tns:ArrayOfString']",
                    doc);
    }
    
    @Test
    public void testDoubleList() throws Exception {
        createService(CollectionService.class, new CollectionService(), null);
        Document doc = getWSDLDocument("CollectionService");
        assertValid(
                    "//xsd:complexType[@name='ArrayOfDouble']"
                    + "/xsd:sequence/xsd:element[@type='xsd:double']",
                    doc);
        
    }

    public class CollectionService {
        
        public Collection<String> getStrings() {
            return null;
        }

        public void setLongs(Collection<Long> longs) {
        }

        public Collection getUnannotatedStrings() {
            return null;
        }

        public Collection<Collection<String>> getStringCollections() {
            return null;
        }
        
        public void takeDoubleList(List<Double> doublesList) {
        }
    }
}
