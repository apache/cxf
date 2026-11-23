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

package org.apache.cxf.rs.security.cors;

import java.util.Collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CrossOriginResourceSharingFilterTest {

    @Test
    public void testWarningForAllOriginsWithCredentials() {
        CrossOriginResourceSharingFilter filter = new CrossOriginResourceSharingFilter();
        filter.setAllowCredentials(true);
        // empty allowOrigins (the default) means all origins are allowed
        assertTrue(filter.warnIfAllOriginsWithCredentials(null));
        // only logged once
        assertFalse(filter.warnIfAllOriginsWithCredentials(null));
    }

    @Test
    public void testNoWarningWithoutCredentials() {
        CrossOriginResourceSharingFilter filter = new CrossOriginResourceSharingFilter();
        assertFalse(filter.warnIfAllOriginsWithCredentials(null));
    }

    @Test
    public void testNoWarningWithConfiguredOrigins() {
        CrossOriginResourceSharingFilter filter = new CrossOriginResourceSharingFilter();
        filter.setAllowCredentials(true);
        filter.setAllowOrigins(Collections.singletonList("https://example.com"));
        assertFalse(filter.warnIfAllOriginsWithCredentials(null));
    }

    @Test
    public void testWarningForAnnotationAllOriginsWithCredentials() {
        CrossOriginResourceSharingFilter filter = new CrossOriginResourceSharingFilter();
        assertTrue(filter.warnIfAllOriginsWithCredentials(annotation(AllOriginsWithCredentials.class)));
    }

    @Test
    public void testNoWarningForAnnotationDefaults() {
        CrossOriginResourceSharingFilter filter = new CrossOriginResourceSharingFilter();
        // allowAllOrigins defaults to false on the annotation, even when the filter's list is empty
        assertFalse(filter.warnIfAllOriginsWithCredentials(annotation(CredentialsOnly.class)));
    }

    private static CrossOriginResourceSharing annotation(Class<?> cls) {
        return cls.getAnnotation(CrossOriginResourceSharing.class);
    }

    @CrossOriginResourceSharing(allowAllOrigins = true, allowCredentials = true)
    private static final class AllOriginsWithCredentials {
    }

    @CrossOriginResourceSharing(allowCredentials = true)
    private static final class CredentialsOnly {
    }
}
