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
package org.apache.cxf.common.jaxb;

import java.util.Collections;
import java.util.Map;

import org.apache.cxf.common.jaxb.JAXBContextCache.CachedContextAndSchemas;

import org.junit.Test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class JAXBContextCacheTest {

    @Test
    public void testExactLookupWithoutDefaultNamespaceIsCached() throws Exception {
        String pkg = org.apache.cxf.configuration.security.ObjectFactory.class.getPackage().getName();
        ClassLoader loader = getClass().getClassLoader();
        CachedContextAndSchemas first = JAXBContextCache.getCachedContextAndSchemas(pkg, null, loader);
        CachedContextAndSchemas second = JAXBContextCache.getCachedContextAndSchemas(pkg, null, loader);
        assertSame(first.getContext(), second.getContext());
    }

    @Test
    public void testContextPropertiesAreNotIgnored() throws Exception {
        String pkg = org.apache.cxf.configuration.security.ObjectFactory.class.getPackage().getName();
        ClassLoader loader = getClass().getClassLoader();
        Map<String, Object> props =
            Collections.singletonMap("org.glassfish.jaxb.defaultNamespaceRemap", "urn:probe");
        CachedContextAndSchemas plain = JAXBContextCache.getCachedContextAndSchemas(pkg, null, loader);
        CachedContextAndSchemas remapped = JAXBContextCache.getCachedContextAndSchemas(pkg, props, loader);
        assertNotSame(plain.getContext(), remapped.getContext());
        // and a context built with properties must not be handed to a caller without them
        CachedContextAndSchemas plainAgain = JAXBContextCache.getCachedContextAndSchemas(pkg, null, loader);
        assertNotSame(remapped.getContext(), plainAgain.getContext());
    }
}
