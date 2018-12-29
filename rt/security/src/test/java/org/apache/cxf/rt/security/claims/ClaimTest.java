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
package org.apache.cxf.rt.security.claims;

import java.net.URI;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

public class ClaimTest {

    @Test
    public void testCloneNull() {
        try {
            new Claim(null);
            fail("IllegalArgumentException was expected");
        } catch (IllegalArgumentException ex) {
            // expected
        }
    }

    @Test
    public void testCloneAllEquals() {
        Claim claim = new Claim();
        claim.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        claim.setOptional(true);
        claim.addValue("value1");
        claim.addValue("value2");
        claim.addValue("value3");
        Claim clone = claim.clone();
        assertEquals(claim, clone);
        assertEquals(claim, new Claim(clone)); // Clone from clone by using clone constructor
    }

    @Test
    public void testCloneTypeOnlySetEquals() {
        Claim claim = new Claim();
        claim.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        Claim clone = claim.clone();
        assertEquals(claim, clone);
        assertEquals(claim, new Claim(clone)); // Clone from clone by using clone constructor
    }

    @Test
    public void testCloneValuesOnlySetEquals() {
        Claim claim = new Claim();
        claim.addValue("value1");
        claim.addValue("value2");
        Claim clone = claim.clone();
        assertEquals(claim, clone);
        assertEquals(claim, new Claim(clone)); // Clone from clone by using clone constructor
    }

    @Test
    public void testCloneUnset() {
        Claim claim = new Claim();
        Claim clone = claim.clone();
        assertEquals(claim, clone);
        assertEquals(claim, new Claim(clone)); // Clone from clone by using clone constructor
    }

    @Test
    public void testCloneAndModifyValuesNotEquals() {
        Claim claim = new Claim();
        claim.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        claim.setOptional(true);
        claim.addValue("value1");
        claim.addValue("value2");
        claim.addValue("value3");
        Claim clone = claim.clone();

        claim.getValues().clear();
        claim.addValue("value4");
        assertNotEquals(claim, clone);
        assertEquals(1, claim.getValues().size());
        assertEquals(3, clone.getValues().size());
        assertEquals(claim.getClaimType(), clone.getClaimType());
        assertEquals(claim.isOptional(), clone.isOptional());
        assertNotEquals(claim.getValues(), clone.getValues());
    }

    @Test
    public void testCloneAndModifyTypeNotEquals() {
        Claim claim = new Claim();
        claim.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        claim.setOptional(true);
        claim.addValue("value1");
        claim.addValue("value2");
        claim.addValue("value3");
        Claim clone = claim.clone();
        clone.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/value"));
        assertNotEquals(claim, clone);
        assertNotEquals(claim.getClaimType(), clone.getClaimType());
        assertEquals(claim.isOptional(), clone.isOptional());
        assertEquals(claim.getValues(), clone.getValues());
    }

}