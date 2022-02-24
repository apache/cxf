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
package org.apache.cxf.tools.java2ws;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolTestBase;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AegisTest extends ToolTestBase {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testAegisBasic() throws Exception {
        final String sei = org.apache.cxf.tools.fortest.aegis2ws.TestAegisSEI.class.getName();
        File wsdlFile = new File(folder.getRoot(), "aegis.wsdl");
        String[] args = new String[] {"-wsdl", "-o", wsdlFile.toString(), "-verbose",
                                      "-d", folder.getRoot().toString(),
                                      "-s", folder.getRoot().toString(),
                                      "-frontend", "jaxws", "-databinding", "aegis",
                                      "-client", "-server", sei};
        JavaToWS.main(args);
        assertTrue("wsdl is not generated", wsdlFile.exists());

        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        Definition def = reader.readWSDL(wsdlFile.toURI().toURL().toString());
        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
        assertValid("//xsd:element[@type='ns0:Something']", wsdl);
    }

    @Test
    public void testAegisReconfigureDatabinding() throws Exception {
        final String sei = org.apache.cxf.tools.fortest.aegis2ws.TestAegisSEI.class.getName();
        File wsdlFile = new File(folder.getRoot(), "aegis.wsdl");
        String[] args = new String[] {"-wsdl", "-o", wsdlFile.toString(),
                                      "-beans",
            new File(getClass().getResource("/revisedAegisDefaultBeans.xml").toURI()).toString(),
                                      "-verbose", "-s", folder.getRoot().toString(), 
                                      "-frontend", "jaxws", "-databinding", "aegis",
                                      "-client", "-server", sei};
        JavaToWS.main(args);
        assertTrue("wsdl is not generated " + getStdErr(), wsdlFile.exists());

        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        reader.setFeature("javax.wsdl.verbose", false);
        Definition def = reader.readWSDL(wsdlFile.toURI().toURL().toString());
        Document wsdl = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
        assertValid("//xsd:element[@type='ns0:Something']", wsdl);
        XPathUtils xpu = new XPathUtils(getNSMap());

        String s = (String)xpu.getValue("//xsd:complexType[@name='takeSomething']/"
                                + "xsd:sequence/xsd:element[@name='arg0']/@minOccurs",
                                wsdl, XPathConstants.STRING);
        assertEquals("50", s);
        assertFalse(xpu.isExist("//xsd:complexType[@name='Something']/xsd:sequence/"
                                + "xsd:element[@name='singular']/@minOccurs",
                         wsdl, XPathConstants.NODE));
    }


    private static Map<String, String> getNSMap() {
        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("s", "http://schemas.xmlsoap.org/soap/envelope/");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("wsdl", "http://schemas.xmlsoap.org/wsdl/");
        namespaces.put("wsdlsoap", "http://schemas.xmlsoap.org/wsdl/soap/");
        namespaces.put("soap", "http://schemas.xmlsoap.org/soap/");
        namespaces.put("soap12env", "http://www.w3.org/2003/05/soap-envelope");
        namespaces.put("xml", "http://www.w3.org/XML/1998/namespace");
        namespaces.put("ns0", "http://aegis2ws.fortest.tools.cxf.apache.org/");
        return namespaces;
    }

    private static void assertValid(String xpathExpression, Document doc) {
        XPathUtils xpu = new XPathUtils(getNSMap());
        if (!xpu.isExist(xpathExpression, doc, XPathConstants.NODE)) {
            Assert.fail("Failed to select any nodes for expression:\n" + xpathExpression
                        + " from document:\n" + StaxUtils.toString(doc));
        }
    }
}