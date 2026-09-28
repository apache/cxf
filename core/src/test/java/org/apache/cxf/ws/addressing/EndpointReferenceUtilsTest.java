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

package org.apache.cxf.ws.addressing;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.validation.Schema;

import org.w3c.dom.Document;

import org.apache.cxf.resource.URIResolver;
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.ws.commons.schema.XmlSchema;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EndpointReferenceUtilsTest {
    private static final String NAMESPACE = "urn:test:endpoint:reference:utils";
    private static final String MEMORY_SYSTEM_ID = "memory:/schema.xsd";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGetSchemaCanOpenSourceUriAllowedByDefaultSchemes() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            String sourceUri = "http://127.0.0.1:" + probeServer.getPort() + "/schema.xsd";
            ServiceInfo serviceInfo = createServiceInfo(sourceUri);

            Schema schema = EndpointReferenceUtils.getSchema(serviceInfo, null);
            assertNotNull(schema);

            probeServer.awaitCompletion();
            assertTrue("Default allowed schemes should permit opening sourceURI", probeServer.wasConnected());
        }
    }

    @Test
    public void testGetSchemaDoesNotOpenDisallowedFtpSourceUri() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            String sourceUri = "ftp://127.0.0.1:" + probeServer.getPort() + "/schema.xsd";

            assertFalse("ftp scheme should be disallowed by default",
                URIResolver.getAllowedSchemes().contains("ftp"));

            ServiceInfo serviceInfo = createServiceInfo(sourceUri);
            Schema schema = EndpointReferenceUtils.getSchema(serviceInfo, null);
            assertNotNull(schema);

            probeServer.awaitCompletion();
            assertFalse("Disallowed sourceURI should not be opened", probeServer.wasConnected());
        }
    }

    @Test
    public void testGetSchemaDoesNotResolveDtdFromSourceUri() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            File xsd = writeSchemaWithExternalDtd("source.xsd", NAMESPACE, probeServer.getPort());
            ServiceInfo serviceInfo = createServiceInfo(xsd.toURI().toString());

            Schema schema = EndpointReferenceUtils.getSchema(serviceInfo, null);

            probeServer.awaitCompletion();
            assertFalse("External DTD of the sourceURI document should not be loaded", probeServer.wasConnected());
            assertNotNull(schema);
        }
    }

    @Test
    public void testGetSchemaDoesNotResolveDtdFromImportedSchema() throws Exception {
        try (LocalHttpProbeServer probeServer = new LocalHttpProbeServer()) {
            String importedNamespace = "urn:test:endpoint:reference:utils:imported";
            File importedXsd = writeSchemaWithExternalDtd("imported.xsd", importedNamespace, probeServer.getPort());

            String schemaText =
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' "
                + "targetNamespace='" + NAMESPACE + "' elementFormDefault='qualified'>"
                + "<xsd:import namespace='" + importedNamespace + "' "
                + "schemaLocation='" + importedXsd.toURI().toString() + "'/>"
                + "<xsd:element name='value' type='xsd:string'/>"
                + "</xsd:schema>";
            Document doc = StaxUtils.read(new ByteArrayInputStream(schemaText.getBytes(StandardCharsets.UTF_8)));

            // Only the SchemaInfo element carries the import, so it is resolved by getSchema itself
            ServiceInfo serviceInfo = createServiceInfo(MEMORY_SYSTEM_ID);
            serviceInfo.getSchemas().iterator().next().setElement(doc.getDocumentElement());

            Schema schema = EndpointReferenceUtils.getSchema(serviceInfo, null);

            probeServer.awaitCompletion();
            assertFalse("External DTD of an imported schema should not be loaded", probeServer.wasConnected());
            assertNotNull(schema);
        }
    }

    private File writeSchemaWithExternalDtd(String name, String namespace, int port) throws Exception {
        String schemaText =
            "<?xml version='1.0'?>"
            + "<!DOCTYPE xsd:schema SYSTEM 'http://127.0.0.1:" + port + "/evil.dtd'>"
            + "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' "
            + "targetNamespace='" + namespace + "' elementFormDefault='qualified'>"
            + "<xsd:element name='other' type='xsd:string'/>"
            + "</xsd:schema>";
        File file = tempFolder.newFile(name);
        Files.write(file.toPath(), schemaText.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    private ServiceInfo createServiceInfo(String sourceUri) throws Exception {
        String namespace = NAMESPACE;
        String schemaText =
            "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' "
            + "targetNamespace='" + namespace + "' elementFormDefault='qualified'>"
            + "<xsd:element name='value' type='xsd:string'/>"
            + "</xsd:schema>";

        Document doc = StaxUtils.read(new ByteArrayInputStream(schemaText.getBytes(StandardCharsets.UTF_8)));

        ServiceInfo serviceInfo = new ServiceInfo();
        XmlSchema xmlSchema = serviceInfo.getXmlSchemaCollection().read(doc.getDocumentElement(), sourceUri);

        SchemaInfo schemaInfo = new SchemaInfo(namespace);
        schemaInfo.setSchema(xmlSchema);
        schemaInfo.setSystemId(MEMORY_SYSTEM_ID);
        serviceInfo.addSchema(schemaInfo);

        return serviceInfo;
    }

    private static final class LocalHttpProbeServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final AtomicBoolean connected = new AtomicBoolean();
        private final Thread thread;

        private LocalHttpProbeServer() throws Exception {
            this.serverSocket = new ServerSocket(0);
            this.serverSocket.setSoTimeout(750);
            this.thread = new Thread(this::acceptOneConnection, "EndpointReferenceUtilsTest-HttpProbe");
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
