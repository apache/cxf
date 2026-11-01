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
package org.apache.cxf.systest.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.namespace.QName;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.xml.soap.AttachmentPart;
import jakarta.xml.soap.MessageFactory;
import jakarta.xml.soap.SOAPMessage;
import jakarta.xml.ws.BindingType;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.SOAPBinding;
import org.apache.cxf.jaxws.DispatchImpl;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Reproducer for CXF-8926: MTOM client hangs indefinitely when sending a large
 * attachment to a server that accepts the TCP connection but never reads from it.
 *
 * Root cause: AttachmentOutEndingInterceptor calls AttachmentSerializer.writeAttachments(),
 * which calls DataHandler.writeTo() writing into HttpClientPipedOutputStream. Once the TCP
 * connection is established (connectionComplete=true), writes bypass the connection-timeout
 * guard and call PipedOutputStream.write() directly. When the server never reads, the TCP
 * receive/send buffers fill up, HttpClient stops consuming from the PipedInputStream, and
 * PipedOutputStream.write() blocks in awaitSpace() indefinitely. The configured
 * receiveTimeout only applies to the HttpRequest response timeout and has no effect here.
 *
 * A raw ServerSocket that accepts but never reads is a reliable way to saturate the
 * OS-level TCP buffers and trigger the stall.
 *
 * Expected behaviour after the fix: the call must unblock and throw an IOException well
 * within the configured receiveTimeout rather than hanging forever.
 */
public class MTOMAttachmentStallTest {
    private static final String PORT = TestUtil.getNewPortNumber(MTOMAttachmentStallTest.class);
    private static final QName SERVICE_NAME =
        new QName("http://cxf.apache.org/test/mtom-stall", "MtomStallService");
    private static final QName PORT_NAME =
        new QName("http://cxf.apache.org/test/mtom-stall", "MtomStallPort");

    /**
     * Attachment size – must exceed the combined TCP send + receive buffer ceiling
     * so that the stall server's receive window drops to zero and the HttpClient
     * publisher stops draining the pipe, blocking PipedOutputStream.write().
     * On macOS net.inet.tcp.autorcvbufmax = 4 MB per direction, so 10 MB reliably
     * saturates the ~8 MB combined buffer on all platforms.
     */
    private static final int ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024;

    /**
     * Client-side receive timeout (ms). The fix uses this as the TimedBlockingPipe
     * write deadline. Must be significantly shorter than TEST_TIMEOUT_SECONDS so
     * the fix resolves the stall well before the outer guard fires.
     */
    private static final long RECEIVE_TIMEOUT_MS = 5000L;

    /**
     * Outer test bound. When the bug is present the call hangs indefinitely;
     * future.get() times out here and the test fails with the thread dump.
     * Set to 3x RECEIVE_TIMEOUT_MS as headroom.
     */
    private static final long TEST_TIMEOUT_SECONDS = 20L;

    /**
     * Attachment size for the slow-server test. Must exceed combined OS TCP send +
     * receive buffers (~8 MB on macOS/Linux) so TCP back-pressure propagates back
     * into the client pipe before the server resumes reading.
     */
    private static final int SLOW_ATTACHMENT_SIZE_BYTES = 10 * 1024 * 1024;

    /**
     * How long the slow server pauses before resuming reads. Must exceed the JDK
     * HttpClient cached-pool idle reap (~60 s) so that without the fix the pipe-
     * reader thread is reaped and the client fails with "Read end dead". The call
     * must succeed with the fix because TimedBlockingPipe survives the stall.
     */
    private static final long SLOW_STALL_MS = 75_000L;

