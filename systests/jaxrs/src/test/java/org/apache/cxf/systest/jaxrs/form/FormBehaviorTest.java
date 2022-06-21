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

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.Bus;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.testutil.common.AbstractClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractServerTestServerBase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FormBehaviorTest extends AbstractClientServerTestBase {
    public static final String PORT = allocatePort(FormBehaviorTest.class);

    public static class Server extends AbstractServerTestServerBase {
        @Override
        protected org.apache.cxf.endpoint.Server createServer(Bus bus) throws Exception {
            final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
            sf.setResourceClasses(FormResource.class);
            sf.setProvider(new FormReaderInterceptor());
            sf.setAddress("http://localhost:" + PORT + "/");
            return sf.create();
        }

        public static void main(String[] args) throws Exception {
            new Server().start();
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        AbstractResourceInfo.clearAllMaps();
        // keep out of process due to stack traces testing failures
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
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
