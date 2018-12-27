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
package org.apache.cxf.systest.jaxrs.form;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FormBehaviorTest extends AbstractBusClientServerTestBase {
    public static final String PORT = allocatePort(FormBehaviorTest.class);

    @Ignore
    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(FormResource.class);
            sf.setProvider(new FormReaderInterceptor());
            sf.setAddress("http://localhost:" + PORT + "/");
            sf.create();
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
        createStaticBus();
    }

    @Test
    public void testInterceptorInvokedOnFormAndFormParamMatchesFormValue() throws Exception {
        Client client = ClientBuilder.newClient();
        String uri = "http://localhost:" + PORT + "/form";
        Form f = new Form("value", "ORIGINAL");
        Response r = client.target(uri)
                           .request(MediaType.APPLICATION_FORM_URLENCODED)
                           .post(Entity.form(f));

        Assert.assertEquals("MODIFIED", r.getHeaderString("FromForm"));
        Assert.assertEquals("MODIFIED", r.getHeaderString("FromFormParam"));
    }
}
