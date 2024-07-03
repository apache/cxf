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
import java.util.List;

import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(Parameterized.class)
public class JexlClaimsMapperTest {

    JexlClaimsMapper jcm;

    public JexlClaimsMapperTest(String scriptPath) throws IOException {
        jcm = new JexlClaimsMapper();
        jcm.setScript(scriptPath);
    }

    @Parameters
    public static Collection<Object[]> data() {
        Object[][] data = new Object[][] {
            {
                "jexlClaimMappingsWithoutFunctions.script"
            }, {
                "jexlClaimMappingsWithFunctions.script"
            }
        };
        return Arrays.asList(data);
    }

    @Test
    public void testClaimMerge() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertTrue(result.size() >= 2);
        assertEquals("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/name", result.get(1).getClaimType());
        assertEquals(1, result.get(1).getValues().size());
        assertEquals("Jan Bernhardt", result.get(1).getValues().get(0));

        for (ProcessedClaim c : result) {
            if ("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname".equals(c.getClaimType())) {
                fail("Only merged claim should be in result set, but not the individual claims");
            }
        }
    }

    @Test
    public void testRoleMappings() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertTrue(result.size() >= 1);
        assertEquals(2, result.get(0).getValues().size());
        assertTrue(result.get(0).getValues().contains("manager"));
        assertTrue(result.get(0).getValues().contains("administrator"));
    }

    @Test
    public void testUnusedClaims() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        for (ProcessedClaim c : result) {
            String claimType = c.getClaimType();
            if ("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/unused".equals(claimType)) {
                fail("Claims not handled within the script should not be copied to the target token");
            }
        }
    }

    @Test
    public void testUpdateIssuer() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        assertEquals("STS-B", result.get(0).getOriginalIssuer());
        assertEquals("NewIssuer", result.get(0).getIssuer());
        assertEquals("STS-A", result.get(1).getOriginalIssuer());
    }

    @Test
    public void testStaticClaim() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim staticClaim = findClaim(result,
                                               "http://schemas.microsoft.com/identity/claims/identityprovider");
        assertNotNull(staticClaim);
    }

    @Test
    public void testUpperCaseClaim() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/uppercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(2, claim.getValues().size());
        assertEquals("VALUE2", claim.getValues().get(1));
    }

    @Test
    public void testLowerCaseClaim() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/lowercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(2, claim.getValues().size());
        assertEquals("value2", claim.getValues().get(1));
    }

    @Test
    public void testWrappedUpperCaseClaim() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/wrappedUppercase");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(1, claim.getValues().size());
        assertEquals("PREFIX_VALUE_SUFFIX", claim.getValues().get(0));
    }

    @Test
    public void testSimpleClaimCopy() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mail");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(1, claim.getValues().size());
        assertEquals("test@apache.com", claim.getValues().get(0));
    }

    @Test
    public void testSingleToMultiValue() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/single2multi");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(3, claim.getValues().size());
        assertEquals("Value2", claim.getValues().get(1));
    }

    @Test
    public void testMultiToSingleValue() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/multi2single");
        assertNotNull(claim);
        assertNotNull(claim.getValues());
        assertEquals(1, claim.getValues().size());
        assertEquals("Value1,Value2,Value3", claim.getValues().get(0));
    }

    @Test
    public void testValueFilter() throws IOException {
        ProcessedClaimCollection result = jcm.mapClaims("A", createClaimCollection(), "B", createProperties());

        assertNotNull(result);
        ProcessedClaim claim = findClaim(result, "http://my.schema.org/identity/claims/filter");
        assertEquals(2, claim.getValues().size());
        assertTrue(claim.getValues().contains("match"));
        assertTrue(claim.getValues().contains("second_match"));
    }

    @SuppressWarnings("unchecked")
    protected ProcessedClaimCollection createClaimCollection() {
        ProcessedClaimCollection cc = new ProcessedClaimCollection();
        ProcessedClaim c = new ProcessedClaim();
        c.setIssuer("STS-A");
        c.setOriginalIssuer("STS-B");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role"));
        c.setValues((List<Object>)(List<?>)Arrays.asList("admin", "manager", "tester"));
        cc.add(c);

        c = new ProcessedClaim();
        c.setIssuer("STS-A");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname"));
        c.setValues((List<Object>)(List<?>)Arrays.asList("Jan"));
        cc.add(c);

        c = new ProcessedClaim();
        c.setIssuer("STS-A");
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname"));
        c.setValues((List<Object>)(List<?>)Arrays.asList("Bernhardt"));
        cc.add(c);

        c = new ProcessedClaim();
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/unused"));
        c.setValues((List<Object>)(List<?>)Arrays.asList("noValue"));
        cc.add(c);

        c = new ProcessedClaim();
        c.setClaimType(URI.create("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mail"));
        c.setValues((List<Object>)(List<?>)Arrays.asList("test@apache.com"));
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

    private ProcessedClaim findClaim(ProcessedClaimCollection claims, String claimType) {
        if (claimType == null || claims == null) {
            return null;
        }
        for (ProcessedClaim c : claims) {
            if (c.getClaimType() != null && claimType.equals(c.getClaimType())) {
                return c;
            }
        }
        return null;
    }

}
