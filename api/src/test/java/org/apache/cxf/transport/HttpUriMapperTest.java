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

package org.apache.cxf.transport;

import java.net.URL;

import org.junit.Assert;
import org.junit.Test;

public class HttpUriMapperTest extends Assert {

    @Test
    public void testGetContext() throws Exception {
        URL url = new URL("http://localhost:8080/SoapContext/SoapPort");
        String path = url.getPath();
        assertEquals("/SoapContext", HttpUriMapper.getContextName(path));
        
        url = new URL("http://localhost:8080/SoapContext/SoapPort/");
        path = url.getPath();
        assertEquals("/SoapContext/SoapPort", HttpUriMapper.getContextName(path));
        
        url = new URL("http://localhost:8080/");
        path = url.getPath();
        assertEquals("", HttpUriMapper.getContextName(path));
    }
    
    @Test
    public void testGetResourceBase() throws Exception {
        URL url = new URL("http://localhost:8080/SoapContext/SoapPort");
        String path = url.getPath();
        assertEquals("/SoapPort", HttpUriMapper.getResourceBase(path));
        url = new URL("http://localhost:8080/SoapContext/SoapPort/");
        path = url.getPath();
        assertEquals("/", HttpUriMapper.getResourceBase(path));
        url = new URL("http://localhost:8080/SoapPort");
        path = url.getPath();
        assertEquals("/SoapPort", HttpUriMapper.getResourceBase(path));
    }
}
