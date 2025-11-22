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
package org.apache.cxf.systest.http.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.NotAuthorizedException;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractTestServerBase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.auth.DigestAuthSupplier;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.eclipse.jetty.ee11.servlet.ServletHolder;
import org.eclipse.jetty.ee11.servlet.security.ConstraintMapping;
import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.HashLoginService;
import org.eclipse.jetty.security.UserStore;
import org.eclipse.jetty.security.authentication.DigestAuthenticator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.security.Credential;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class DigestAuthSupplierJettyTest extends AbstractClientServerTestBase {

    private static final String USER = "alice";
    private static final String PWD = "ecila";

    @BeforeClass
    public static void startServer() throws Exception {
        launchServer(DigestAuthSupplierJettyServer.class);
    }

    @Test
    public void test() {
        WebClient client = WebClient.create("http://localhost:" + DigestAuthSupplierJettyServer.PORT, (String) null);

        assertThrows(NotAuthorizedException.class, () -> client.get(String.class));

        HTTPConduit conduit = WebClient.getConfig(client).getHttpConduit();
        conduit.setAuthSupplier(new DigestAuthSupplier());
        conduit.getAuthorization().setUserName(USER);
        conduit.getAuthorization().setPassword(PWD);

        assertEquals(TestServlet.RESPONSE, client.get(String.class));
    }

    public static class DigestAuthSupplierJettyServer extends AbstractTestServerBase {
        private static final int PORT = allocatePortAsInt(DigestAuthSupplierJettyServer.class);

        private static Server server;

        @Override
        protected void run() {
            server = new Server(PORT);

            HashLoginService loginService = new HashLoginService();
            loginService.setName("My Realm");
            UserStore userStore = new UserStore();
            String[] roles = new String[] {"user"};
            userStore.addUser(USER, Credential.getCredential(PWD), roles);
            loginService.setUserStore(userStore);

            Constraint.Builder constraint = new Constraint.Builder();
            constraint.name("DIGEST_AUTH");
            constraint.roles(roles);
            
            
            ConstraintMapping cm = new ConstraintMapping();
            cm.setConstraint(constraint.build());
            cm.setPathSpec("/*");

            ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
            csh.setAuthenticator(new DigestAuthenticator());
            csh.addConstraintMapping(cm);
            csh.setLoginService(loginService);

            ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
            context.setSecurityHandler(csh);
            context.setContextPath("/");
            server.setHandler(context);
            context.addServlet(new ServletHolder(new TestServlet()), "/*");

            try {
                server.start();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void tearDown() throws Exception {
            if (server != null) {
                server.stop();
                server.destroy();
                server = null;
            }
        }
    }

    @SuppressWarnings("serial")
    static class TestServlet extends HttpServlet {

        static final String RESPONSE = "Hi!";

        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
            resp.getWriter().print(RESPONSE);
        }

    }

}
