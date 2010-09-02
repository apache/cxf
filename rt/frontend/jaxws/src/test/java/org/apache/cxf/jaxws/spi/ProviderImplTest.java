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

package org.apache.cxf.jaxws.spi;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.BusFactory;
import org.junit.After;


public class ProviderImplTest extends org.junit.Assert {
    @org.junit.Test
    public void testCreateW3CEpr() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org", "ServiceName");
        QName portName = new QName("http://cxf.apache.org", "PortName");
        ProviderImpl impl = new ProviderImpl();
        W3CEndpointReference w3Epr = impl.createW3CEndpointReference("http://myaddress", serviceName,
                                                                      portName, null, "wsdlLoc",
                                                                      null);
        
        java.io.StringWriter sw = new java.io.StringWriter();
        StreamResult result = new StreamResult(sw);
        w3Epr.writeTo(result);
        String expected = "<wsdl:definitions"; 
        assertTrue("Embeded wsdl element is not generated", sw.toString().indexOf(expected) > -1);
        assertTrue("wsdlLocation attribute has the wrong value", 
                   sw.toString().contains("wsdli:wsdlLocation=\"http://cxf.apache.org wsdlLoc\""));
    }

    @After
    public void tearDown() {
        BusFactory.setDefaultBus(null);
    }
    
    

}
