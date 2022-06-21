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

package org.apache.cxf.systest.jaxws.attachment;

import jakarta.activation.DataHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class ValidationWithAttachmentTest {

    static final String PORT = TestUtil.getNewPortNumber(ValidationWithAttachmentTest.class);
    static final String ADDRESS = "http://localhost:" + PORT + "/" + ValidationWithAttachmentTest.class.getSimpleName();

    static Server server;
    static AttachmentService client;

    @BeforeClass
    public static void setUp() {
        initServer();
        initClient();
    }

    @AfterClass
    public static void tearDown() {
        server.stop();
    }

    @Test
    public void test() throws Exception {
        Request request = new Request();
        request.setContent(new DataHandler("test", "text/plain"));

        int bytes = client.test(request);
        Assert.assertTrue("Attachment data were not received", bytes > 0);
    }

    private static void initServer() {
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(AttachmentServiceImpl.class);
        factory.setAddress(ADDRESS);
        factory.setServiceBean(new AttachmentServiceImpl());
        server = factory.create();
    }

    private static void initClient() {
        JaxWsProxyFactoryBean clientFactory = new JaxWsProxyFactoryBean();
        clientFactory.setServiceClass(AttachmentService.class);
        clientFactory.setAddress(ADDRESS);
        client = (AttachmentService) clientFactory.create();

        //enable MTOM in client
        BindingProvider bp = (BindingProvider) client;
        SOAPBinding binding = (SOAPBinding) bp.getBinding();
        binding.setMTOMEnabled(true);
    }
}
