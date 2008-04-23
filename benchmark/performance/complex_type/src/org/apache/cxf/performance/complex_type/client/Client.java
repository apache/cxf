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
package org.apache.cxf.performance.complex_type.client;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.namespace.QName;

import org.apache.cxf.pat.internal.TestCaseBase;
import org.apache.cxf.pat.internal.TestResult;
import org.apache.cxf.performance.complex_type.ComplexPortType;
import org.apache.cxf.performance.complex_type.ComplexService;
import org.apache.cxf.performance.complex_type.server.Server;
import org.apache.cxf.performance.complex_type.types.ColourEnum;
import org.apache.cxf.performance.complex_type.types.NestedComplexType;
import org.apache.cxf.performance.complex_type.types.NestedComplexTypeSeq;
import org.apache.cxf.performance.complex_type.types.SimpleStruct;

public final class Client extends TestCaseBase<ComplexPortType> {
    private static final QName SERVICE_NAME = new QName(
                                             "http://cxf.apache.org/performance/complex_type",
                                             "ComplexService");          
    private static final QName PORT_NAME = new QName(
                                          "http://cxf.apache.org/performance/complex_type",
                                          "ComplexPortType");
    private ComplexService cs;
    private final NestedComplexTypeSeq complexTypeSeq = new NestedComplexTypeSeq();
       
    public Client(String[] args) {
        super("Complex Type TestCase", args);
        serviceName = "ComplexService";
        portName = "ComplexPortType";
        operationName = "sendReceiveData";
        wsdlNameSpace = "http://cxf.apache.org/performance/complex_type";
        amount = 30;
        packetSize = 1;
    }

    public static void main(String args[]) throws Exception {
                       
        Client client = new Client(args);
        
        client.initialize(); 
        
        client.run();
        
        List results = client.getTestResults();
        TestResult testResult = null;
        for (Iterator iter = results.iterator(); iter.hasNext();) {
            testResult = (TestResult)iter.next();
            System.out.println("Throughput " + testResult.getThroughput());
            System.out.println("AVG Response Time " + testResult.getAvgResponseTime());
        }
        System.out.println("cxf client is going to shutdown!");
        System.exit(0);
    }
    
    private SimpleStruct getSimpleStruct() throws DatatypeConfigurationException {
        SimpleStruct ss = new SimpleStruct();
        ss.setVarFloat(Float.MAX_VALUE);
        ss.setVarShort(Short.MAX_VALUE);
        ss.setVarByte(Byte.MAX_VALUE);
        ss.setVarDecimal(new BigDecimal("3.1415926"));
        ss.setVarDouble(Double.MAX_VALUE);
        ss.setVarString("1234567890!@#$%^&*()abcdefghijk");
        ss.setVarAttrString("1234567890!@#$%^&*()abcdefghijk");
        ss.setVarDateTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(2005, 12, 3, 0, 0, 9, 0, 0));
        return ss;
    }

    
    public void initTestData() {
        NestedComplexType  complexType  = new NestedComplexType();
        complexType.setVarString("#12345ABc");
        complexType.setVarUByte(Short.MAX_VALUE);
        complexType.setVarUnsignedLong(new BigInteger("13691056728"));
        complexType.setVarFloat(Float.MAX_VALUE);
        complexType.setVarQName(new QName("return", "return"));
        try {
            complexType.setVarStruct(getSimpleStruct());
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
         
        complexType.setVarEnum(ColourEnum.RED);
        byte[] binary = new byte[1024];
        for (int idx = 0; idx < 4; idx++) {
            for (int jdx = 0; jdx < 256; jdx++) {
                binary[idx * 256 + jdx] = (byte)(jdx - 128);
            }
        }
        complexType.setVarBase64Binary(binary);
        complexType.setVarHexBinary(binary);

        for (int i = 0; i < packetSize; i++) {
            complexTypeSeq.getItem().add(complexType);
        }            
    }
    
    public void doJob(ComplexPortType port) {
        port.sendReceiveData(complexTypeSeq);
    }

    public ComplexPortType getPort() {        
        try{ 
            URL wsdl = null;
            if ((wsdlPath.startsWith("file://")) || (wsdlPath.startsWith("http://"))) {
                 wsdl = new URL(wsdlPath);
            } else {
                 wsdl = new URL("file://" + wsdlPath);
            }
            cs = new ComplexService(wsdl, SERVICE_NAME);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return cs.getSoapPort();        
    }

    public void printUsage() {
        System.out.println("Syntax is: Client [-WSDL wsdllocation] [-PacketSize packetnumber] ");
    }
}
