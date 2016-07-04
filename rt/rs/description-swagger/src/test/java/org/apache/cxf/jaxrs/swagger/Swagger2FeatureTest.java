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

package org.apache.cxf.jaxrs.swagger;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class Swagger2FeatureTest extends Assert {
    @Test
    public void testSetBasePathByAddress() {
        Swagger2Feature f = new Swagger2Feature();

        f.setBasePathByAddress("http://localhost:8080/foo");
        assertEquals("/foo", f.getBasePath());
        assertEquals("localhost:8080", f.getHost());
        unsetBasePath(f);

        f.setBasePathByAddress("http://localhost/foo");
        assertEquals("/foo", f.getBasePath());
        assertEquals("localhost", f.getHost());
        unsetBasePath(f);

        f.setBasePathByAddress("/foo");
        assertEquals("/foo", f.getBasePath());
        assertNull(f.getHost());
        unsetBasePath(f);
    }

    private static void unsetBasePath(Swagger2Feature f) {
        f.setBasePath(null);
        f.setHost(null);
    }
}
