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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.xml.ws.Binding;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.soap.SOAPFaultException;
import org.apache.cxf.Download;
import org.apache.cxf.DownloadFault_Exception;
import org.apache.cxf.DownloadNextResponseType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class AttachmentChunkingTest extends AbstractBusClientServerTestBase {
    private static final String PORT = allocatePort(DownloadServer.class);
    private static final Logger LOG = LogUtils.getLogger(AttachmentChunkingTest.class);

    private static final class DownloadImpl implements Download {
        @Override
        public DownloadNextResponseType downloadNext(Boolean simulate) {
            final DownloadNextResponseType responseType = new DownloadNextResponseType();
            responseType.setDataContent(new DataHandler(new DataSource() {
                @Override
                public InputStream getInputStream() {
                    if (simulate) {
                        return simulate();
                    } else {
                        return generate(100000);
                    }
                }

                @Override
                public OutputStream getOutputStream() {
                    return null;
                }

                @Override
                public String getContentType() {
                    return "";
                }

                @Override
                public String getName() {
                    return "";
                }
            }));

            return responseType;
        }
    }

    public static class DownloadServer extends AbstractBusTestServerBase {
        protected void run() {
            Object implementor = new DownloadImpl();
            String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
            Endpoint.publish(address, implementor);
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
    public void testChunkingPartialFailure() {
        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Download.class);

        final Download client = (Download) factory.create();
        final BindingProvider bindingProvider = (BindingProvider) client;
        final Binding binding = bindingProvider.getBinding();

        final String address = String.format("http://localhost:%s/SoapContext/SoapPort/DownloadPort", PORT);
        bindingProvider.getRequestContext().put("jakarta.xml.ws.service.endpoint.address", address);
        ((SOAPBinding) binding).setMTOMEnabled(true);

        // See please https://issues.apache.org/jira/browse/CXF-9057
        SOAPFaultException ex = assertThrows(SOAPFaultException.class, () -> client.downloadNext(true));
        assertThat(ex.getMessage(), containsString("simulated error during stream processing"));
    }
    
    @Test
    public void testChunking() throws IOException, DownloadFault_Exception {
        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Download.class);

        final Download client = (Download) factory.create();
        final BindingProvider bindingProvider = (BindingProvider) client;
        final Binding binding = bindingProvider.getBinding();

        final String address = String.format("http://localhost:%s/SoapContext/SoapPort/DownloadPort", PORT);
        bindingProvider.getRequestContext().put("jakarta.xml.ws.service.endpoint.address", address);
        ((SOAPBinding) binding).setMTOMEnabled(true);

        final DownloadNextResponseType response = client.downloadNext(false);
        assertThat(response.getDataContent().getInputStream().readAllBytes().length, equalTo(100000));
    }
    
    private static InputStream generate(int size) {
        final byte[] buf = new byte[size];
        Arrays.fill(buf, (byte) 'x');
        return new ByteArrayInputStream(buf);
    }
    
    private static InputStream simulate() {
        return new InputStream() {
            @Override
            public int read() {
                return (byte) 'x';
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (ThreadLocalRandom.current().nextBoolean()) {
                    throw new IllegalArgumentException("simulated error during stream processing");
                }

                for (int i = off; i < off + len; i++) {
                    b[i] = (byte) 'x';
                }

                return len;
            }
        };
    }
}
