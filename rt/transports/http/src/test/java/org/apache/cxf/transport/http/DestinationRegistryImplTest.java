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

package org.apache.cxf.transport.http;

import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class DestinationRegistryImplTest extends Assert {
    private static final String ADDRESS = "http://bar/snafu";
    private static final QName QNAME = new QName(ADDRESS, "foobar");

    private static final String[] REGISTERED_PATHS = {"/soap", "/soap2", "/soappath", "/soap/test",
                                                      "/test/tst", "/test2/"};
    private static final String[] REQUEST_PATHS = {"/soap", "/soap/2", "/soap2", "/soap3", 
                                                   "/soap/test", "/soap/tst", "/soap/", "/test/tst/2", 
                                                   "/test/2", "/test2", "/test2/", "/test2/3"};
    private static final int[] MATCHED_PATH_INDEXES = {0, 0, 1, -1, 
                                                       3, 0, 0, 4, 
                                                       -1, 5, 5, 5};
    private IMocksControl control; 
    private DestinationRegistry registry;
    private MessageObserver observer;

    @Before
    public void setUp() {
        control = EasyMock.createNiceControl();
        registry = new DestinationRegistryImpl();
        observer = control.createMock(MessageObserver.class);
    }
    
    @After
    public void tearDown() {
        control = null;
        registry = null;
    }
    
    @Test
    public void testAddAndGetDestinations() throws Exception {
        setUpDestinations();

        Set<String> paths = registry.getDestinationsPaths();
        assertEquals(REGISTERED_PATHS.length, paths.size());
        
        for (int i = 0; i < REGISTERED_PATHS.length; i++) {
            assertTrue(paths.contains(REGISTERED_PATHS[i]));
            
            AbstractHTTPDestination path = registry.getDestinationForPath(REGISTERED_PATHS[i]);
            assertNotNull(path);
        }
    }
    
    @Test
    public void testCheckRestfulRequest() throws Exception {
        setUpDestinations();
        for (int i = 0; i < REQUEST_PATHS.length; i++) {
            final int mi = MATCHED_PATH_INDEXES[i];
            
            for (int j = 0; j < REGISTERED_PATHS.length; j++) {
                AbstractHTTPDestination target = registry.getDestinationForPath(REGISTERED_PATHS[j]);
                if (mi == j) {
                    EasyMock.expect(target.getMessageObserver()).andReturn(observer);
                    EndpointInfo endpoint = new EndpointInfo();
                    endpoint.setAddress(REGISTERED_PATHS[mi]);
                    endpoint.setName(QNAME);
                    EasyMock.expect(target.getEndpointInfo()).andReturn(endpoint);

                } else {
                    EasyMock.expect(target.getMessageObserver()).andReturn(observer).anyTimes();
                }
                
            }
            
            control.replay();
            
            AbstractHTTPDestination destination = registry.checkRestfulRequest(REQUEST_PATHS[i]);
            
            if (0 <= mi) {
                EndpointInfo endpoint = destination.getEndpointInfo();
                assertNotNull(endpoint);
                
                assertEquals(endpoint.getAddress(), REGISTERED_PATHS[mi]);
            } else {
                assertNull(destination);
            }
            
            control.verify();
            
            control.reset();
        }
        
    }

    private void setUpDestinations() {
        for (int i = 0; i < REGISTERED_PATHS.length; i++) {
            AbstractHTTPDestination destination = control.createMock(AbstractHTTPDestination.class);
            EndpointInfo endpoint = new EndpointInfo();
            endpoint.setAddress(REGISTERED_PATHS[i]);
            endpoint.setName(QNAME);
            EasyMock.expect(destination.getEndpointInfo()).andReturn(endpoint);

            control.replay();
            registry.addDestination(destination);
            control.reset();
        }
        
    }
}
