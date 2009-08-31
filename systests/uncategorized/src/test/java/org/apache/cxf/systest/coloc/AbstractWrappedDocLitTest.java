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

package org.apache.cxf.systest.coloc;

import javax.xml.namespace.QName;

import static junit.framework.Assert.assertEquals;

import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.GreeterImpl;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;

/**
 * This class invokes the service described in /wsdl/greeter_control.wsdl.
 * This WSDL contains operations with in-out parameters.
 * It sets up the a client in "getPort()" to send requests to the
 * server which is listening on port 9001 (SOAP/HTTP).
 * The subclass defines where CXF configuration and the
 * target server (transport, etc).
 *
 */
public abstract class AbstractWrappedDocLitTest extends AbstractColocTest {
    static final QName SERVICE_NAME = new QName("http://apache.org/hello_world_soap_http",
                                                "SOAPService");
    static final QName PORT_NAME = new QName("http://apache.org/hello_world_soap_http",
                                             "SoapPort");
    static final String WSDL_LOCATION = "/wsdl/hello_world.wsdl";

    private Greeter port;
    private GreeterImpl impl = new GreeterImpl();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        port = getPort(
                         getServiceQname(),
                         getPortQName(),
                         getWsdlLocation(),
                         Greeter.class);
    }

    @Test
    public void testTwoWayOperation() {
        for (int idx = 0; idx < 2; idx++) {
            verifySayHi(port);
            verifyGreetMe(port);
        }
    }
    
    @Test
    public void testOneWayOperation() {
        for (int idx = 0; idx < 2; idx++) {
            verifyGreetMeOneway(port);
        }
    }

    @Test
    public void testFault() {
        for (int idx = 0; idx < 2; idx++) {
            verifyTestDocLitFault(port);
        }
    }

    protected void verifyTestDocLitFault(Greeter proxy) {
        try {
            proxy.testDocLitFault(BadRecordLitFault.class.getSimpleName());
            fail("Should throw a BadRecordLitFault Exception");
        } catch (BadRecordLitFault brlf) {
            assertEquals(BadRecordLitFault.class.getSimpleName(), brlf.getFaultInfo());
        } catch (NoSuchCodeLitFault nsclf) {
            fail("Should not throw a NoSuchCodeLitFault Exception");
        }
    }

    protected void verifyGreetMeOneway(Greeter proxy) {
        int count = impl.getInvocationCount();
        proxy.greetMeOneWay("oneWay");
        assertTrue("Count Should not be same", count != impl.getInvocationCount());
    }
    
    protected void verifySayHi(Greeter greeterPort) {
        String resp = greeterPort.sayHi();
        assertEquals("Bonjour", resp);
    }

    protected void verifyGreetMe(Greeter greeterPort) {
        String resp = greeterPort.greetMe("BART");
        assertEquals("Hello BART", resp);
    }

    protected Object getServiceImpl() {
        return impl;
    }

    protected String getWsdlLocation() {
        return WSDL_LOCATION;
    }

    protected QName getServiceQname() {
        return SERVICE_NAME;
    }

    protected QName getPortQName() {
        return PORT_NAME;
    }

    protected boolean isFaultCodeCheckEnabled() {
        return false;
    }
}
