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
package org.apache.cxf.systest.ws.mtom;

import java.io.InputStream;
import java.net.URL;
import java.time.Instant;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.xml.ws.Service;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItStreamingMtomPortType;

import org.junit.Before;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests for CXF-9129: MTOM attachment streaming with WS-Security.
 *
 * The root cause is that WSS4JOutInterceptor defaults to expandXopInclude=true and
 * storeBytesInAttachment=true when MTOM is enabled. With these defaults WSS4J reads every
 * attachment byte during the POST_PROTOCOL security phase and processes attachments via
 * SwA-style references, which prevents streaming DataHandlers from working correctly
 * (the attachment stream is consumed before AttachmentOutEndingInterceptor can write it).
 *
 * The fix is to set expandXopInclude=false and storeBytesInAttachment=false on
 * WSS4JOutInterceptor. WS-Security then signs only the SOAP body (which contains the
 * xop:Include reference), leaving the raw attachment bytes to flow through
 * AttachmentOutEndingInterceptor at PRE_STREAM_ENDING without being consumed early.
 */
public class MTOMStreamingSecurityTest extends AbstractBusClientServerTestBase {

    public static final String PORT = allocatePort(MTOMServer.class);

    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");

    @Before
    public void resetStreamingTracker() {
        MTOMStreamingImpl.resetStreamingFinished();
        MTOMLargeStreamingImpl.resetStreamingFinished();
    }

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
            "Server failed to launch",
            launchServer(MTOMServer.class, true)
        );
    }

    @org.junit.AfterClass
    public static void cleanup() throws Exception {
        stopAllServers();
    }

    /**
     * Baseline: without WS-Security, the MTOM endpoint returns all streamed bytes correctly.
     */
    @org.junit.Test
    public void testStreamingMTOMNoSecurity() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMStreamingSecurityTest.class.getResource("streaming-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = MTOMStreamingSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItStreamingMtomPort");
        DoubleItStreamingMtomPortType port =
            service.getPort(portQName, DoubleItStreamingMtomPortType.class);
        updateAddressPort(port, PORT);

        DataHandler response = port.doubleIt5(25);

        int received = drain(response);
        assertEquals("Should receive all streamed bytes",
            MTOMStreamingImpl.CHUNK_COUNT * MTOMStreamingImpl.CHUNK_SIZE_BYTES, received);

        Instant serverFinished = MTOMStreamingImpl.getStreamingFinished();
        assertNotNull("Streaming should have completed", serverFinished);

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }

    /**
     * Tests CXF-9129 fix: with expandXopInclude=false and storeBytesInAttachment=false, the
     * signed MTOM endpoint returns all streamed bytes correctly without errors.
     */
    @org.junit.Test
    public void testStreamingMTOMWithExpandXopFix() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMStreamingSecurityTest.class.getResource("streaming-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = MTOMStreamingSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItStreamingMtomSignedPort");
        DoubleItStreamingMtomPortType port =
            service.getPort(portQName, DoubleItStreamingMtomPortType.class);
        updateAddressPort(port, PORT);

        DataHandler response = port.doubleIt5(25);

        int received = drain(response);
        assertEquals("Should receive all streamed bytes with WS-Security (CXF-9129)",
            MTOMStreamingImpl.CHUNK_COUNT * MTOMStreamingImpl.CHUNK_SIZE_BYTES, received);

        Instant serverFinished = MTOMStreamingImpl.getStreamingFinished();
        assertNotNull("Streaming should have completed", serverFinished);

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }

    /**
     * Reproduces the original CXF-9129 bug report scenario without WS-Security:
     * a large MTOM attachment (64 KB &gt; Jetty's default 32 KB output buffer) forces
     * Jetty to auto-flush HTTP chunks while the server is still writing.
     * The CXF client receives the SOAP body early, creates a lazy DataHandler, and
     * returns to the caller before the server finishes producing the remaining chunks.
     */
    @org.junit.Test
    public void testStreamingMTOMTimingReproducer() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMStreamingSecurityTest.class.getResource("streaming-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = MTOMStreamingSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItStreamingMtomLargePort");
        DoubleItStreamingMtomPortType port =
            service.getPort(portQName, DoubleItStreamingMtomPortType.class);
        updateAddressPort(port, PORT);

        DataHandler response = port.doubleIt5(25);
        Instant callReturnedInstant = Instant.now();

        int received = drain(response);
        assertEquals("Should receive all streamed bytes",
            MTOMLargeStreamingImpl.CHUNK_COUNT * MTOMLargeStreamingImpl.CHUNK_SIZE_BYTES, received);

        Instant serverFinished = MTOMLargeStreamingImpl.getStreamingFinished();
        assertNotNull("Streaming should have completed", serverFinished);
        assertTrue("JAX-WS call returned before server finished streaming",
            callReturnedInstant.isBefore(serverFinished));

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }

    /**
     * The full CXF-9129 reproducer: large MTOM attachment (64 KB) + WS-Security signing
     * with expandXopInclude=false and storeBytesInAttachment=false.
     *
     * Jetty auto-flushes the first ~32 KB (SOAP body + partial attachment) to the client
     * while the server is still writing. Because WSS4J signs only the SOAP body (xop:Include
     * reference) and never touches the raw attachment bytes, the PipedInputStream is not
     * consumed early. The client therefore obtains a lazy DataHandler and the JAX-WS call
     * returns before the server finishes streaming — exactly as in the original reproducer.
     */
    @org.junit.Test
    public void testStreamingMTOMTimingReproducerWithSecurity() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        URL busFile = MTOMStreamingSecurityTest.class.getResource("streaming-client.xml");

        Bus bus = bf.createBus(busFile.toString());
        BusFactory.setDefaultBus(bus);
        BusFactory.setThreadDefaultBus(bus);

        URL wsdl = MTOMStreamingSecurityTest.class.getResource("DoubleItMtom.wsdl");
        Service service = Service.create(wsdl, SERVICE_QNAME);
        QName portQName = new QName(NAMESPACE, "DoubleItStreamingMtomLargeSignedPort");
        DoubleItStreamingMtomPortType port =
            service.getPort(portQName, DoubleItStreamingMtomPortType.class);
        updateAddressPort(port, PORT);

        DataHandler response = port.doubleIt5(25);
        Instant callReturnedInstant = Instant.now();

        int received = drain(response);
        assertEquals("Should receive all streamed bytes with WS-Security and large payload",
            MTOMLargeStreamingImpl.CHUNK_COUNT * MTOMLargeStreamingImpl.CHUNK_SIZE_BYTES, received);

        Instant serverFinished = MTOMLargeStreamingImpl.getStreamingFinished();
        assertNotNull("Streaming should have completed", serverFinished);
        assertTrue("JAX-WS call returned before server finished streaming ",
            callReturnedInstant.isBefore(serverFinished));

        ((java.io.Closeable) port).close();
        bus.shutdown(true);
    }

    private static int drain(DataHandler dh) throws Exception {
        int total = 0;
        byte[] buf = new byte[4096];
        try (InputStream is = dh.getInputStream()) {
            int n;
            while ((n = is.read(buf)) != -1) {
                total += n;
            }
        }
        return total;
    }
}
