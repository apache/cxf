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
package org.apache.cxf.binding.corba.types;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.BindingFactory;
import org.apache.cxf.binding.BindingFactoryManager;
import org.apache.cxf.binding.corba.CorbaDestination;
import org.apache.cxf.binding.corba.CorbaTypeMap;
import org.apache.cxf.binding.corba.TestUtils;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.wsdl.CorbaConstants;
import org.apache.cxf.binding.corba.wsdl.TypeMappingType;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.omg.CORBA.ORB;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class CorbaHandlerUtilsTest {

    private static final String COMPLEX_TYPES_NAMESPACE_URI
        = "http://cxf.apache.org/bindings/corba/ComplexTypes/idl_types";
    private static final String COMPLEX_TYPES_PREFIX = "corbatm";

    protected EndpointInfo endpointInfo;
    BindingFactory factory;
    CorbaTypeMap typeMap;
    ServiceInfo service;

    private ORB orb;
    private Bus bus;


    @Before
    public void setUp() throws Exception {

        bus = BusFactory.getDefaultBus();
        BindingFactoryManager bfm = bus.getExtension(BindingFactoryManager.class);
        factory = bfm.getBindingFactory("http://cxf.apache.org/bindings/corba");
        bfm.registerBindingFactory(CorbaConstants.NU_WSDL_CORBA, factory);

        java.util.Properties props = System.getProperties();


        props.put("yoko.orb.id", "CXF-CORBA-Server-Binding");
        orb = ORB.init(new String[0], props);

        CorbaDestination destination = getDestination();
        service = destination.getBindingInfo().getService();
        List<TypeMappingType> corbaTypes
            = service.getDescription().getExtensors(TypeMappingType.class);
        typeMap = CorbaUtils.createCorbaTypeMap(corbaTypes);
    }

    protected CorbaDestination getDestination() throws Exception {
        TestUtils testUtils = new TestUtils();
        return testUtils.getComplexTypesTestDestination();
    }

    @After
    public void tearDown() throws Exception {
        bus.shutdown(true);
        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                // Do nothing.  Throw an Exception?
            }
        }
    }


    @Test
    public void testCreateTypeHandler() {
        // Test for an array handler
        QName objName = new QName("object");
        QName objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestArray", COMPLEX_TYPES_PREFIX);
        CorbaObjectHandler result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaArrayHandler);

        // Test for an enum handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestEnum", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaEnumHandler);

        // Test for a fixed handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestFixed", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaFixedHandler);

        // Test for a primitive handler
        objName = new QName("object");
        objIdlType = CorbaConstants.NT_CORBA_BOOLEAN;
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaPrimitiveHandler);

        // Test for a sequence handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestSequence", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaSequenceHandler);

        // Test for a struct handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestStruct", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaStructHandler);

        // Test for a union handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestUnion", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.createTypeHandler(orb, objName, objIdlType, typeMap);
        assertTrue(result instanceof CorbaUnionHandler);
    }

    @Test
    public void testInitializeObjectHandler() {
        // Test for an array handler
        QName objName = new QName("object");
        QName objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestArray", COMPLEX_TYPES_PREFIX);
        CorbaObjectHandler result =
            CorbaHandlerUtils.initializeObjectHandler(orb, objName, objIdlType, typeMap, service);
        assertTrue(result instanceof CorbaArrayHandler);
        CorbaArrayHandler arrayHandler = (CorbaArrayHandler)result;
        // WSDL defines the array to have 5 elements
        assertTrue(arrayHandler.getElements().size() == 5);


        // Test for a sequence handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestSequence", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.initializeObjectHandler(orb, objName, objIdlType, typeMap, service);
        assertTrue(result instanceof CorbaSequenceHandler);
        CorbaSequenceHandler seqHandler = (CorbaSequenceHandler)result;
        // This is an unbounded sequence so make sure there are no elements and the template
        // element has been set.
        assertTrue(seqHandler.getElements().isEmpty());
        assertNotNull(seqHandler.getTemplateElement());

        // Test for a bounded sequence handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestBoundedSequence", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.initializeObjectHandler(orb, objName, objIdlType, typeMap, service);
        assertTrue(result instanceof CorbaSequenceHandler);
        CorbaSequenceHandler boundedSeqHandler = (CorbaSequenceHandler)result;
        // This is a bounded sequence with WSDL defining 5 elements.
        assertTrue(boundedSeqHandler.getElements().size() == 5);

        // Test for a struct handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestStruct", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.initializeObjectHandler(orb, objName, objIdlType, typeMap, service);
        assertTrue(result instanceof CorbaStructHandler);
        CorbaStructHandler structHandler = (CorbaStructHandler)result;
        // The WSDL defines this struct as having three members
        assertTrue(structHandler.getMembers().size() == 3);

        // Test for a union handler
        objName = new QName("object");
        objIdlType = new QName(COMPLEX_TYPES_NAMESPACE_URI, "TestUnion", COMPLEX_TYPES_PREFIX);
        result = CorbaHandlerUtils.initializeObjectHandler(orb, objName, objIdlType, typeMap, service);
        assertTrue(result instanceof CorbaUnionHandler);
    }
}