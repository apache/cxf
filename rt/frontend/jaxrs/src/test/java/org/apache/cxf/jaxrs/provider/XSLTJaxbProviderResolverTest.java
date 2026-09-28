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

package org.apache.cxf.jaxrs.provider;

import java.io.File;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XSLTJaxbProviderResolverTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testResolverIsUsedForLocalImport() throws Exception {
        writeFile("imported.xsl", stylesheet(""));
        File main = writeFile("main.xsl", stylesheet("<xsl:import href='imported.xsl'/>"));
        RecordingResolver resolver = new RecordingResolver();

        Templates templates = createTemplates(main.toURI().toURL(), resolver, true);

        assertNotNull(templates);
        assertTrue("Resolver should be used for a local import", resolver.wasCalled());
    }

    @Test
    public void testResolverIsNotUsedForRemoteImport() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            File main = writeFile("main.xsl", stylesheet(
                "<xsl:import href='http://127.0.0.1:" + probeServer.getPort() + "/imported.xsl'/>"));
            RecordingResolver resolver = new RecordingResolver();

            Templates templates = createTemplates(main.toURI().toURL(), resolver, true);

            probeServer.awaitCompletion();
            assertFalse("Remote import should not be loaded", probeServer.wasConnected());
            assertFalse("Resolver should not be used for a remote import", resolver.wasCalled());
            assertNull(templates);
        }
    }

    @Test
    public void testResolverIsNotUsedForRemoteArchiveImport() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            File main = writeFile("main.xsl", stylesheet(
                "<xsl:import href='jar:http://127.0.0.1:" + probeServer.getPort() + "/a.jar!/imported.xsl'/>"));
            RecordingResolver resolver = new RecordingResolver();

            Templates templates = createTemplates(main.toURI().toURL(), resolver, true);

            probeServer.awaitCompletion();
            assertFalse("Remote archive import should not be loaded", probeServer.wasConnected());
            assertFalse("Resolver should not be used for a remote archive import", resolver.wasCalled());
            assertNull(templates);
        }
    }

    @Test
    public void testResolverIsUsedForRemoteImportWithoutSecureProcessing() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            File main = writeFile("main.xsl", stylesheet(
                "<xsl:import href='http://127.0.0.1:" + probeServer.getPort() + "/imported.xsl'/>"));
            RecordingResolver resolver = new RecordingResolver();

            createTemplates(main.toURI().toURL(), resolver, false);

            assertTrue("Resolver should be used when secure processing is disabled", resolver.wasCalled());
        }
    }

    private static Templates createTemplates(URL url, URIResolver resolver, boolean secureProcessing) {
        XSLTJaxbProvider<Object> provider = new XSLTJaxbProvider<>();
        provider.setSecureProcessing(secureProcessing);
        provider.setResolver(resolver);
        return provider.createTemplates(url);
    }

    private static String stylesheet(String imports) {
        return "<xsl:stylesheet version='1.0' xmlns:xsl='http://www.w3.org/1999/XSL/Transform'>"
            + imports
            + "<xsl:template match='/'><result/></xsl:template>"
            + "</xsl:stylesheet>";
    }

    private File writeFile(String name, String content) throws Exception {
        File file = tempFolder.newFile(name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * Records that it was called and returns an in-memory stylesheet without fetching anything.
     */
    private static final class RecordingResolver implements URIResolver {
        private final AtomicBoolean called = new AtomicBoolean();

        @Override
        public Source resolve(String href, String base) {
            called.set(true);
            return new StreamSource(new StringReader(stylesheet("")), href);
        }

        boolean wasCalled() {
            return called.get();
        }
    }

    private static final class LocalHttpProbeServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final AtomicBoolean connected = new AtomicBoolean();
        private final Thread thread;

        private LocalHttpProbeServer() throws Exception {
            this.serverSocket = new ServerSocket(0);
            this.serverSocket.setSoTimeout(750);
            this.thread = new Thread(this::acceptOneConnection, "XSLTJaxbProviderResolverTest-HttpProbe");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        private void acceptOneConnection() {
            try (Socket socket = serverSocket.accept()) {
                connected.set(true);
                OutputStream out = socket.getOutputStream();
                out.write("HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
            } catch (Exception ex) {
                // timeout/no connection is expected for this test
            }
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private boolean wasConnected() {
            return connected.get();
        }

        private void awaitCompletion() throws InterruptedException {
            thread.join(1500);
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            awaitCompletion();
        }
    }
}
