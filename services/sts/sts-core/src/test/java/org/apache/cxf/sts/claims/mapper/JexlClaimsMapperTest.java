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

package org.apache.cxf.sts.claims.mapper;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;

import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.Claim;
import org.apache.cxf.sts.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class JexlClaimsMapperTest extends org.junit.Assert {

    JexlClaimsMapper jcm;

    public JexlClaimsMapperTest(String scriptPath) throws IOException {
        jcm = new JexlClaimsMapper();
        jcm.setScript(scriptPath);
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
            {
                "src/test/resources/jexlClaimMappingsWithoutFunctions.script"
            }, {
                "src/test/resources/jexlClaimMappingsWithFunctions.script"
            }
        };
        return Arrays.asList(data);
    }

    @Test
    public void testClaimMerge() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertEquals("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name", result.get(1).getClaimType()
            .toString());
        assertEquals(1, result.get(1).getValues().size());
        assertEquals("Jan Bernhardt", result.get(1).getValues().get(0));

        for (Claim c : result) {
            if ("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname".equals(c.getClaimType())) {
                fail("Only merged claim should be in result set, but not the individual claims");
            }
        }
    }

    @Test
    public void testRoleMappings() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertTrue(result.size() >= 1);
        assertEquals(2, result.get(0).getValues().size());
        assertTrue(result.get(0).getValues().contains("manager"));
        assertTrue(result.get(0).getValues().contains("administrator"));
    }

    @Test
    public void testUnusedClaims() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        for (Claim c : result) {
            URI claimType = c.getClaimType();
            if (claimType != null
                && "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/unused".equals(claimType.toString())) {
                fail("Claims not handled within the script should not be copied to the target token");
            }
        }
    }

    @Test
    public void testUpdateIssuer() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertEquals("STS-B", result.get(0).getOriginalIssuer());
        assertEquals("NewIssuer", result.get(0).getIssuer());
        assertEquals("STS-A", result.get(1).getOriginalIssuer());
    }

    @Test
    public void testStaticClaim() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        Claim staticClaim = findClaim(result, 
                                               "http://schemas.microsoft.com/identity/claims/identityprovider");
        assertNotNull(staticClaim);
    }

    @Test
    public void testUpperCaseClaim() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        Claim claim = findClaim(result, "http://my.schema.org/identity/claims/uppercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(2, claim.getValues().size());
        assertEquals("VALUE2", claim.getValues().get(1));
    }

    @Test
    public void testLowerCaseClaim() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        Claim claim = findClaim(result, "http://my.schema.org/identity/claims/lowercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(2, claim.getValues().size());
        assertEquals("value2", claim.getValues().get(1));
    }

    @Test
    public void testWrappedUpperCaseClaim() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        Claim claim = findClaim(result, "http://my.schema.org/identity/claims/wrappedUppercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(1, claim.getValues().size());
        assertEquals("PREFIX_VALUE_SUFFIX", claim.getValues().get(0));
    }

    @Test
    public void testSimpleClaimCopy() throws IOException {
        ClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        Claim claim = findClaim(result, "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mail");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(1, claim.getValues().size());
        assertEquals("test@apache.com", claim.getValues().get(0));
    }

    @SuppressWarnings("unchecked")
    protected ClaimCollection createClaimCollection() {
        ClaimCollection cc = new ClaimCollection();
        Claim c = new Claim();
        c.setIssuer("STS-A");
        c.setOriginalIssuer("STS-B");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        c.setValues(Arrays.asList("admin", "manager", "tester"));
        cc.add(c);

        c = new Claim();
        c.setIssuer("STS-A");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"));
        c.setValues(Arrays.asList("Jan"));
        cc.add(c);

        c = new Claim();
        c.setIssuer("STS-A");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        c.setValues(Arrays.asList("Bernhardt"));
        cc.add(c);

        c = new Claim();
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/unused"));
        c.setValues(Arrays.asList("noValue"));
        cc.add(c);

        c = new Claim();
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mail"));
        c.setValues(Arrays.asList("test@apache.com"));
        cc.add(c);

        return cc;
    }

    protected ClaimsParameters createProperties() {
        StaticSTSProperties stsProp = new StaticSTSProperties();
        stsProp.setIssuer("NewIssuer");
        ClaimsParameters param = new ClaimsParameters();
        param.setStsProperties(stsProp);
        return param;
    }

    private Claim findClaim(ClaimCollection claims, String claimType) {
        if (claimType == null || claims == null) {
            return null;
        }
        for (Claim c : claims) {
            if (c.getClaimType() != null && claimType.equals(c.getClaimType().toString())) {
                return c;
            }
        }
        return null;
    }

}
