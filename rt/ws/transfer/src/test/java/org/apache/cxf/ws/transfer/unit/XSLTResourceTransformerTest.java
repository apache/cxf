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

package org.apache.cxf.ws.transfer.unit;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.transfer.Representation;
import org.apache.cxf.ws.transfer.validationtransformation.ResourceTransformer;
import org.apache.cxf.ws.transfer.validationtransformation.XSLTResourceTransformer;

import org.junit.Assert;
import org.junit.Test;

public class XSLTResourceTransformerTest {

    private Representation loadRepresentation(InputStream input) throws XMLStreamException {
        Document doc = StaxUtils.read(input);
        Representation representation = new Representation();
        representation.setAny(doc.getDocumentElement());
        return representation;
    }

    @Test
    public void transformTest() throws XMLStreamException {
        ResourceTransformer transformer = new XSLTResourceTransformer(new StreamSource(
                getClass().getResourceAsStream("/xml/xsltresourcetransformer/stylesheet.xsl")));
        Representation representation = loadRepresentation(
                getClass().getResourceAsStream("/xml/xsltresourcetransformer/representation.xml"));

        transformer.transform(representation, null);

        Element representationEl = (Element) representation.getAny();
        Assert.assertEquals("Expected root element with name \"person\".", "person",
                representationEl.getLocalName());
        Assert.assertTrue("Expected one element \"firstname\".",
                representationEl.getElementsByTagName("firstname").getLength() == 1);
        Assert.assertTrue("Expected one element \"lastname\".",
                representationEl.getElementsByTagName("lastname").getLength() == 1);
    }
}
