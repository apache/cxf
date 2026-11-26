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
package org.apache.cxf.staxutils;

import java.io.StringReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class W3CNamespaceContextTest {

    private static Element parse(String xml) throws Exception {
        Document doc = StaxUtils.read(new StringReader(xml));
        return doc.getDocumentElement();
    }

    @Test
    public void testGetPrefixIgnoresNonNamespaceAttributes() throws Exception {
        Element root = parse("<root targetNamespace=\"urn:a\"><child/></root>");
        W3CNamespaceContext ctx = new W3CNamespaceContext(DOMUtils.getFirstElement(root));
        assertNull(ctx.getPrefix("urn:a"));
    }

    @Test
    public void testGetPrefixFromDeclarations() throws Exception {
        Element root = parse("<root xmlns:a=\"urn:a\" xmlns=\"urn:d\" other=\"urn:d\"><child/></root>");
        W3CNamespaceContext ctx = new W3CNamespaceContext(DOMUtils.getFirstElement(root));
        assertEquals("a", ctx.getPrefix("urn:a"));
        assertEquals("", ctx.getPrefix("urn:d"));
    }
}
