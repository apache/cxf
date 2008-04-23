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

import org.apache.cxf.endpoint.Endpoint;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class AbstractEndpointTest extends Assert {

    private IMocksControl control;    
    private RMEndpoint rme;
    
    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        rme = control.createMock(RMEndpoint.class);
    }
    
    @After
    public void tearDown() {
        control.verify();
    }
    
    @Test
    public void testAccessors() {
        Endpoint ae = control.createMock(Endpoint.class);
        EasyMock.expect(rme.getApplicationEndpoint()).andReturn(ae);
        RMManager mgr = control.createMock(RMManager.class);
        EasyMock.expect(rme.getManager()).andReturn(mgr);
        control.replay();
        AbstractEndpoint tested = new AbstractEndpoint(rme);
        assertSame(rme, tested.getReliableEndpoint());
        assertSame(ae, tested.getEndpoint());
        assertSame(mgr, tested.getManager());
    }
    
    @Test
    public void testGenerateSequenceIdentifier() {
        RMManager mgr = control.createMock(RMManager.class); 
        EasyMock.expect(rme.getManager()).andReturn(mgr);
        SequenceIdentifierGenerator generator = control.createMock(SequenceIdentifierGenerator.class);
        EasyMock.expect(mgr.getIdGenerator()).andReturn(generator);
        Identifier id = control.createMock(Identifier.class);
        EasyMock.expect(generator.generateSequenceIdentifier()).andReturn(id);
        control.replay();
        AbstractEndpoint tested = new AbstractEndpoint(rme);
        assertSame(id, tested.generateSequenceIdentifier());
        control.verify();
    }
}