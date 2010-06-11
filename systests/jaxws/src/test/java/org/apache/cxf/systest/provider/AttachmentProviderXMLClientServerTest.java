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

package org.apache.cxf.systest.provider;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.junit.BeforeClass;
import org.junit.Test;

public class AttachmentProviderXMLClientServerTest extends AbstractBusClientServerTestBase {
    private static final String ADDRESS = AttachmentServer.ADDRESS;
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                launchServer(AttachmentServer.class, true));
    }

    @Test
    public void testRequestWithAttachment() throws Exception {
        
        HttpURLConnection connection =  
            (HttpURLConnection)new URL(ADDRESS).openConnection();
        connection.setRequestMethod("POST");
        
        String ct = "multipart/related; type=\"text/xml\"; " + "start=\"rootPart\"; "
                    + "boundary=\"----=_Part_4_701508.1145579811786\"";
        connection.addRequestProperty("Content-Type", ct);
        
        connection.setDoOutput(true);
        
        InputStream is = getClass().getResourceAsStream("attachmentData");
        IOUtils.copy(is, connection.getOutputStream());
        connection.getOutputStream().close();
        is.close();

        assertTrue("wrong content type: " + connection.getContentType(),
                   connection.getContentType().contains("multipart/related"));
        String input = IOUtils.toString(connection.getInputStream());
        
        int idx = input.indexOf("--uuid");
        int idx2 = input.indexOf("--uuid", idx + 5);
        String root = input.substring(idx, idx2);
        idx = root.indexOf("\r\n\r\n");
        root = root.substring(idx).trim();
        

        Document result = XMLUtils.parse(root);
        
        List<Element> resList = DOMUtils.findAllElementsByTagName(result.getDocumentElement(), "att");
        assertEquals("Two attachments must've been encoded", 2, resList.size());
        
        verifyAttachment(resList, "foo", "foobar");
        verifyAttachment(resList, "bar", "barbaz");
        
        input = input.substring(idx2);
        assertTrue(input.contains("<foo>"));
        assertTrue(input.contains("ABCDEFGHIJKLMNOP"));
    }

    private void verifyAttachment(List<Element> atts, String contentId, String value) {

        for (Element expElem : atts) {
            String child = expElem.getFirstChild().getNodeValue();
            String contentIdVal = expElem.getAttribute("contentId");
            if (contentId.equals(contentIdVal)
                && (Base64Utility.encode(value.getBytes()).equals(child)
                    || Base64Utility.encode((value + "\n").getBytes()).equals(child))) {
                return;    
            }
        }
        
        fail("No encoded attachment with id " + contentId + " found");
    }
}
