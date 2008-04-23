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

import java.net.MalformedURLException;
import java.net.URL;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEventListener;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;



import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.hello_world_soap_http.Greeter;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;


public abstract class ManagedConnectionTestBase extends Assert {
    protected Subject subj;

    protected CXFConnectionRequestInfo cri;

    protected CXFConnectionRequestInfo cri2;

    protected ManagedConnectionImpl mci;

    protected ManagedConnectionFactoryImpl factory = EasyMock.createMock(ManagedConnectionFactoryImpl.class);

    protected Bus bus;
    
    protected ConnectionEventListener mockListener = EasyMock.createMock(ConnectionEventListener.class);
    
    public ManagedConnectionTestBase() {
        
    }
    
    @Before
    public void setUp() throws ResourceException, MalformedURLException, BusException {
               
        subj = new Subject();
        
        URL wsdl = getClass().getResource("/wsdl/hello_world.wsdl");
        
        QName serviceName = new QName("http://apache.org/hello_world_soap_http", "SOAPService");
        
        QName serviceName2 = new QName("http://apache.org/hello_world_soap_http", "SOAPService2");
        
        QName portName = new QName("http://apache.org/hello_world_soap_http", "SoapPort");
        
        QName portName2 = new QName("http://apache.org/hello_world_soap_http", "SoapPort2");

        cri = new CXFConnectionRequestInfo(Greeter.class, wsdl, serviceName, portName);

        cri2 = new CXFConnectionRequestInfo(Greeter.class, wsdl, serviceName2, portName2);
        
        BusFactory bf = BusFactory.newInstance();
        bus = bf.createBus();
        BusFactory.setDefaultBus(bus);
        
        
        EasyMock.reset(factory); 
        
        factory.getBus();
        
        EasyMock.expectLastCall().andReturn(bus).anyTimes();
        EasyMock.replay(factory);
                
        mci = new ManagedConnectionImpl(factory, cri, subj);        
              
        mci.addConnectionEventListener(mockListener);
    }
    
}
