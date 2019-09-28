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

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TextExtensionFragmentParserTest {

    @Test
    public void testGetExtensions() throws IOException {
        InputStream is = TextExtensionFragmentParserTest.class.getResourceAsStream("extension2.txt");
        List<Extension> extensions = new TextExtensionFragmentParser(null).getExtensions(is);
        assertEquals("Unexpected number of Extension elements.", 3, extensions.size());

        Extension e = extensions.get(0);
        assertFalse("Extension is deferred.", e.isDeferred());
        assertEquals("Unexpected class name.",
                     "org.apache.cxf.foo.FooImpl", e.getClassname());
        assertEquals("Unexpected number of namespace elements.", 0, e.getNamespaces().size());
        e = extensions.get(1);
        assertTrue("Extension is not deferred.", e.isDeferred());
        assertEquals("Unexpected implementation class name.",
                     "java.lang.Boolean", e.getClassname());
        assertNull("Interface should be null", e.getInterfaceName());
        assertEquals("Unexpected number of namespace elements.", 0, e.getNamespaces().size());
    }
}
