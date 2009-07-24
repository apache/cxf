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
package org.apache.cxf.jca.outbound;

import javax.security.auth.Subject;
import javax.xml.namespace.QName;

import org.apache.hello_world_soap_http.Greeter;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Unit test for ManagedConnectionImpl
 */
public class ManagedConnectionImplTest {

    /**
     * Verify the connection handle's equals() method
     */
    @Test
    public void testHandleEqualsMethod() throws Exception {
        IMocksControl control = EasyMock.createNiceControl();

        ManagedConnectionFactoryImpl mcf = control.createMock(ManagedConnectionFactoryImpl.class);

        CXFConnectionSpec cxRequestInfo = new CXFConnectionSpec();

        cxRequestInfo.setWsdlURL(getClass().getResource("/wsdl/hello_world.wsdl"));
        cxRequestInfo.setServiceClass(Greeter.class);
        cxRequestInfo.setEndpointName(new QName("http://apache.org/hello_world_soap_http", "SoapPort"));
        cxRequestInfo.setServiceName(new QName("http://apache.org/hello_world_soap_http", "SOAPService"));

        control.replay();

        Subject subject = new Subject();

        ManagedConnectionImpl conn = new ManagedConnectionImpl(mcf, cxRequestInfo, subject);

        Object handle1 = conn.getConnection(subject, cxRequestInfo);
        Object handle2 = conn.getConnection(subject, cxRequestInfo);

        assertEquals(handle1, handle1);
        assertEquals(handle2, handle2);
        assertFalse(handle1.equals(handle2));

    }

}
