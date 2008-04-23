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
package org.apache.cxf.jca.cxf;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ManagedConnectionMetaData;
import javax.resource.spi.ResourceAdapterInternalException;
import javax.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;


import org.apache.cxf.connector.Connection;
import org.apache.cxf.jca.cxf.handlers.ProxyInvocationHandler;
import org.apache.hello_world_soap_http.Greeter;
import org.easymock.classextension.EasyMock;
import org.junit.Ignore;
import org.junit.Test;

public class ManagedConnectionImplTest extends ManagedConnectionTestBase {

    protected URL wsdl;
    protected QName serviceName;
    protected QName portName;

    @Test
    public void testInstanceOfConnection() throws Exception {
        assertTrue("Instance of Connection", mci instanceof Connection);
        ((Connection)mci).close();
    }

    @Test
    public void testGetConnectionServiceGetPortThrows() throws Exception {
        cri = new CXFConnectionRequestInfo(Foo.class, null, null, null);
        cri.setAddress("http://localhost:9000/soap");
        Object o = mci.getConnection(subj, cri);
        assertTrue(o instanceof Foo);
    }

    
    @Ignore("Need to check the classloader")
    public void testThreadContextClassLoaderIsSet() throws Exception {
        //set the threadContextClassLoader for Bus 
        //TODO njiang classloader things
        //check the threadContextClassLoader 
        mci.getConnection(subj, cri);
    }
    
    @Test
    public void testGetConnectionWithNoWSDLInvokesCreateClientWithTwoParameters() throws Exception {
        cri = new CXFConnectionRequestInfo(Greeter.class, null, serviceName, portName);
        // need to get wsdl
        Object o = mci.getConnection(subj, cri);

        assertTrue("Checking implementation of Connection interface", o instanceof Connection);
        assertTrue("Checking implementation of passed interface", o instanceof Greeter);
    }
    

    @Test
    public void testGetConnectionWithNoWSDLInvokesCreateClientWithTwoArgs()
        throws Exception {
        cri = new CXFConnectionRequestInfo(Greeter.class, null, serviceName, null);
        Object o = mci.getConnection(subj, cri);
        assertTrue("Checking implementation of Connection interface", o instanceof Connection);
        assertTrue("Checking implementation of passed interface", o instanceof Greeter);
    }

    @Ignore
    public void testGetConnectionWithNoPortReturnsConnection() throws Exception {

        cri = new CXFConnectionRequestInfo(Greeter.class, 
                                           wsdl,
                                           serviceName,
                                           null);
        
        Object o = mci.getConnection(subj, cri);

        assertTrue("Returned connection does not implement Connection interface", o instanceof Connection);
        assertTrue("Returned connection does not implement Connection interface", o instanceof Greeter);
    }


    @Test
    public void testGetConnectionReturnsConnection() throws ResourceException {
        Object o = mci.getConnection(subj, cri);
        assertTrue("Returned connection does not implement Connection interface", o instanceof Connection);
        assertTrue("Returned connection does not implement Connection interface", o instanceof Greeter);
    }

    private void verifyProxyInterceptors(Object o) {
        assertTrue(o instanceof Proxy);
        assertEquals("First handler must be a ProxyInvocation Handler", ProxyInvocationHandler.class, 
                     Proxy.getInvocationHandler(o).getClass());
    }


    @Test
    public void testGetConnectionWithDudSubjectA() throws ResourceException {
        Object o = mci.getConnection(subj, cri);
        verifyProxyInterceptors(o);
    }


    @Test
    public void testGetConnectionWithDudSubjectB() throws ResourceException {
        String user = new String("user");
        char password[] = {'a', 'b', 'c'};
        PasswordCredential creds = new PasswordCredential(user, password);
        subj.getPrivateCredentials().add(creds);
        Object o = mci.getConnection(subj, cri);
        
        verifyProxyInterceptors(o);
    }


    @Test
    public void testGetConnectionWithSubject() throws ResourceException {
        String user = new String("user");
        char password[] = {'a', 'b', 'c'};
        PasswordCredential creds = new PasswordCredential(user, password);
        creds.setManagedConnectionFactory(factory);
        subj.getPrivateCredentials().add(creds);
        Object o = mci.getConnection(subj, cri);

        verifyProxyInterceptors(o);
    }
 

    @Test
    public void testCloseConnection() throws Exception {      
        Connection conn = (Connection)mci.getConnection(subj, cri);
        EasyMock.reset(mockListener);
        mockListener.connectionClosed(EasyMock.isA(ConnectionEvent.class));
        EasyMock.expectLastCall();
        EasyMock.replay(mockListener);
        conn.close();
    }


    @Test
    public void testAssociateConnection() throws Exception {
        
        CXFConnectionRequestInfo cri2 = new CXFConnectionRequestInfo(Greeter.class,
                                                                         new URL("file:/tmp/foo2"),
                                                                         new QName("service2"),
                                                                         new QName("fooPort2"));
        ManagedConnectionImpl mci2 = new ManagedConnectionImpl(factory, cri2, new Subject());
        mci2.addConnectionEventListener(mockListener);

        Object o = mci.getConnection(subj, cri);

        assertTrue("Returned connection does not implement Connection interface", o instanceof Connection);
        assertTrue("Returned connection does not implement Connection interface", o instanceof Greeter);
        assertTrue("Returned connection is not a java.lang.reflect.Proxy instance", o instanceof Proxy);

        InvocationHandler handler = Proxy.getInvocationHandler(o);

        assertTrue("Asserting handler class: " + handler.getClass(),
                   handler instanceof CXFInvocationHandler);

        Object assocMci = ((CXFInvocationHandler)handler).getData().getManagedConnection();

        assertTrue("Asserting associated ManagedConnection.", mci == assocMci);
        assertTrue("Asserting associated ManagedConnection.", mci2 != assocMci);

        mci2.associateConnection(o);

        assocMci = ((CXFInvocationHandler)handler).getData().getManagedConnection();

        assertTrue("Asserting associated ManagedConnection.", mci2 == assocMci);
        assertTrue("Asserting associated ManagedConnection.", mci != assocMci);

    }


    @Test
    public void testAssociateConnectionThrowsException() throws Throwable {

        InvocationHandler ih = EasyMock.createMock(InvocationHandler.class);
                
        Object dodgyHandle = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {Foo.class}, ih);

        try {
            mci.associateConnection(dodgyHandle);
            fail("Except exception on call with ClassCast Exception");
        } catch (ResourceAdapterInternalException raie) {
            assertTrue(true);
        }

    }


    @Test
    public void testGetMetaData() throws Exception {
        ManagedConnectionMetaData data = mci.getMetaData();
        assertEquals("Checking the EISProductionVersion", "1.1", data.getEISProductVersion());
        assertEquals("Checking the EISProductName", "WS-based-EIS", data.getEISProductName());
    }
  
}
