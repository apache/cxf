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

package org.apache.cxf.ws.rm;

import javax.xml.namespace.QName;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RMUtilsTest extends Assert {

    private IMocksControl control;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
    }

    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testGetName() {
        Endpoint e = control.createMock(Endpoint.class);
        EndpointInfo ei = control.createMock(EndpointInfo.class);
        EasyMock.expect(e.getEndpointInfo()).andReturn(ei).times(2);
        QName eqn = new QName("ns2", "endpoint");
        EasyMock.expect(ei.getName()).andReturn(eqn);
        ServiceInfo si = control.createMock(ServiceInfo.class);
        EasyMock.expect(ei.getService()).andReturn(si);
        QName sqn = new QName("ns1", "service");
        EasyMock.expect(si.getName()).andReturn(sqn);
        control.replay();
        assertEquals("{ns1}service.{ns2}endpoint", RMUtils.getEndpointIdentifier(e));
    } 
}
