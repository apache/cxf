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

package org.apache.cxf.systest.ldap.jaxrs;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.ws.rs.InternalServerErrorException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.zapodot.junit.ldap.EmbeddedLdapRule;
import org.zapodot.junit.ldap.EmbeddedLdapRuleBuilder;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Add a test for JAX-RS search using the LdapQueryVisitor.
 */
public class JAXRSLDAPUserTest {
    public static final String PORT = UserLDAPServer.PORT;
    public static final String PORT2 = UserLDAPServer.PORT2;

    @ClassRule
    public static EmbeddedLdapRule embeddedLdapRule = EmbeddedLdapRuleBuilder
        .newInstance()
        .bindingToAddress("localhost")
        .usingBindCredentials("ldap_su")
        .usingBindDSN("UID=admin,DC=example,DC=com")
        .usingDomainDsn("dc=example,dc=com")
        .importingLdifs("ldap.ldif")
        .build();

    private static boolean portUpdated;

    @BeforeClass
    public static void startServers() throws Exception {

        // Launch servers
        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(UserLDAPServer.class, true)
        );
    }

    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // Read in ldap.xml and substitute in the correct port
            Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/ldap-jaxrs.xml");
            String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            content = content.replaceAll("portno", Integer.toString(embeddedLdapRule.embeddedServerPort()));

            Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/ldap-jaxrsport.xml");
            Files.write(path2, content.getBytes());

            portUpdated = true;
        }

    }

    @AfterClass
    public static void cleanup() throws Exception {
        AbstractClientServerTestBase.stopAllServers();
    }

    @Test
    public void testSearchUser() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT);

        User user = wc.path("users/search/name==alice").get(User.class);
        Assert.assertEquals("alice", user.getName());
        Assert.assertEquals("smith", user.getSurname());
    }

    // Check that we can't inject an unknown parameter into the search query
    @Test(expected = InternalServerErrorException.class)
    public void testUnknownParameter() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT);

        wc.path("users/search/name==alice%3Bage==40").get(User.class);
    }

    // Check that wildcards are not supported by default
    @Test(expected = InternalServerErrorException.class)
    public void testSearchUserWildcard() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT);

        wc.path("users/search/name==a*").get(User.class);
    }

    // Here we configure the LDAPQueryVisitor not to encode the query values
    @Test
    public void testSearchUserWildcardAllowed() throws Exception {
        WebClient wc = WebClient.create("http://localhost:" + PORT2);

        User user = wc.path("users/search/name==a*").get(User.class);
        Assert.assertEquals("alice", user.getName());
        Assert.assertEquals("smith", user.getSurname());
    }
}
