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

package org.apache.cxf.aegis.type.array;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.frontend.ServerFactoryBean;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FlatArrayTest extends AbstractAegisTest {

    private static final int[] INT_ARRAY = new int[] {4, 6, 12};
    private static final String[] STRING_ARRAY = {
        "fillo", "dough"
    };
    private Document arrayWsdlDoc;
    private FlatArrayService service;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        service = new FlatArrayService();

        ServerFactoryBean sf = new ServerFactoryBean();
        // use full parameter names.
        sf.setServiceClass(FlatArrayServiceInterface.class);
        sf.setServiceBean(service);
        sf.setAddress("local://FlatArray");
        sf.setDataBinding(new AegisDatabinding());
        sf.create();

        arrayWsdlDoc = getWSDLDocument("FlatArrayServiceInterface");
    }

    @Test
    public void testXmlConfigurationOfParameterTypeSchema() throws Exception {
        NodeList typeList = assertValid(
                                        "/wsdl:definitions/wsdl:types"
                                            + "/xsd:schema"
                                            + "[@targetNamespace='http://array.type.aegis.cxf.apache.org/']"
                                            + "/xsd:complexType[@name=\"submitStringArray\"]"
                                            + "/xsd:sequence/xsd:element" + "[@name='array']", arrayWsdlDoc);
        Element typeElement = (Element)typeList.item(0);
        String nillableValue = typeElement.getAttribute("nillable");
        assertTrue(nillableValue == null || "".equals(nillableValue) || "false".equals(nillableValue));
        String typeString = typeElement.getAttribute("type");
        assertEquals("xsd:string", typeString); // no ArrayOf

        typeList = assertValid("/wsdl:definitions/wsdl:types"
                               + "/xsd:schema[@targetNamespace='http://array.type.aegis.cxf.apache.org']"
                               + "/xsd:complexType[@name='BeanWithFlatArray']"
                               + "/xsd:sequence/xsd:element[@name='values']", arrayWsdlDoc);
        typeElement = (Element)typeList.item(0);
        assertValidBoolean("@type='xsd:int'", typeElement);
    }

    @Test
    public void testDataMovementPart() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://FlatArray");
        proxyFac.setBus(getBus());

        FlatArrayServiceInterface client = proxyFac.create(FlatArrayServiceInterface.class);
        client.submitStringArray(STRING_ARRAY);
        assertArrayEquals(STRING_ARRAY, service.stringArrayValue);
    }

    @Test
    public void testDataMovementBean() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://FlatArray");
        proxyFac.setBus(getBus());

        FlatArrayServiceInterface client = proxyFac.create(FlatArrayServiceInterface.class);
        BeanWithFlatArray bwfa = new BeanWithFlatArray();
        bwfa.setValues(INT_ARRAY);
        client.takeBeanWithFlatArray(bwfa);
        assertArrayEquals(INT_ARRAY, service.beanWithFlatArrayValue.getValues());
    }
    @Test
    public void testFlatCollection() throws Exception {
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://FlatArray");
        proxyFac.setBus(getBus());

        FlatArrayServiceInterface client = proxyFac.create(FlatArrayServiceInterface.class);
        BeanWithFlatCollection bwfc = new BeanWithFlatCollection();
        bwfc.getValues().add(1);
        bwfc.getValues().add(2);
        bwfc.getValues().add(3);
        bwfc = client.echoBeanWithFlatCollection(bwfc);
        assertEquals(3, bwfc.getValues().size());
        assertEquals(Integer.valueOf(1), bwfc.getValues().get(0));
        assertEquals(Integer.valueOf(2), bwfc.getValues().get(1));
        assertEquals(Integer.valueOf(3), bwfc.getValues().get(2));
    }

}