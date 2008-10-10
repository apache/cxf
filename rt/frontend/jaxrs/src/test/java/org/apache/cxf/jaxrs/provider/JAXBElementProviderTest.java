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

package org.apache.cxf.jaxrs.provider;

import java.util.ArrayList;
import java.util.List;

import javax.xml.validation.Schema;

import org.junit.Assert;
import org.junit.Test;


public class JAXBElementProviderTest extends Assert {

    @Test
    public void testSetSchemasFromClasspath() {
        JAXBElementProvider provider = new JAXBElementProvider();
        List<String> locations = new ArrayList<String>();
        locations.add("classpath:/test.xsd");
        provider.setSchemas(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from classpath", s);
    }
    
    @Test
    public void testSetSchemasFromDisk() {
        JAXBElementProvider provider = new JAXBElementProvider();
        List<String> locations = new ArrayList<String>();
        String loc = getClass().getClassLoader().getResource("test.xsd").getFile();
        locations.add(loc);
        provider.setSchemas(locations);
        Schema s = provider.getSchema();
        assertNotNull("schema can not be read from disk", s);
    }
    
}
