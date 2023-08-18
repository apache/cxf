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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HandlerTestBase {
    protected Bus mockBus = mock(Bus.class);
    protected CXFManagedConnection mockManagedConnection =
                mock(CXFManagedConnection.class);

    protected CXFInvocationHandler mockHandler =
                mock(CXFInvocationHandler.class);

    protected ManagedConnectionFactoryImpl mcf =
                mock(ManagedConnectionFactoryImpl.class);
    protected ManagedConnectionImpl mci =
                mock(ManagedConnectionImpl.class);
    protected Method testMethod;
    protected TestTarget target = new TestTarget();

    public HandlerTestBase() {
    }


    @Before
    public void setUp() {
        when(mcf.getBus()).thenReturn(mockBus);
        when(mci.getManagedConnectionFactory()).thenReturn(mcf);

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