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

package org.apache.cxf.bus.extension;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class ExtensionFragmentParserTest extends Assert {

    @Test
    public void testGetExtensions() {
        InputStream is = ExtensionFragmentParserTest.class.getResourceAsStream("extension1.xml");
        List<Extension> extensions = new ExtensionFragmentParser().getExtensions(is);
        assertEquals("Unexpected number of Extension elements.", 3, extensions.size());
        
        Extension e = extensions.get(0);
        assertTrue("Extension is deferred.", !e.isDeferred());
        assertEquals("Unexpected class name.", 
                     "org.apache.cxf.foo.FooImpl", e.getClassname());
        Collection<String> namespaces = e.getNamespaces();
        for (String ns : namespaces) {
            assertTrue("Unexpected namespace.", "http://cxf.apache.org/a/b/c".equals(ns)
                                                || "http://cxf.apache.org/d/e/f".equals(ns));
        }
        assertEquals("Unexpected number of namespace elements.", 2, namespaces.size());
        
        e = extensions.get(1);
        assertTrue("Extension is not deferred.", e.isDeferred());
        assertEquals("Unexpected implementation class name.", 
                     "java.lang.Boolean", e.getClassname());
        namespaces = e.getNamespaces();
        for (String ns : namespaces) {
            assertEquals("Unexpected namespace.", "http://cxf.apache.org/x/y/z", ns);            
        }
        assertEquals("Unexpected number of namespace elements.", 1, namespaces.size());
    }
}
