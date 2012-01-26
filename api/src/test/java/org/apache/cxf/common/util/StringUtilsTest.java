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

package org.apache.cxf.common.util;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTest extends Assert {
    
    @Test
    public void testDiff() throws Exception {
        String str1 = "http://local/SoapContext/SoapPort/greetMe/me/CXF";
        String str2 = "http://local/SoapContext/SoapPort";
        String str3 = "http://local/SoapContext/SoapPort/";
        assertEquals("/greetMe/me/CXF", StringUtils.diff(str1, str2));
        assertEquals("greetMe/me/CXF", StringUtils.diff(str1, str3));
        assertEquals("http://local/SoapContext/SoapPort/", StringUtils.diff(str3, str1));
    }
    
    @Test
    public void testGetFirstNotEmpty() throws Exception {        
        assertEquals("greetMe", StringUtils.getFirstNotEmpty("/greetMe/me/CXF", "/"));
        assertEquals("greetMe", StringUtils.getFirstNotEmpty("greetMe/me/CXF", "/"));
    }
    
    @Test
    public void testGetParts() throws Exception {
        String str = "/greetMe/me/CXF";
        List<String> parts = StringUtils.getParts(str, "/");
        assertEquals(3, parts.size());
        assertEquals("greetMe", parts.get(0));
        assertEquals("me", parts.get(1));
        assertEquals("CXF", parts.get(2));
    }
    
    @Test
    public void testGetFound() throws Exception {
        String regex = "velocity-\\d+\\.\\d+\\.jar";
        
        assertTrue(StringUtils.isEmpty(StringUtils.getFound("velocity-dep-1.4.jar", regex)));
        assertFalse(StringUtils.isEmpty(StringUtils.getFound("velocity-1.4.jar", regex)));
        assertTrue(StringUtils.isEmpty(StringUtils.getFound(null, regex)));
    }
    
    @Test
    public void testFormatVersionNumber() throws Exception {
        assertEquals("2.0", StringUtils.formatVersionNumber("2.0-M1-SNAPSHOT"));
        assertEquals("2.0.12", StringUtils.formatVersionNumber("2.0.12-M1-SNAPSHOT"));
    }
    
    @Test
    public void testAddPortIfMissing() throws Exception {
        assertEquals("http://localhost:80", StringUtils.addDefaultPortIfMissing("http://localhost"));
        assertEquals("http://localhost:80/", StringUtils.addDefaultPortIfMissing("http://localhost/"));
        assertEquals("http://localhost:80/abc", StringUtils.addDefaultPortIfMissing("http://localhost/abc"));
        assertEquals("http://localhost:80", StringUtils.addDefaultPortIfMissing("http://localhost:80"));
        
        assertEquals("http://localhost:9090", 
                     StringUtils.addDefaultPortIfMissing("http://localhost", "9090"));
    }
}
