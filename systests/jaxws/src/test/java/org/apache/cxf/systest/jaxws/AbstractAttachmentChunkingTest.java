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

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Download;
import org.apache.cxf.DownloadFault_Exception;
import org.apache.cxf.DownloadNextResponseType;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

abstract class AbstractAttachmentChunkingTest extends AbstractBusClientServerTestBase {
    protected static final class DownloadImpl implements Download {
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
    
    @Test
    public void testChunking() throws IOException, DownloadFault_Exception {
        final JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Download.class);

        final Download client = (Download) factory.create();
        final BindingProvider bindingProvider = (BindingProvider) client;
        final Binding binding = bindingProvider.getBinding();

        final String address = String.format("http://localhost:%s/SoapContext/SoapPort/DownloadPort", getPort());
        bindingProvider.getRequestContext().put("jakarta.xml.ws.service.endpoint.address", address);
        ((SOAPBinding) binding).setMTOMEnabled(true);

        final DownloadNextResponseType response = client.downloadNext(false);
        try (InputStream in = response.getDataContent().getInputStream()) {
            assertThat(IOUtils.readBytesFromStream(in).length, equalTo(100000));
        }
    }
    
    protected abstract String getPort();
    
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