    @Test
    public void testMtomClientDoesNotHangWhenNetworkStalls() throws Exception {
        // Raw ServerSocket: accept the TCP connection but never read from it.
        // This fills the OS-level TCP receive buffer, then the client send buffer,
        // and finally the pipe – reproducing the hang on the client.
        ServerSocket stallServer = new ServerSocket();
        stallServer.bind(new InetSocketAddress(Integer.parseInt(PORT)));
        try (stallServer) {
            Thread acceptor = new Thread(() -> {
                try (Socket accepted = stallServer.accept()) {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception ignored) {
                }
            });
            acceptor.setDaemon(true);
            acceptor.start();

            Dispatch<SOAPMessage> dispatch =
                createMtomDispatch("http://localhost:" + PORT + "/stall");
            SOAPMessage request = buildLargeMtomRequest();

            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<?> future = exec.submit(() -> {
                try {
                    dispatch.invoke(request);
                } catch (Exception expected) {
                    // Any exception is acceptable – the test only verifies liveness.
                }
            });
            try {
                future.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                String dump = captureHungThreadDump();
                future.cancel(true);
                fail("CXF-8926: MTOM client hung for >" + TEST_TIMEOUT_SECONDS
                    + "s while sending a large attachment to a stalled server."
                    + " PipedOutputStream.write() blocked with no way for the"
                    + " configured receiveTimeout to break the pipe deadlock."
                    + dump);
            } finally {
                exec.shutdownNow();
                ((DispatchImpl<?>) dispatch).close();
            }
        }
    }

