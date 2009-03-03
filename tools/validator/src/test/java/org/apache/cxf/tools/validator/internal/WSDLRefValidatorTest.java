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

package org.apache.cxf.tools.validator.internal;

import java.util.HashSet;
import java.util.Set;
import javax.wsdl.Definition;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.validator.internal.model.XNode;
import org.apache.cxf.wsdl11.WSDLDefinitionBuilder;
import org.junit.Assert;
import org.junit.Test;

public class WSDLRefValidatorTest extends Assert {

    private Definition getWSDL(String wsdl) throws Exception {
        Bus b = BusFactory.getDefaultBus();
        WSDLDefinitionBuilder wsdlBuilder = new WSDLDefinitionBuilder(b);
        return wsdlBuilder.build(wsdl);
    }
    
    @Test
    public void testNoService() throws Exception {
        String wsdl = getClass().getResource("resources/b.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        assertFalse(validator.isValid());
        ValidationResult results = validator.getValidationResults();
        assertEquals(0, results.getWarnings().size());
    }

    @Test
    public void testWSDLImport1() throws Exception {
        String wsdl = getClass().getResource("resources/a.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        validator.isValid();
        ValidationResult results = validator.getValidationResults();
        assertEquals(2, results.getErrors().size());
        String t = results.getErrors().pop();
        String text = "{http://apache.org/hello_world/messages}[portType:GreeterA][operation:sayHi]";
        Message msg;
        if (StaxUtils.isWoodstox()) {
            msg = new Message("FAILED_AT_POINT",
                              WSDLRefValidator.LOG,
                              27,
                              2,
                              new java.net.URI(wsdl).toURL(),
                              text);
        } else {
            // sjsxp
            msg = new Message("FAILED_AT_POINT",
                              WSDLRefValidator.LOG,
                              -1,
                              -1,
                              new java.net.URI(wsdl).toURL(),
                              text);
        }
        assertEquals(msg.toString(), t);
    }
    

    @Test
    public void testWSDLImport2() throws Exception {
        String wsdl = getClass().getResource("resources/physicalpt.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        assertTrue(validator.isValid());
        String expected = "/wsdl:definitions[@targetNamespace='http://schemas.apache.org/yoko/idl/OptionsPT']"
            + "/wsdl:portType[@name='foo.bar']";

        Set<String> xpath = new HashSet<String>();

        for (XNode node : validator.vNodes) {
            xpath.add(node.toString());
        }
        assertTrue(xpath.contains(expected));
    }

    @Test
    public void testNoTypeRef() throws Exception {
        String wsdl = getClass().getResource("resources/NoTypeRef.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        assertFalse(validator.isValid());
        assertEquals(3, validator.getValidationResults().getErrors().size());

        String expected = "Part <header_info> in Message "
            + "<{http://apache.org/samples/headers}inHeaderRequest>"
            + " referenced Type <{http://apache.org/samples/headers}SOAPHeaderInfo> "
            + "can not be found in the schemas";
        String t = null;
        while (!validator.getValidationResults().getErrors().empty()) {
            t = validator.getValidationResults().getErrors().pop();
            if (expected.equals(t)) {
                break;
            }
        }
        assertEquals(expected, t);
    }

    @Test
    public void testNoBindingWSDL() throws Exception {
        String wsdl = getClass().getResource("resources/nobinding.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        validator.isValid();
        ValidationResult results = validator.getValidationResults();

        assertEquals(0, results.getWarnings().size());

        WSDLRefValidator v = new WSDLRefValidator(getWSDL(wsdl), null);
        v.setSuppressWarnings(true);
        assertTrue(v.isValid());
    }

    @Test
    public void testLogicalWSDL() throws Exception {
        String wsdl = getClass().getResource("resources/logical.wsdl").toURI().toString();
        WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
        validator.isValid();
        ValidationResult results = validator.getValidationResults();
        
        assertEquals(1, results.getErrors().size());
        String text = "{http://schemas.apache.org/yoko/idl/OptionsPT}[message:getEmployee]";
        Message msg;
        if (StaxUtils.isWoodstox()) {
            msg = new Message("FAILED_AT_POINT",
                              WSDLRefValidator.LOG,
                              42,
                              6,
                              new java.net.URI(wsdl).toURL(),
                              text);
        } else {
            msg = new Message("FAILED_AT_POINT",
                              WSDLRefValidator.LOG,
                              -1,
                              -1,
                              new java.net.URI(wsdl).toURL(),
                              text);
        }
        assertEquals(msg.toString(), results.getErrors().pop());
    }

    @Test
    public void testNotAWsdl() throws Exception {
        try {        
            String wsdl = getClass().getResource("resources/c.xsd").toURI().toString();
            WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
            validator.isValid();
        } catch (Exception e) {
            String expected = "WSDLException (at /xs:schema): faultCode=INVALID_WSDL: "
                + "Expected element '{http://schemas.xmlsoap.org/wsdl/}definitions'.";
            assertTrue(e.getMessage().contains(expected));
        }
    }

    @Test
    public void testXSDAnyType() throws Exception {
        String wsdl = getClass().getResource("resources/anytype.wsdl").toURI().toString();
        try {
            WSDLRefValidator validator = new WSDLRefValidator(getWSDL(wsdl), null);
            assertTrue(validator.isValid());
        } catch (Exception e) {
            fail("Valid wsdl, no exception should be thrown" + e.getMessage());
        }
    }
}
