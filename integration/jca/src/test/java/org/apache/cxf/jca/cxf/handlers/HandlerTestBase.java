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
package org.apache.cxf.jca.cxf.handlers;

import java.lang.reflect.Method;

import org.apache.cxf.Bus;
import org.apache.cxf.jca.cxf.CXFInvocationHandler;
import org.apache.cxf.jca.cxf.CXFManagedConnection;
import org.apache.cxf.jca.cxf.ManagedConnectionFactoryImpl;
import org.apache.cxf.jca.cxf.ManagedConnectionImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class HandlerTestBase extends Assert {
    protected Bus mockBus = EasyMock.createMock(Bus.class);
    protected CXFManagedConnection mockManagedConnection = 
                EasyMock.createMock(CXFManagedConnection.class);

    protected CXFInvocationHandler mockHandler = 
                EasyMock.createMock(CXFInvocationHandler.class);

    protected ManagedConnectionFactoryImpl mcf = 
                EasyMock.createMock(ManagedConnectionFactoryImpl.class);
    protected ManagedConnectionImpl mci =
                EasyMock.createMock(ManagedConnectionImpl.class);
    protected Method testMethod;
    protected TestTarget target = new TestTarget();
    
    public HandlerTestBase() {
    }


    @Before
    public void setUp() {
        EasyMock.reset(mcf);
        EasyMock.reset(mci);
    
        mcf.getBus();
        EasyMock.expectLastCall().andReturn(mockBus);
        EasyMock.replay(mcf);
        
        mci.getManagedConnectionFactory();
        EasyMock.expectLastCall().andReturn(mcf);
        EasyMock.replay(mci);
        try {
            testMethod = TestTarget.class.getMethod("testMethod", new Class[0]);
        } catch (NoSuchMethodException ex) {
            fail(ex.toString());
        }
        
    }

    @Test
    public void testNullTestTarget() {
       // do nothing here ,just for avoid the junit test warning
    }
    
}
