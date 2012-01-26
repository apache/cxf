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

import java.util.*;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PropertiesLoaderUtilsTest extends Assert {

    Properties properties;
    String soapBindingFactory = "org.apache.cxf.bindings.soap.SOAPBindingFactory";
    
    @Before
    public void setUp() throws Exception {
        properties = PropertiesLoaderUtils.
            loadAllProperties("org/apache/cxf/common/util/resources/bindings.properties.xml",
                              Thread.currentThread().getContextClassLoader());
        assertNotNull(properties);        
        
    }
    @Test
    public void testLoadBindings() throws Exception {

        assertEquals(soapBindingFactory,
                     properties.getProperty("http://schemas.xmlsoap.org/wsdl/soap/"));

        assertEquals(soapBindingFactory,
                     properties.getProperty("http://schemas.xmlsoap.org/wsdl/soap/http"));

        assertEquals(soapBindingFactory,
                     properties.getProperty("http://cxf.apache.org/transports/jms"));
        

    }

    @Test
    public void testGetPropertyNames() throws Exception {
        Collection<String> names = PropertiesLoaderUtils.getPropertyNames(properties, soapBindingFactory);
        assertNotNull(names);
        assertEquals(3, names.size());
        assertTrue(names.contains("http://schemas.xmlsoap.org/wsdl/soap/"));
        assertTrue(names.contains("http://schemas.xmlsoap.org/wsdl/soap/http"));
        assertTrue(names.contains("http://cxf.apache.org/transports/jms"));
    }
}
