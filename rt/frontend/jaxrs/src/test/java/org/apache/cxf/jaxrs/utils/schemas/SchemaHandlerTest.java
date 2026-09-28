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

package org.apache.cxf.jaxrs.utils.schemas;

import java.io.File;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javax.xml.validation.Schema;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SchemaHandlerTest {
    private static final String NAMESPACE = "urn:test:schema:handler";
    private static final String IMPORTED_NAMESPACE = "urn:test:schema:handler:imported";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRelativeImportFromFile() throws Exception {
        writeFile("imported.xsd", importedSchema(""));
        File main = writeFile("main.xsd", mainSchema("imported.xsd"));

        Schema schema = SchemaHandler.createSchema(
            Collections.singletonList(main.toURI().toString()), null, null);
        assertNotNull(schema);
    }

    @Test
    public void testRelativeImportFromJar() throws Exception {
        File jar = tempFolder.newFile("schemas.jar");
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jar.toPath()))) {
            jos.putNextEntry(new ZipEntry("schemas/main.xsd"));
            jos.write(mainSchema("imported.xsd").getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("schemas/imported.xsd"));
            jos.write(importedSchema("").getBytes(StandardCharsets.UTF_8));
            jos.closeEntry();
        }

        Schema schema = SchemaHandler.createSchema(
            Collections.singletonList("jar:" + jar.toURI() + "!/schemas/main.xsd"), null, null);
        assertNotNull(schema);
    }

    @Test
    public void testExternalDtdIsNotLoaded() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            String doctype = "<!DOCTYPE xsd:schema SYSTEM 'http://127.0.0.1:" + probeServer.getPort()
                + "/evil.dtd'>";
            File main = writeFile("main.xsd", importedSchema(doctype));

            try {
                SchemaHandler.createSchema(Collections.singletonList(main.toURI().toString()), null, null);
                fail("Schema with an external DTD should not be loaded");
            } catch (IllegalArgumentException ex) {
                // expected
            }

            probeServer.awaitCompletion();
            assertFalse("External DTD should not be loaded", probeServer.wasConnected());
        }
    }

    @Test
    public void testRemoteImportIsNotLoaded() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            File main = writeFile("main.xsd",
                mainSchema("http://127.0.0.1:" + probeServer.getPort() + "/imported.xsd"));

            try {
                SchemaHandler.createSchema(Collections.singletonList(main.toURI().toString()), null, null);
                fail("Schema importing a remote schema should not be loaded");
            } catch (IllegalArgumentException ex) {
                // expected
            }

            probeServer.awaitCompletion();
            assertFalse("Remote schema import should not be loaded", probeServer.wasConnected());
        }
    }

    private static String mainSchema(String importLocation) {
        return "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' xmlns:i='" + IMPORTED_NAMESPACE + "' "
            + "targetNamespace='" + NAMESPACE + "' elementFormDefault='qualified'>"
            + "<xsd:import namespace='" + IMPORTED_NAMESPACE + "' schemaLocation='" + importLocation + "'/>"
            + "<xsd:element name='value' type='i:valueType'/>"
            + "</xsd:schema>";
    }

    private static String importedSchema(String doctype) {
        return "<?xml version='1.0'?>" + doctype
            + "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' "
            + "targetNamespace='" + IMPORTED_NAMESPACE + "'>"
            + "<xsd:simpleType name='valueType'><xsd:restriction base='xsd:string'/></xsd:simpleType>"
            + "</xsd:schema>";
    }

    private File writeFile(String name, String content) throws Exception {
        File file = tempFolder.newFile(name);
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private static final class LocalHttpProbeServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final AtomicBoolean connected = new AtomicBoolean();
        private final Thread thread;

        private LocalHttpProbeServer() throws Exception {
            this.serverSocket = new ServerSocket(0);
            this.serverSocket.setSoTimeout(750);
            this.thread = new Thread(this::acceptOneConnection, "SchemaHandlerTest-HttpProbe");
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
