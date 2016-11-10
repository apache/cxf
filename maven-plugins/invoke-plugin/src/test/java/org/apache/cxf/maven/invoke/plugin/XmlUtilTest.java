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
package org.apache.cxf.maven.invoke.plugin;

import java.io.IOException;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.xml.sax.SAXException;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class XmlUtilTest {

    @Test
    public void shouldCompileXPathExpressions() throws XPathExpressionException {
        final XPathExpression expression = XmlUtil.xpathExpression("//*");

        assertNotNull("XPath expressions should be compiled", expression);
    }

    @Test
    public void shouldDomDocuments() {
        final Document document = XmlUtil.document();

        assertNotNull("Document should be created", document);
    }

    @Test
    public void shouldParseXml() throws SAXException, IOException {
        final Node node = XmlUtil.parse("<some><xml/></some>");

        assertNotNull("XML should be parsed", node);
    }
}
