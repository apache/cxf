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

package org.apache.cxf.systest.schema_validation;

import java.io.Serializable;
import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.schema_validation.SchemaValidation;
import org.apache.schema_validation.SchemaValidationService;
import org.apache.schema_validation.types.ComplexStruct;
import org.apache.schema_validation.types.OccuringStruct;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidationClientServerTest extends AbstractBusClientServerTestBase {

    private final QName serviceName = new QName("http://apache.org/schema_validation",
                                                "SchemaValidationService");
    private final QName portName = new QName("http://apache.org/schema_validation", "SoapPort");


    @BeforeClass
    public static void startservers() throws Exception {
        // set up configuration to enable schema validation
        URL url = ValidationClientServerTest.class.getResource("cxf-config.xml");
        assertNotNull("cannot find test resource", url);
        defaultConfigFileName = url.toString();

        assertTrue("server did not launch correctly", launchServer(ValidationServer.class));
    }

    // TODO : Change this test so that we test the combinations of
    // client and server with schema validation enabled/disabled...
    // Only tests client side validation enabled/server side disabled.
    @Test
    public void testSchemaValidation() throws Exception {
        System.setProperty("cxf.config.file.url", getClass().getResource("cxf-config.xml").toString());
        URL wsdl = getClass().getResource("/wsdl/schema_validation.wsdl");
        assertNotNull(wsdl);

        SchemaValidationService service = new SchemaValidationService(wsdl, serviceName);
        assertNotNull(service);

        SchemaValidation validation = service.getPort(portName, SchemaValidation.class);

        ComplexStruct complexStruct = new ComplexStruct();
        complexStruct.setElem1("one");
        // Don't initialize a member of the structure.  
        // Client side validation should throw an exception.
        // complexStruct.setElem2("two");
        complexStruct.setElem3(3);
        try {
            /*boolean result =*/
            validation.setComplexStruct(complexStruct);
            fail("Set ComplexStruct hould have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":elem2}' is expected.";
            assertTrue(e.getMessage(), e.getMessage().indexOf(expected) != -1);
        }

        OccuringStruct occuringStruct = new OccuringStruct();
        // Populate the list in the wrong order.
        // Client side validation should throw an exception.
        List<Serializable> floatIntStringList = occuringStruct.getVarFloatAndVarIntAndVarString();
        floatIntStringList.add(new Integer(42));
        floatIntStringList.add(new Float(4.2f));
        floatIntStringList.add("Goofus and Gallant");
        try {
            /*boolean result =*/
            validation.setOccuringStruct(occuringStruct);
            fail("Set OccuringStruct hould have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":varFloat}' is expected.";
            assertTrue(e.getMessage().indexOf(expected) != -1);
        }

        try {
            // The server will attempt to return an invalid ComplexStruct
            // When validation is disabled on the server side, we'll get the
            // exception while unmarshalling the invalid response.
            /*complexStruct =*/
            validation.getComplexStruct("Hello");
            fail("Get ComplexStruct should have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":elem2}' is expected.";
            assertTrue("Found message " + e.getMessage(), 
                       e.getMessage().indexOf(expected) != -1);
        }

        try {
            // The server will attempt to return an invalid OccuringStruct
            // When validation is disabled on the server side, we'll get the
            // exception while unmarshalling the invalid response.
            /*occuringStruct =*/
            validation.getOccuringStruct("World");
            fail("Get OccuringStruct should have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":varFloat}' is expected.";
            assertTrue(e.getMessage().indexOf(expected) != -1);
        }
    }

}
