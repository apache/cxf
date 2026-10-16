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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.Download;
import org.apache.cxf.DownloadNextResponseType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * CXF-8542: if the InputStream of an MTOM attachment's DataHandler throws an IOException while
 * the server is writing the response, the client must not receive the truncated data as if it
 * were complete.
 */
public class MtomAttachmentStreamFailureTest extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(MtomAttachmentStreamFailureTest.class);
    private static final Logger LOG = LogUtils.getLogger(MtomAttachmentStreamFailureTest.class);

    /** Size of the attachment when no failure is simulated */
    private static final int FULL_SIZE = 4 * 1024 * 1024;

    public static class Server extends AbstractBusTestServerBase {
        protected void run() {
            String address = "http://localhost:" + PORT + "/SoapContext/SoapPort";
            Endpoint endpoint = Endpoint.publish(address, new FailingDownloadImpl());
            ((SOAPBinding)endpoint.getBinding()).setMTOMEnabled(true);
        }

        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                LOG.info("done!");
            }
        }
    }

    /**
     * Returns an attachment of FULL_SIZE bytes, or, if simulate is true, an attachment whose
     * InputStream throws an IOException after minNumberOfChunks KB have been read.
     */
    static final class FailingDownloadImpl implements Download {
        @Override
        public DownloadNextResponseType downloadNext(Integer minNumberOfChunks, Boolean simulate) {
            final long size = Boolean.TRUE.equals(simulate) ? minNumberOfChunks * 1024L : FULL_SIZE;
            final boolean fail = Boolean.TRUE.equals(simulate);
            DownloadNextResponseType response = new DownloadNextResponseType();
            response.setDataContent(new DataHandler(new DataSource() {
                @Override
                public InputStream getInputStream() {
                    return new GeneratingInputStream(size, fail);
                }

                @Override
                public OutputStream getOutputStream() {
                    return null;
                }

                @Override
                public String getContentType() {
                    return "application/octet-stream";
                }

                @Override
                public String getName() {
                    return "data";
                }
            }));
            return response;
        }
    }

    /**
     * Generates size bytes, then either reports the end of the stream or throws an IOException.
     */
    static final class GeneratingInputStream extends InputStream {
        private final long size;
        private final boolean fail;
        private long count;

        GeneratingInputStream(long size, boolean fail) {
            this.size = size;
            this.fail = fail;
        }

        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            return read(b, 0, 1) == -1 ? -1 : b[0];
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (count >= size) {
                if (fail) {
                    throw new IOException("simulated I/O error after " + count + " bytes");
                }
                return -1;
            }
            int n = (int)Math.min(len, size - count);
            for (int i = off; i < off + n; i++) {
                b[i] = (byte)'x';
            }
            count += n;
            return n;
        }
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }

    @Test
    public void testFullDownload() throws Exception {
        Download client = createClient(false);
        DownloadNextResponseType response = client.downloadNext(0, false);
        assertEquals(FULL_SIZE, readAll(response.getDataContent()));
    }

    @Test
    public void testFailureAfterResponseCommitted() throws Exception {
        // 1 MB is well beyond the servlet response buffer, so the response is committed
        assertFailureSignaled(createClient(false), 1024);
    }

    @Test
    public void testFailureBeforeResponseCommitted() throws Exception {
        // 1 KB still fits in the servlet response buffer
        assertFailureSignaled(createClient(false), 1);
    }

    @Test
    public void testFailureAfterResponseCommittedURLConnection() throws Exception {
        assertFailureSignaled(createClient(true), 1024);
    }

    @Test
    public void testFailureBeforeResponseCommittedURLConnection() throws Exception {
        assertFailureSignaled(createClient(true), 1);
    }

    private static void assertFailureSignaled(Download client, int failAfterKb) throws Exception {
        final DownloadNextResponseType response;
        try {
            response = client.downloadNext(failAfterKb, true);
        } catch (WebServiceException ex) {
            // the response was aborted before the attachment could be read
            LOG.fine("Failure signaled by the invocation: " + ex);
            return;
        }
        final long read;
        try {
            read = readAll(response.getDataContent());
        } catch (IOException ex) {
            // the response was aborted while the attachment was being read
            LOG.fine("Failure signaled while reading the attachment: " + ex);
            return;
        }
        fail("The server failed after " + failAfterKb * 1024L + " bytes, but the client read "
            + read + " bytes without any error");
    }

    private Download createClient(boolean urlConnection) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(Download.class);
        factory.setAddress("http://localhost:" + PORT + "/SoapContext/SoapPort/DownloadPort");
        Download client = (Download)factory.create();
        BindingProvider bp = (BindingProvider)client;
        ((SOAPBinding)bp.getBinding()).setMTOMEnabled(true);
        if (urlConnection) {
            bp.getRequestContext().put("force.urlconnection.http.conduit", Boolean.TRUE);
        }
        return client;
    }

    private static long readAll(DataHandler dh) throws IOException {
        long total = 0;
        byte[] buf = new byte[8192];
        try (InputStream in = dh.getInputStream()) {
            for (int n = in.read(buf); n != -1; n = in.read(buf)) {
                total += n;
            }
        }
        return total;
    }
}
