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

package org.apache.cxf.systest.jaxrs.security.samlsso;

import java.net.URL;
import java.security.cert.X509Certificate;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.utils.Constants;

import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test the SAML SSO Metadata service
 */
public class MetadataTest extends AbstractBusClientServerTestBase {
    public static final String PORT = MetadataServer.PORT;

    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly",
                   launchServer(MetadataServer.class, true));
    }

    @org.junit.Test
    public void testGetMetadata() throws Exception {
        URL busFile = MetadataTest.class.getResource("client.xml");

        String address = "https://localhost:" + PORT + "/sso/metadata";
        WebClient client = WebClient.create(address, busFile.toString());
        client.accept("text/xml");

        Response response = client.get();
        assertEquals(response.getStatus(), 200);
        Document doc = response.readEntity(Document.class);
        assertEquals("EntityDescriptor", doc.getDocumentElement().getLocalName());

        // Now validate the signature
        Element signature =
            (Element)doc.getElementsByTagNameNS(Constants.SignatureSpecNS, "Signature").item(0);
        assertNotNull(signature);
        XMLSignature signatureElem = new XMLSignature(signature, "");
        doc.getDocumentElement().setIdAttributeNS(null, "ID", true);

        X509Certificate signingCert = signatureElem.getKeyInfo().getX509Certificate();
        assertNotNull(signingCert);
        assertTrue(signatureElem.checkSignatureValue(signingCert));
    }

}
