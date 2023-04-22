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

package org.apache.cxf.systest.kerberos.jaxrs.kerberos;

import java.io.File;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.security.KerberosAuthOutInterceptor;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.apache.kerby.kerberos.kerb.server.SimpleKdcServer;
import org.ietf.jgss.GSSName;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * A set of tests for Kerberos Tokens that use an Apache Kerby instance as the KDC.
 */
public class JAXRSKerberosBookTest extends AbstractBusClientServerTestBase {
    public static final String PORT = BookKerberosServer.PORT;

    private static final String KERBEROS_CONFIG_FILE =
        "org/apache/cxf/systest/kerberos/jaxrs/kerberos/kerberosClient.xml";

    private static boolean runTests;

    private static SimpleKdcServer kerbyServer;

    @BeforeClass
    public static void startServers() throws Exception {

        //
        // This test fails with the IBM JDK
        //
        if (!"IBM Corporation".equals(System.getProperty("java.vendor"))) {
            runTests = true;
        }

        String basedir = System.getProperty("basedir");
        if (basedir == null) {
            basedir = new File(".").getCanonicalPath();
        }

        // System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.auth.login.config", basedir + "/target/test-classes/kerberos.jaas");
        System.setProperty("java.security.krb5.conf", basedir + "/target/krb5.conf");

        kerbyServer = new SimpleKdcServer();

        kerbyServer.setKdcRealm("service.ws.apache.org");
        kerbyServer.setAllowUdp(false);
        kerbyServer.setWorkDir(new File(basedir + "/target"));

        //kerbyServer.setInnerKdcImpl(new NettyKdcServerImpl(kerbyServer.getKdcSetting()));

        kerbyServer.init();

        // Create principals
        String alice = "alice@service.ws.apache.org";
        String bob = "bob/service.ws.apache.org@service.ws.apache.org";

        kerbyServer.createPrincipal(alice, "alice");
        kerbyServer.createPrincipal(bob, "bob");

        kerbyServer.start();

        // Launch servers
        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractClientServerTestBase.launchServer(BookKerberosServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        AbstractClientServerTestBase.stopAllServers();
        if (kerbyServer != null) {
            kerbyServer.stop();
        }
    }

    @Test
    public void testGetBookWithConfigInHttpConduit() throws Exception {
        if (!runTests) {
            return;
        }

        doTestGetBook123Proxy(KERBEROS_CONFIG_FILE);
    }

    private void doTestGetBook123Proxy(String configFile) throws Exception {
        BookStore bs = JAXRSClientFactory.create("http://localhost:" + PORT, BookStore.class,
                configFile);
        WebClient.getConfig(bs).getOutInterceptors().add(new LoggingOutInterceptor());

        SpnegoAuthSupplier authSupplier = new SpnegoAuthSupplier();
        authSupplier.setServicePrincipalName("bob@service.ws.apache.org");
        authSupplier.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);
        WebClient.getConfig(bs).getHttpConduit().setAuthSupplier(authSupplier);

        // just to verify the interface call goes through CGLIB proxy too
        Assert.assertEquals("http://localhost:" + PORT, WebClient.client(bs).getBaseURI().toString());
        Book b = bs.getBook("123");
        Assert.assertEquals(b.getId(), 123);
        b = bs.getBook("123");
        Assert.assertEquals(b.getId(), 123);
    }

    @Test
    public void testGetBookWithInterceptor() throws Exception {
        if (!runTests) {
            return;
        }

        WebClient wc = WebClient.create("http://localhost:" + PORT + "/bookstore/books/123");

        KerberosAuthOutInterceptor kbInterceptor = new KerberosAuthOutInterceptor();

        AuthorizationPolicy policy = new AuthorizationPolicy();
        policy.setAuthorizationType(HttpAuthHeader.AUTH_TYPE_NEGOTIATE);
        policy.setAuthorization("alice");
        policy.setUserName("alice");
        policy.setPassword("alice");

        kbInterceptor.setPolicy(policy);
        kbInterceptor.setCredDelegation(true);

        WebClient.getConfig(wc).getOutInterceptors().add(new LoggingOutInterceptor());
        WebClient.getConfig(wc).getOutInterceptors().add(kbInterceptor);

        // Required so as to get it working with our KDC
        kbInterceptor.setServicePrincipalName("bob@service.ws.apache.org");
        kbInterceptor.setServiceNameType(GSSName.NT_HOSTBASED_SERVICE);

        Book b = wc.get(Book.class);
        Assert.assertEquals(b.getId(), 123);
    }

}
