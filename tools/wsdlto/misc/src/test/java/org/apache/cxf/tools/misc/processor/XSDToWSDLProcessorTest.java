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

package org.apache.cxf.tools.misc.processor;

import java.io.File;
import java.io.FileReader;
import java.net.URISyntaxException;

import org.apache.cxf.tools.common.ProcessorTestBase;
import org.apache.cxf.tools.misc.XSDToWSDL;
import org.junit.Test;

public class XSDToWSDLProcessorTest
    extends ProcessorTestBase {
    
    @Test
    public void testNewTypes() throws Exception {
        String[] args = new String[] {"-t", "http://org.apache/invoice", "-n", "Invoice", "-d",
                                      output.getCanonicalPath(), "-o", "Invoice_xsd.wsdl",
                                      getLocation("/misctools_wsdl/Invoice.xsd")};
        XSDToWSDL.main(args);

        File outputFile = new File(output, "Invoice_xsd.wsdl");
        assertTrue("New wsdl file is not generated: " + outputFile.getAbsolutePath(),
                   outputFile.exists());
        FileReader fileReader = new FileReader(outputFile);
        char[] chars = new char[100];
        int size = 0;
        StringBuffer sb = new StringBuffer();
        while (size < outputFile.length()) {
            int readLen = fileReader.read(chars);
            sb.append(chars, 0, readLen);
            size = size + readLen;
        }
        String serviceString = new String(sb);
        assertTrue(serviceString.indexOf("<wsdl:types>") >= 0);
        assertTrue(serviceString.indexOf("<schema targetNamespace=\"http:/"
                                         + "/apache.org/Invoice\" xmlns=\"http:/"
                                         + "/www.w3.org/2001/XMLSchema\" xmlns:soap=\"http:/"
                                         + "/schemas.xmlsoap.org/wsdl/soap/\" xmlns:tns=\"http:/"
                                         + "/apache.org/Invoice\" xmlns:wsdl=\"http:/"
                                         + "/schemas.xmlsoap.org/wsdl/\">") >= 0);
        assertTrue(serviceString.indexOf("<complexType name=\"InvoiceHeader\">") >= 0);
        
    }

    @Test
    public void testDefaultFileName() throws Exception {
        String[] args = new String[] {"-t", "http://org.apache/invoice", "-n", "Invoice", "-d",
                                      output.getCanonicalPath(), getLocation("/misctools_wsdl/Invoice.xsd")};
        XSDToWSDL.main(args);

        File outputFile = new File(output, "Invoice.wsdl");
        assertTrue("PortType file is not generated", outputFile.exists());
        FileReader fileReader = new FileReader(outputFile);
        char[] chars = new char[100];
        int size = 0;
        StringBuffer sb = new StringBuffer();
        while (size < outputFile.length()) {
            int readLen = fileReader.read(chars);
            sb.append(chars, 0, readLen);
            size = size + readLen;
        }
        String serviceString = new String(sb);
        assertTrue(serviceString.indexOf("<wsdl:types>") >= 0);
        assertTrue(serviceString.indexOf("<schema targetNamespace=\"http:/"
                                         + "/apache.org/Invoice\" xmlns=\"http:/"
                                         + "/www.w3.org/2001/XMLSchema\" xmlns:soap=\"http:/"
                                         + "/schemas.xmlsoap.org/wsdl/soap/\" xmlns:tns=\"http:/"
                                         + "/apache.org/Invoice\" xmlns:wsdl=\"http:/"
                                         + "/schemas.xmlsoap.org/wsdl/\">") >= 0);
        assertTrue(serviceString.indexOf("<complexType name=\"InvoiceHeader\">") >= 0);        
    }


    protected String getLocation(String wsdlFile) throws URISyntaxException {
        return new File(XSDToWSDLProcessorTest.class.getResource(wsdlFile).toURI()).getAbsolutePath();
    }

}