    /**
     * New reproducer for CXF-8926: server accepts the connection and reads the
     * MTOM attachment, but pauses for SLOW_STALL_MS before draining it.
     *
     * Without the fix: the JDK HttpClient cached-pool idle-reaps the thread that
     * drains CXF's PipedInputStream after ~60 s of inactivity; the pipe read-end
     * dies and the client fails with "Read end dead" / "Could not write attachments"
     * even though the server never closed the connection and eventually resumes.
     *
     * With the fix (TimedBlockingPipe): the pipe has no thread-identity check, so
     * the JDK reap is invisible to it; the write blocks quietly until the server
     * resumes draining, at which point the upload completes successfully.
     *
     * This test uses receiveTimeout=0 (infinite) so the failure mode is the raw
     * "Read end dead" exception, not a plain timeout.
     */
    @Test
    public void testMtomClientSucceedsWhenServerResumesAfterStall() throws Exception {
        String slowPort = TestUtil.getNewPortNumber(MTOMAttachmentStallTest.class);
        String address = "http://localhost:" + slowPort + "/mtom-slow";

        Endpoint ep = Endpoint.publish(address, new SlowMtomProvider(SLOW_STALL_MS));
        try {
            Dispatch<SOAPMessage> dispatch = createMtomDispatch(address);

            // Override to infinite receive timeout: exposes "Read end dead" (bug)
            // rather than a plain 5 s timeout which would mask the root cause.
            HTTPConduit conduit =
                (HTTPConduit) ((DispatchImpl<?>) dispatch).getClient().getConduit();
            HTTPClientPolicy policy = new HTTPClientPolicy();
            policy.setConnectionTimeout(10_000L);
            policy.setReceiveTimeout(0L);
            conduit.setClient(policy);

            SOAPMessage request = buildSlowMtomRequest();

            ExecutorService exec = Executors.newSingleThreadExecutor();
            Future<?> future = exec.submit(() -> {
                try {
                    dispatch.invoke(request);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            // Generous bound: stall + upload completion + headroom.
            long testTimeoutSeconds = SLOW_STALL_MS / 1000 + 60;
            try {
                future.get(testTimeoutSeconds, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                future.cancel(true);
                Throwable cause = e.getCause();
                fail("CXF-8926 (slow-server): MTOM client threw an exception instead of"
                    + " recovering after the server resumed reading. "
                    + "Expected: call succeeds once back-pressure releases. "
                    + "Actual cause: " + cause);
            } catch (TimeoutException e) {
                future.cancel(true);
                fail("CXF-8926 (slow-server): MTOM client hung for >"
                    + testTimeoutSeconds + "s." + captureHungThreadDump());
            } finally {
                exec.shutdownNow();
                ((DispatchImpl<?>) dispatch).close();
            }
        } finally {
            ep.stop();
        }
    }

    /**
     * Scans all live threads for the MTOM pipe-deadlock frame and returns a formatted
     * stack trace. When the bug is present, AttachmentSerializer.writeAttachments() is
     * blocked inside PipedInputStream.receive() / awaitSpace(), matching the CXF-8926
     * Jira report. Also dumps all WAITING/BLOCKED non-daemon threads as a fallback.
     */
    private static String captureHungThreadDump() {
        Map<Thread, StackTraceElement[]> all = Thread.getAllStackTraces();
        StringBuilder primary = new StringBuilder();
        StringBuilder fallback = new StringBuilder();

        for (Map.Entry<Thread, StackTraceElement[]> entry : all.entrySet()) {
            Thread t = entry.getKey();
            StackTraceElement[] stack = entry.getValue();
            boolean relevant = false;
            for (StackTraceElement frame : stack) {
                String cn = frame.getClassName();
                String mn = frame.getMethodName();
                if ("org.apache.cxf.attachment.AttachmentSerializer".equals(cn)
                        || ("java.io.PipedInputStream".equals(cn)
                            && ("awaitSpace".equals(mn) || "receive".equals(mn)))
                        || (cn.contains("TimedBlockingPipe") && "doWrite".equals(mn))) {
                    relevant = true;
                    break;
                }
            }
            if (relevant) {
                appendThread(primary, t, stack);
            } else if ((t.getState() == Thread.State.WAITING
                            || t.getState() == Thread.State.TIMED_WAITING
                            || t.getState() == Thread.State.BLOCKED)
                        && !t.isDaemon()
                        && !t.getName().startsWith("main")) {
                appendThread(fallback, t, stack);
            }
        }

        StringBuilder sb = new StringBuilder("\n\nHung thread stack trace(s):\n");
        if (primary.length() > 0) {
            sb.append(primary);
        } else {
            sb.append("  (no pipe-deadlock frame found; dumping all non-daemon waiting threads)\n");
            sb.append(fallback);
        }
        return sb.toString();
    }

    private static void appendThread(StringBuilder sb, Thread t, StackTraceElement[] stack) {
        sb.append('\n').append("  Thread \"").append(t.getName())
          .append("\" state=").append(t.getState()).append('\n');
        for (StackTraceElement frame : stack) {
            sb.append("    at ").append(frame).append('\n');
        }
    }

    private static Dispatch<SOAPMessage> createMtomDispatch(String address) {
        Service service = Service.create(SERVICE_NAME);
        service.addPort(PORT_NAME, SOAPBinding.SOAP11HTTP_BINDING, address);
        Dispatch<SOAPMessage> dispatch =
            service.createDispatch(PORT_NAME, SOAPMessage.class, Service.Mode.MESSAGE);
        ((SOAPBinding) dispatch.getBinding()).setMTOMEnabled(true);

        HTTPConduit conduit = (HTTPConduit) ((DispatchImpl<?>) dispatch).getClient().getConduit();
        HTTPClientPolicy policy = new HTTPClientPolicy();
        policy.setConnectionTimeout(5000L);
        policy.setReceiveTimeout(RECEIVE_TIMEOUT_MS);
        conduit.setClient(policy);
        return dispatch;
    }

    private static SOAPMessage buildLargeMtomRequest() throws Exception {
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage msg = msgFactory.createMessage();
        msg.getSOAPBody()
            .addBodyElement(new QName("http://cxf.apache.org/test/mtom-stall", "upload"))
            .setTextContent("stall-test");

        byte[] data = new byte[ATTACHMENT_SIZE_BYTES];
        Arrays.fill(data, (byte) 'X');
        AttachmentPart att = msg.createAttachmentPart(
            new DataHandler(new LargeByteArrayDataSource(data)));
        att.setContentId("<large-attachment@stall-test>");
        msg.addAttachmentPart(att);
        msg.saveChanges();
        return msg;
    }

    private static SOAPMessage buildSlowMtomRequest() throws Exception {
        MessageFactory msgFactory = MessageFactory.newInstance();
        SOAPMessage msg = msgFactory.createMessage();
        msg.getSOAPBody()
            .addBodyElement(new QName("http://cxf.apache.org/test/mtom-stall", "upload"))
            .setTextContent("slow-stall-test");

        AttachmentPart att = msg.createAttachmentPart(
            new DataHandler(new GeneratedDataSource(SLOW_ATTACHMENT_SIZE_BYTES)));
        att.setContentId("<slow-attachment@stall-test>");
        msg.addAttachmentPart(att);
        msg.saveChanges();
        return msg;
    }

    /**
     * DataSource that generates {@code size} bytes on the fly without allocating
     * them all in memory, so an arbitrarily large MTOM attachment can be sent
     * cheaply to ensure OS TCP buffers are saturated during the server's stall.
     */
    private static final class GeneratedDataSource implements DataSource {
        private final int size;

        GeneratedDataSource(int size) {
            this.size = size;
        }

        @Override
        public InputStream getInputStream() {
            return new InputStream() {
                private int pos;
                @Override
                public int read() {
                    return pos < size ? (pos++ & 0x7F) : -1;
                }
                @Override
                public int read(byte[] b, int off, int len) {
                    if (pos >= size) {
                        return -1;
                    }
                    int n = Math.min(len, size - pos);
                    for (int i = 0; i < n; i++) {
                        b[off + i] = (byte) ((pos + i) & 0x7F);
                    }
                    pos += n;
                    return n;
                }
            };
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            throw new IOException("read-only");
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public String getName() {
            return "generated-" + size + "-bytes";
        }
    }

    /**
     * A real CXF/Jetty MTOM service that sleeps {@code stallMs} milliseconds before
     * draining the request attachment. During the sleep, the client's large attachment
     * body fills OS TCP receive/send buffers, triggering the CXF-8926 pipe stall.
     * After the sleep the server reads all attachment data so the response can be sent.
     *
     * Unlike a raw ServerSocket, this is a fully functional HTTP server: the
     * connection stays open throughout the stall, proving that the failure is
     * entirely on the client's MTOM write path.
     */
    @WebServiceProvider(targetNamespace = "http://cxf.apache.org/test/mtom-stall",
                        serviceName = "MtomSlowService",
                        portName = "MtomSlowPort")
    @ServiceMode(Service.Mode.MESSAGE)
    @BindingType(SOAPBinding.SOAP11HTTP_MTOM_BINDING)
    static final class SlowMtomProvider implements Provider<SOAPMessage> {
        private final long stallMs;

        SlowMtomProvider(long stallMs) {
            this.stallMs = stallMs;
        }

        @Override
        public SOAPMessage invoke(SOAPMessage request) {
            try {
                // Pause before touching the attachment. For a large attachment the
                // OS TCP buffers fill during this sleep, propagating back-pressure
                // to the client and triggering the CXF-8926 pipe stall.
                Thread.sleep(stallMs);

                // Drain all attachment data so the server can send a response.
                // In CXF's MTOM handling the attachment DataHandler reads lazily
                // from the network, so this is where the deferred bytes actually
                // flow from the socket.
                Iterator<AttachmentPart> it = request.getAttachments();
                while (it.hasNext()) {
                    AttachmentPart ap = it.next();
                    try (InputStream in = ap.getDataHandler().getInputStream()) {
                        byte[] buf = new byte[8192];
                        while (in.read(buf) != -1) {
                            // drain
                        }
                    }
                }

                SOAPMessage response = MessageFactory.newInstance().createMessage();
                response.getSOAPBody()
                    .addBodyElement(
                        new QName("http://cxf.apache.org/test/mtom-stall", "uploadResponse"))
                    .setTextContent("ok");
                return response;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new WebServiceException("Interrupted during stall", e);
            } catch (Exception e) {
                throw new WebServiceException("Error in slow MTOM provider", e);
            }
        }
    }

    private static final class LargeByteArrayDataSource implements DataSource {
        private final byte[] data;

        LargeByteArrayDataSource(byte[] data) {
            this.data = data;
        }

        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public OutputStream getOutputStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getContentType() {
            return "application/octet-stream";
        }

        @Override
        public String getName() {
            return "large-attachment";
        }
    }
}
