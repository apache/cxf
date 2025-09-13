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

package org.apache.cxf.systest.provider.datasource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import jakarta.activation.DataSource;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.TestUtil;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DataSourceProviderTest extends AbstractBusClientServerTestBase {
    static String serverPort = TestUtil.getPortNumber(Server.class);

    static final Logger LOG = LogUtils.getLogger(DataSourceProviderTest.class);
    private static final String BOUNDARY = "----=_Part_4_701508.1145579811786";

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                launchServer(Server.class, true));
    }

    @Test
    public void invokeOnServer() throws Exception {
        URL url = new URL("http://localhost:" + serverPort + "/test/foo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        printSource(new StreamSource(conn.getInputStream()));
    }

    @Test
    public void invokePostAttachmentToServer() throws Exception {
        URL url = new URL("http://localhost:" + serverPort + "/test/foo");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);

        String contentType = "multipart/related; type=\"text/xml\"; "
            + "start=\"attachmentData\"; "
            + "boundary=\"" + BOUNDARY + "\"";

        InputStream in = getClass().getResourceAsStream("/attachmentBinaryData");
        assertNotNull("could not load test data", in);

        conn.setRequestMethod("POST");
        conn.addRequestProperty("Content-Type", contentType);
        try (OutputStream out = conn.getOutputStream()) {
            IOUtils.copy(in, out);
        }
        MimeMultipart mm = readAttachmentParts(conn.getContentType(),
                                               conn.getInputStream());

        assertEquals("incorrect number of parts received by server", 3, mm.getCount());

    }

    private void printSource(Source source) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            StreamResult sr = new StreamResult(bos);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Transformer transformer = transformerFactory.newTransformer();
            Properties oprops = new Properties();
            oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperties(oprops);
            transformer.transform(source, sr);
            assertEquals(bos.toString(), "<doc><response>Hello</response></doc>");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static MimeMultipart readAttachmentParts(String contentType, InputStream bais) throws
        MessagingException, IOException {
        DataSource source = new ByteArrayDataSource(bais, contentType);
        MimeMultipart mpart = new MimeMultipart(source);
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mm = new MimeMessage(session);
        mm.setContent(mpart);
        mm.addHeaderLine("Content-Type:" + contentType);
        return (MimeMultipart) mm.getContent();
    }


}
