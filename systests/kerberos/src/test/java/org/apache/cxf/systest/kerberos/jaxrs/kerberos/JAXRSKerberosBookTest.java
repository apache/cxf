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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.security.KerberosAuthOutInterceptor;
import org.apache.cxf.systest.kerberos.common.SecurityTestUtil;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.transport.http.auth.HttpAuthHeader;
import org.apache.cxf.transport.http.auth.SpnegoAuthSupplier;
import org.apache.directory.server.annotations.CreateKdcServer;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ApplyLdifFiles;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.integ.AbstractLdapTestUnit;
import org.apache.directory.server.core.integ.FrameworkRunner;
import org.apache.directory.server.core.kerberos.KeyDerivationInterceptor;
import org.ietf.jgss.GSSName;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * A set of tests for Kerberos Tokens that use an Apache DS instance as the KDC.
 */

@RunWith(FrameworkRunner.class)

//Define the DirectoryService
@CreateDS(name = "AbstractKerberosTest-class",
    enableAccessControl = false,
    allowAnonAccess = false,
    enableChangeLog = true,
    partitions = {
        @CreatePartition(
            name = "example",
            suffix = "dc=example,dc=com",
            indexes = {
                @CreateIndex(attribute = "objectClass"),
                @CreateIndex(attribute = "dc"),
                @CreateIndex(attribute = "ou")
            }
        ) },
    additionalInterceptors = {
        KeyDerivationInterceptor.class
        }
)

@CreateLdapServer(
    transports = {
        @CreateTransport(protocol = "LDAP")
        }
)

@CreateKdcServer(
    transports = {
        @CreateTransport(protocol = "KRB", address = "127.0.0.1")
        },
    primaryRealm = "service.ws.apache.org",
    kdcPrincipal = "krbtgt/service.ws.apache.org@service.ws.apache.org"
)

//Inject an file containing entries
@ApplyLdifFiles("kerberos.ldif")

public class JAXRSKerberosBookTest extends AbstractLdapTestUnit {
    public static final String PORT = BookKerberosServer.PORT;

    private static final String KERBEROS_CONFIG_FILE =
        "org/apache/cxf/systest/kerberos/jaxrs/kerberos/kerberosClient.xml";
    
    private static boolean runTests;
    private static boolean portUpdated;
    
    @Before
    public void updatePort() throws Exception {
        if (!portUpdated) {
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // Read in krb5.conf and substitute in the correct port
            Path path = FileSystems.getDefault().getPath(basedir, "/src/test/resources/krb5.conf");
            String content = new String(Files.readAllBytes(path), "UTF-8");
            content = content.replaceAll("port", "" + super.getKdcServer().getTransports()[0].getPort());
            
            Path path2 = FileSystems.getDefault().getPath(basedir, "/target/test-classes/jaxrs.krb5.conf");
            Files.write(path2, content.getBytes());
            
            System.setProperty("java.security.krb5.conf", path2.toString());
            
            portUpdated = true;
        }
    }
    
    @BeforeClass
    public static void startServers() throws Exception {

        //
        // This test fails with the IBM JDK
        //
        if (!"IBM Corporation".equals(System.getProperty("java.vendor"))) {
            runTests = true;
            String basedir = System.getProperty("basedir");
            if (basedir == null) {
                basedir = new File(".").getCanonicalPath();
            }

            // System.setProperty("sun.security.krb5.debug", "true");
            System.setProperty("java.security.auth.login.config", 
                               basedir + "/src/test/resources/kerberos.jaas");
            
        }
        
        // Launch servers
        org.junit.Assert.assertTrue(
            "Server failed to launch",
            // run the server in the same process
            // set this to false to fork
            AbstractBusClientServerTestBase.launchServer(BookKerberosServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        SecurityTestUtil.cleanup();
        AbstractBusClientServerTestBase.stopAllServers();
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
