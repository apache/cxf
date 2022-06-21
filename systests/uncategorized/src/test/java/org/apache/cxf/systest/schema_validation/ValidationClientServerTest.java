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
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.WebServiceException;
import org.apache.cxf.annotations.SchemaValidation.SchemaValidationType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.schema_validation.DoSomethingFault;
import org.apache.schema_validation.SchemaValidation;
import org.apache.schema_validation.SchemaValidationService;
import org.apache.schema_validation.types.ComplexStruct;
import org.apache.schema_validation.types.OccuringStruct;
import org.apache.schema_validation.types.SomeRequest;
import org.apache.schema_validation.types.SomeResponse;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ValidationClientServerTest extends AbstractBusClientServerTestBase {
    public static final String PORT = ValidationServer.PORT;

    private static Locale oldLocale;

    private final QName serviceName = new QName("http://apache.org/schema_validation",
                                                "SchemaValidationService");
    private final QName portName = new QName("http://apache.org/schema_validation", "SoapPort");

    @BeforeClass
    public static void startservers() throws Exception {
        oldLocale = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        assertTrue("server did not launch correctly", launchServer(ValidationServer.class, true));
    }

    @AfterClass
    public static void resetLocale() {
        Locale.setDefault(oldLocale);
    }

    @Test
    public void testSchemaValidationProviderPayload() throws Exception {
        doProviderTest("PProvider");
    }
    @Test
    public void testSchemaValidationProviderMessage() throws Exception {
        doProviderTest("MProvider");
    }
    private void doProviderTest(String postfix) throws Exception {
        SchemaValidation validation = createService(Boolean.FALSE, postfix);
        SomeRequest req = new SomeRequest();
        req.setId("9999999999");
        try {
            validation.doSomething(req);
            fail("Should have faulted");
        } catch (DoSomethingFault e) {
            assertEquals("1234", e.getFaultInfo().getErrorCode());
        }
        req.setId("8888888888");
        try {
            validation.doSomething(req);
            fail("Should have faulted");
        } catch (DoSomethingFault e) {
            fail("Should not have happened");
        } catch (WebServiceException e) {
            String expected = "Value '1' is not facet-valid";
            assertTrue(e.getMessage().indexOf(expected) != -1);
        }
        ((java.io.Closeable)validation).close();
    }

    @Test
    public void testSchemaValidationServer() throws Exception {
        SchemaValidation validation = createService(Boolean.FALSE, "SoapPortValidate");
        runSchemaValidationTest(validation);
        ((java.io.Closeable)validation).close();
    }

    @Test
    public void testSchemaValidationServerForMethod() throws Exception {
        SchemaValidation validation = createService(Boolean.FALSE, "SoapPortMethodValidate");
        ComplexStruct complexStruct = new ComplexStruct();
        complexStruct.setElem1("one");
        complexStruct.setElem3(3);
        try {
            validation.setComplexStruct(complexStruct);
            fail("Set ComplexStruct should have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":elem2}' is expected.";
            assertTrue(e.getMessage(), e.getMessage().indexOf(expected) != -1);
        }

        SchemaValidation novlidation = createService(Boolean.FALSE, "SoapPort");
        try {
            novlidation.setComplexStruct(complexStruct);

        } catch (WebServiceException e) {
            fail("Exception is not expected :" + e);
        }

    }

    @Test
    public void testSchemaValidationClient() throws Exception {
        SchemaValidation validation = createService(Boolean.TRUE, "SoapPort");
        runSchemaValidationTest(validation);
        ((java.io.Closeable)validation).close();
    }
    private void runSchemaValidationTest(SchemaValidation validation) {
        ComplexStruct complexStruct = new ComplexStruct();
        complexStruct.setElem1("one");
        // Don't initialize a member of the structure.
        // Client side validation should throw an exception.
        // complexStruct.setElem2("two");
        complexStruct.setElem3(3);
        try {
            /*boolean result =*/
            validation.setComplexStruct(complexStruct);
            fail("Set ComplexStruct should have thrown ProtocolException");
        } catch (WebServiceException e) {
            String expected = "'{\"http://apache.org/schema_validation/types\":elem2}' is expected.";
            assertTrue(e.getMessage(), e.getMessage().indexOf(expected) != -1);
        }

        OccuringStruct occuringStruct = new OccuringStruct();
        // Populate the list in the wrong order.
        // Client side validation should throw an exception.
        List<Serializable> floatIntStringList = occuringStruct.getVarFloatAndVarIntAndVarString();
        floatIntStringList.add(Integer.valueOf(42));
        floatIntStringList.add(Float.valueOf(4.2f));
        floatIntStringList.add("Goofus and Gallant");
        try {
            /*boolean result =*/
            validation.setOccuringStruct(occuringStruct);
            fail("Set OccuringStruct should have thrown ProtocolException");
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


        SomeRequest req = new SomeRequest();
        req.setId("9999999999");
        try {
            validation.doSomething(req);
            fail("Should have faulted");
        } catch (DoSomethingFault e) {
            assertEquals("1234", e.getFaultInfo().getErrorCode());
        }
        req.setId("8888888888");
        try {
            validation.doSomething(req);
            fail("Should have faulted");
        } catch (DoSomethingFault e) {
            fail("Should not have happened");
        } catch (WebServiceException e) {
            String expected = "Value '1' is not facet-valid";
            assertTrue(e.getMessage().indexOf(expected) != -1);
        }
    }

    @Test
    public void testRequestFailedSchemaValidation() throws Exception {
        assertFailedRequestValidation(Boolean.TRUE);
    }

    @Test
    public void testFailedRequestSchemaValidationTypeBoth() throws Exception {
        assertFailedRequestValidation(SchemaValidationType.BOTH.name());
    }

    @Test
    public void testFailedSchemaValidationSchemaValidationTypeOut() throws Exception {
        assertFailedRequestValidation(SchemaValidationType.OUT.name());
    }

    @Test
    public void testIgnoreRequestSchemaValidationNone() throws Exception {
        assertIgnoredRequestValidation(SchemaValidationType.NONE.name());
    }

    @Test
    public void testIgnoreRequestSchemaValidationResponseOnly() throws Exception {
        assertIgnoredRequestValidation(SchemaValidationType.IN.name());
    }

    @Test
    public void testIgnoreRequestSchemaValidationFalse() throws Exception {
        assertIgnoredRequestValidation(Boolean.FALSE);
    }

    @Test
    public void testResponseFailedSchemaValidation() throws Exception {
        assertFailureResponseValidation(Boolean.TRUE);
    }

    @Test
    public void testResponseSchemaFailedValidationBoth() throws Exception {
        assertFailureResponseValidation(SchemaValidationType.BOTH.name());
    }

    @Test
    public void testResponseSchemaFailedValidationIn() throws Exception {
        assertFailureResponseValidation(SchemaValidationType.IN.name());
    }

    @Test
    public void testIgnoreResponseSchemaFailedValidationNone() throws Exception {
        assertIgnoredResponseValidation(SchemaValidationType.NONE.name());
    }

    @Test
    public void testIgnoreResponseSchemaFailedValidationFalse() throws Exception {
        assertIgnoredResponseValidation(Boolean.FALSE);
    }

    @Test
    public void testIgnoreResponseSchemaFailedValidationOut() throws Exception {
        assertIgnoredResponseValidation(SchemaValidationType.OUT.name());
    }

    @Test
    public void testHeaderValidation() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/schema_validation.wsdl");
        assertNotNull(wsdl);
        SchemaValidationService service = new SchemaValidationService(wsdl, serviceName);
        assertNotNull(service);


        String smsg = "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'><soap:Header>"
            + "<SomeHeader soap:mustUnderstand='1' xmlns='http://apache.org/schema_validation/types'>"
            + "<id>1111111111</id></SomeHeader>"
            + "</soap:Header><soap:Body><SomeRequestWithHeader xmlns='http://apache.org/schema_validation/types'>"
            + "<id>1111111111</id></SomeRequestWithHeader></soap:Body></soap:Envelope>";
        Dispatch<Source> dsp = service.createDispatch(SchemaValidationService.SoapPort, Source.class, Mode.MESSAGE);
        updateAddressPort(dsp, PORT);
        dsp.invoke(new StreamSource(new StringReader(smsg)));
    }

    private SomeResponse execute(SchemaValidation service, String id) throws Exception {
        SomeRequest request = new SomeRequest();
        request.setId(id);
        return service.doSomething(request);
    }

    private void assertFailureResponseValidation(Object validationConfig) throws Exception {
        SchemaValidation service = createService(validationConfig);

        SomeResponse response = execute(service, "1111111111"); // valid request
        assertEquals(response.getTransactionId(), "aaaaaaaaaa");

        try {
            execute(service, "1234567890"); // valid request, but will result in invalid response
            fail("should catch marshall exception as the invalid incoming message per schema");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Unmarshalling Error"));
            assertTrue(e.getMessage().contains("is not facet-valid with respect to pattern"));
        }

        ((java.io.Closeable)service).close();
    }

    private void assertIgnoredRequestValidation(Object validationConfig) throws Exception {
        SchemaValidation service = createService(validationConfig);

        // this is an invalid request but validation is turned off.
        SomeResponse response = execute(service, "1234567890aaaa");
        assertEquals(response.getTransactionId(), "aaaaaaaaaa");
        ((java.io.Closeable)service).close();
    }

    private void assertIgnoredResponseValidation(Object validationConfig) throws Exception {
        SchemaValidation service = createService(validationConfig);

        // the request will result in invalid response but validation is turned off
        SomeResponse response = execute(service, "1234567890");
        assertEquals(response.getTransactionId(), "aaaaaaaaaaxxx");
        ((java.io.Closeable)service).close();
    }

    private void assertFailedRequestValidation(Object validationConfig) throws Exception {
        SchemaValidation service = createService(validationConfig);

        SomeResponse response = execute(service, "1111111111");
        assertEquals(response.getTransactionId(), "aaaaaaaaaa");

        try {
            execute(service, "1234567890aaa");
            fail("should catch marshall exception as the invalid outgoing message per schema");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Marshalling Error"));
            assertTrue(e.getMessage().contains("is not facet-valid with respect to pattern"));
        }
        ((java.io.Closeable)service).close();
    }

    private SchemaValidation createService(Object validationConfig) throws Exception {
        return createService(validationConfig, "SoapPort");
    }
    private SchemaValidation createService(Object validationConfig, String postfix) throws Exception {
        URL wsdl = getClass().getResource("/wsdl/schema_validation.wsdl");
        assertNotNull(wsdl);

        SchemaValidationService service = new SchemaValidationService(wsdl, serviceName);
        assertNotNull(service);

        SchemaValidation validation = service.getPort(portName, SchemaValidation.class);
        setAddress(validation, "http://localhost:" + PORT + "/SoapContext/" + postfix);

        ((BindingProvider)validation).getRequestContext().put(Message.SCHEMA_VALIDATION_ENABLED, validationConfig);
        ((BindingProvider)validation).getResponseContext().put(Message.SCHEMA_VALIDATION_ENABLED, validationConfig);
        new LoggingFeature().initialize((Client)validation, getBus());
        return validation;
    }
}
