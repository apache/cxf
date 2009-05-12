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

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DataSourceProviderTest extends AbstractBusClientServerTestBase {

    static final Logger LOG = LogUtils.getLogger(DataSourceProviderTest.class);
    private static final String BOUNDARY = "----=_Part_4_701508.1145579811786";
    private HttpURLConnection conn;
    private URL url;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                launchServer(Server.class, true));
    }
    
    @Before
    public void createConnection() throws Exception {
        url = new URL("http://localhost:9000/test/foo");
        conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
    }


    @Test 
    public void invokeOnServer() throws Exception { 
        url = new URL("http://localhost:9000/test/foo");
        conn = (HttpURLConnection) url.openConnection();
        printSource(new StreamSource(conn.getInputStream())); 
    }
    
    @Test
    public void postAttachmentToServer() throws Exception {
        String contentType = "multipart/related; type=\"text/xml\"; "
            + "start=\"attachmentData\"; "
            + "boundary=\"" + BOUNDARY + "\"";

        InputStream in = getClass().getResourceAsStream("/attachmentBinaryData");
        assertNotNull("could not load test data", in);

        conn.setRequestMethod("POST");
        conn.addRequestProperty("Content-Type", contentType);
        OutputStream out = conn.getOutputStream();
        IOUtils.copy(in, out);
        out.close();
        MimeMultipart mm = readAttachmentParts(conn.getContentType(),
                                               conn.getInputStream());

        assertEquals("incorrect number of parts received by server", 3, mm.getCount());

    }

    private void printSource(Source source) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            StreamResult sr = new StreamResult(bos);
            Transformer trans = TransformerFactory.newInstance().newTransformer();
            Properties oprops = new Properties();
            oprops.put(OutputKeys.OMIT_XML_DECLARATION, "yes");
            trans.setOutputProperties(oprops);
            trans.transform(source, sr);
            assertEquals(bos.toString(), "<doc><response>Hello</response></doc>");
            bos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static MimeMultipart readAttachmentParts(String contentType, InputStream bais) throws 
        MessagingException, IOException {
        Session session = Session.getDefaultInstance(new Properties());
        MimeMessage mm = new MimeMessage(session, bais);
        mm.addHeaderLine("Content-Type:" + contentType);
        return (MimeMultipart) mm.getContent();
    }


}
