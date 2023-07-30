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
import org.apache.cxf.ws.rm.v200702.Identifier;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 */
public class AbstractEndpointTest {

    private RMEndpoint rme;

    @Before
    public void setUp() {
        rme = mock(RMEndpoint.class);
    }

    @Test
    public void testAccessors() {
        Endpoint ae = mock(Endpoint.class);
        when(rme.getApplicationEndpoint()).thenReturn(ae);
        RMManager mgr = mock(RMManager.class);
        when(rme.getManager()).thenReturn(mgr);

        AbstractEndpoint tested = new AbstractEndpoint(rme);
        assertSame(rme, tested.getReliableEndpoint());
        assertSame(ae, tested.getEndpoint());
        assertSame(mgr, tested.getManager());
    }

    @Test
    public void testGenerateSequenceIdentifier() {
        RMManager mgr = mock(RMManager.class);
        when(rme.getManager()).thenReturn(mgr);
        SequenceIdentifierGenerator generator = mock(SequenceIdentifierGenerator.class);
        when(mgr.getIdGenerator()).thenReturn(generator);
        Identifier id = mock(Identifier.class);
        when(generator.generateSequenceIdentifier()).thenReturn(id);

        AbstractEndpoint tested = new AbstractEndpoint(rme);
        assertSame(id, tested.generateSequenceIdentifier());
    }
}