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

package org.apache.cxf.configuration.jsse;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContainingInAnyOrder;

public class TLSClientParametersTest {
    private String oldProtocols;
    private String oldIgnoreProtocols;
    
    @Before
    public void setUp() {
        oldProtocols = System.getProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS);
        oldIgnoreProtocols = System.getProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS);
    }
    
    @After
    public void tearDown() {
        if (oldProtocols != null) {
            System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, oldProtocols);
        } else {
            System.clearProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS);
        }

        if (oldIgnoreProtocols != null) {
            System.setProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS, oldIgnoreProtocols);
        } else {
            System.clearProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS);
        }
    }
    
    @Test
    public void testDefaultHttpsProtocols() {
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder("TLSv1", 
            "TLSv1.1", 
            "TLSv1.2", 
            "TLSv1.3"));
    }
    
    @Test
    public void testConfiguredHttpsProtocols() {
        System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, "SSLv3,");
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder("SSLv3"));
    }
    
    @Test
    public void testIgnoreConfiguredHttpsProtocols() {
        System.setProperty(TLSClientParameters.IGNORE_CONFIGURED_HTTPS_PROTOCOLS, "true");
        System.setProperty(TLSClientParameters.CONFIGURED_HTTPS_PROTOCOLS, "SSLv3,");
        assertThat(TLSClientParameters.getPreferredClientProtocols(), arrayContainingInAnyOrder("TLSv1", 
            "TLSv1.1", 
            "TLSv1.2", 
            "TLSv1.3"));
    }
}
