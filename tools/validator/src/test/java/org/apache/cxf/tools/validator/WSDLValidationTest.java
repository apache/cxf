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
package org.apache.cxf.tools.validator;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolTestBase;
import org.junit.Before;
import org.junit.Test;

public class WSDLValidationTest extends ToolTestBase {
    @Before    
    public void setUp() {
        super.setUp();
    }
    
    @Test
    public void testValidateDefaultOpMessageNames() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/defaultOpMessageNames.wsdl")};
        WSDLValidator.main(args);
        assertTrue(this.getStdOut().contains("Valid WSDL"));
    }

    @Test
    public void testValidateUniqueBody() throws Exception {
        String[] args = new String[] {"-verbose", getLocation("/validator_wsdl/doc_lit_bare.wsdl")};
        WSDLValidator.main(args);
        assertTrue("Non Unique Body Parts Error should be discovered: " + getStdErr(),
                   getStdErr().indexOf("Non unique body part") > -1);
    }

    @Test
    public void testValidateMixedStyle() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_mixed_style.wsdl")};
        WSDLValidator.main(args);
        assertTrue("Mixed style. Error should have been discovered: " + getStdErr(),
                   getStdErr().indexOf("Mixed style, invalid WSDL") > -1);
    }

    @Test
    public void testValidateTypeElement() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_doc_lit_type.wsdl")};
        WSDLValidator.main(args);
        assertTrue("Must refer to type element error should have been discovered: " + getStdErr(),
                   getStdErr().indexOf("using the element attribute") > -1);
    }

    @Test
    public void testValidateAttribute() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_error_attribute.wsdl")};
        WSDLValidator.main(args);
        String expected = "WSDLException (at /wsdl:definitions/wsdl:message[1]/wsdl:part): "
            + "faultCode=INVALID_WSDL: Encountered illegal extension attribute 'test'. "
            + "Extension attributes must be in a namespace other than WSDL's";
        assertTrue("Attribute error should be discovered: " + getStdErr(),
                   getStdErr().indexOf(expected) > -1);
    }

    @Test
    public void testValidateReferenceError() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_error_reference.wsdl")};
        WSDLValidator.main(args);
        String error = getStdErr();
        if (StaxUtils.isWoodstox()) {
            // sjsxp doesn't report locations.
            assertTrue("error message does not contain [147,3]. error message: "
                + error, error.indexOf("[147,3]") != -1);
        }
        assertTrue(error.indexOf("Caused by {http://apache.org/hello_world_soap_http}"
                                       + "[binding:Greeter_SOAPBinding1] not exist.") != -1);
    }

    @Test
    public void testBug305872() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/bug305872/http.xsd")};
        WSDLValidator.main(args);
        String expected = "Expected element '{http://schemas.xmlsoap.org/wsdl/}definitions'.";
        assertTrue("Tools should check if this file is a wsdl file: " + getStdErr(),
                   getStdErr().indexOf(expected) > -1);
    }

    @Test
    public void testImportWsdlValidation() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_import.wsdl")};
        WSDLValidator.main(args);
        
        assertTrue("Is not valid wsdl!: " + getStdOut(),
                   getStdOut().indexOf("Passed Validation") > -1);
    }

    @Test
    public void testImportSchemaValidation() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/hello_world_schema_import.wsdl")};
        WSDLValidator.main(args);
        
        assertTrue("Is not valid wsdl: " + getStdOut(),
                   getStdOut().indexOf("Passed Validation") > -1);
    }
    @Test
    public void testSOAPHeadersInMultiOperations() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/cxf1793.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr(), getStdOut().indexOf("Passed Validation : Valid WSDL") > -1);
    }


    @Test
    public void testWSIBP2210() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/soapheader.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2210") > -1);
    }

    @Test
    public void testWSIBPR2726() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/jms_test.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2726") > -1);
    }

    @Test
    public void testWSIBPR2205() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/jms_test2.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2205") > -1);
    }

    @Test
    public void testWSIBPR2203() throws Exception {
        String[] args = new String[] {"-verbose",
                                      getLocation("/validator_wsdl/header_rpc_lit.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdOut().indexOf("Passed Validation : Valid WSDL") > -1);

        args = new String[] {"-verbose",
                             getLocation("/validator_wsdl/header_rpc_lit_2203_in.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr().indexOf("soapbind:body element(s), only to wsdl:part element(s)") > -1);

        args = new String[] {"-verbose",
                             getLocation("/validator_wsdl/header_rpc_lit_2203_out.wsdl")};
        WSDLValidator.main(args);
        assertTrue(getStdErr().indexOf("soapbind:body element(s), only to wsdl:part element(s)") > -1);
    }

    @Test
    public void testBPR2717() throws Exception {
        try {
            String[] args = new String[] {"-verbose",
                                          getLocation("/validator_wsdl/cxf996.wsdl")};
            WSDLValidator.main(args);
        } catch (Exception e) {
            assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2717") == -1);
            assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2210") != -1);
        }

        try {
            String[] args = new String[] {"-verbose",
                                          getLocation("/validator_wsdl/bp2717.wsdl")};
            WSDLValidator.main(args);
        } catch (Exception e) {
            assertTrue(getStdErr().indexOf("WSI-BP-1.0 R2717") != -1);
        }
    }

    @Override
    protected String getLocation(String wsdlFile) throws Exception {
        Enumeration<URL> e = WSDLValidationTest.class.getClassLoader().getResources(wsdlFile);
        while (e.hasMoreElements()) {
            URL u = e.nextElement();
            File f = new File(u.toURI());
            if (f.exists() && f.isDirectory()) {
                return f.toString();
            }
        }

        return WSDLValidationTest.class.getResource(wsdlFile).toURI().getPath();
    }
}
