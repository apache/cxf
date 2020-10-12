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
package org.apache.cxf.ws.security.tokenstore;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.wss4j.common.util.DateUtil;

import static org.apache.wss4j.common.WSS4JConstants.WST_NS_05_12;
import static org.apache.wss4j.common.WSS4JConstants.WSU_NS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SecurityTokenTest {

    @org.junit.Test
    public void testCreateSecurityToken() throws Exception {
        String key = "key";
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expires = created.plusSeconds(20L);

        SecurityToken token = new SecurityToken(key, created, expires);
        assertEquals(key, token.getId());
        assertEquals(created, token.getCreated());
        assertEquals(expires, token.getExpires());
    }

    @org.junit.Test
    public void testParseLifetimeElement() throws Exception {
        String key = "key";
        Element tokenElement = DOMUtils.createDocument().createElement("token");

        // Create Lifetime
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expires = created.plusSeconds(20L);

        writer.writeStartElement("wst", "Lifetime", WST_NS_05_12);
        writer.writeStartElement("wsu", "Created", WSU_NS);
        writer.writeCharacters(created.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();

        writer.writeStartElement("wsu", "Expires", WSU_NS);
        writer.writeCharacters(expires.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();
        writer.writeEndElement();

        SecurityToken token = new SecurityToken(key, tokenElement, writer.getDocument().getDocumentElement());
        assertEquals(key, token.getId());
        assertEquals(created, token.getCreated());
        assertEquals(expires, token.getExpires());
    }

    @org.junit.Test
    public void testLifetimeNoCreated() throws Exception {
        String key = "key";
        Element tokenElement = DOMUtils.createDocument().createElement("token");

        // Create Lifetime
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        Instant expires = created.plusSeconds(20L);

        writer.writeStartElement("wst", "Lifetime", WST_NS_05_12);

        writer.writeStartElement("wsu", "Expires", WSU_NS);
        writer.writeCharacters(expires.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();
        writer.writeEndElement();

        SecurityToken token = new SecurityToken(key, tokenElement, writer.getDocument().getDocumentElement());
        assertEquals(key, token.getId());
        // It should default to the current time
        assertNotNull(token.getCreated());
        assertEquals(expires, token.getExpires());
    }

    @org.junit.Test
    public void testLifetimeNoExpires() throws Exception {
        String key = "key";
        Element tokenElement = DOMUtils.createDocument().createElement("token");

        // Create Lifetime
        W3CDOMStreamWriter writer = new W3CDOMStreamWriter();
        Instant created = Instant.now().truncatedTo(ChronoUnit.MILLIS);

        writer.writeStartElement("wst", "Lifetime", WST_NS_05_12);
        writer.writeStartElement("wsu", "Created", WSU_NS);
        writer.writeCharacters(created.atZone(ZoneOffset.UTC).format(DateUtil.getDateTimeFormatter(true)));
        writer.writeEndElement();

        writer.writeEndElement();

        SecurityToken token = new SecurityToken(key, tokenElement, writer.getDocument().getDocumentElement());
        assertEquals(key, token.getId());
        assertEquals(created, token.getCreated());
        assertNull(token.getExpires());
    }

    @org.junit.Test
    public void testTokenExpiry() {
        SecurityToken token = new SecurityToken();

        Instant expires = Instant.now().plusSeconds(5L * 60L);
        token.setExpires(expires);

        assertFalse(token.isExpired());
        assertFalse(token.isAboutToExpire(100L));
        assertTrue(token.isAboutToExpire((5L * 60L * 1000L) + 1L));
    }
}
