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

package org.apache.cxf.systest.jaxws;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Endpoint;
import javax.xml.ws.WebServiceException;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Download;
import org.apache.cxf.DownloadFault_Exception;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ext.logging.LoggingFeature;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AttachmentMtomChunkingTest extends AbstractAttachmentChunkingTest {
    private static final String PORT = allocatePort(DownloadServer.class);
    private static final Logger LOG = LogUtils.getLogger(AttachmentMtomChunkingTest.class);

    public static class DownloadServer extends AbstractBusTestServerBase {
        protected void run() {
            Object implementor = new DownloadImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
            final Endpoint endpoint = Endpoint.publish(address, implementor, new LoggingFeature());
            ((SOAPBinding)endpoint.getBinding()).setMTOMEnabled(true);
        }

        public static void main(String[] args) {
            try {
                DownloadServer s = new DownloadServer();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                LOG.info("done!");
            }
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(DownloadServer.class, true));
    }
    
    @Test
    public void testChunkingPartialFailure() throws IOException, DownloadFault_Exception {
        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setFeatures(Arrays.asList(new LoggingFeature()));
        factory.setServiceClass(Download.class);

        final Download client = (Download) factory.create();
        final BindingProvider bindingProvider = (BindingProvider) client;
        final Binding binding = bindingProvider.getBinding();

        final String address = String.format("http://localhost:%s/SoapContext/SoapPort/DownloadPort", getPort());
        bindingProvider.getRequestContext().put("jakarta.xml.ws.service.endpoint.address", address);
        ((SOAPBinding) binding).setMTOMEnabled(true);

        // See please https://issues.apache.org/jira/browse/CXF-9057
        assertThrows(WebServiceException.class, () -> client.downloadNext(true));
    }

    @Override
    protected String getPort() {
        return PORT;
    }
}
