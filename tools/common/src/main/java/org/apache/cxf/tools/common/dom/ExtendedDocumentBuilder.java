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

package org.apache.cxf.tools.common.dom;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.validation.Schema;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.commons.xml.secure.SecureSchemaFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;

/**
 * (not thread safe)
 *
 */
public class ExtendedDocumentBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(ExtendedDocumentBuilder.class);

    private static final Schema TOOLSPEC_SCHEMA;
    static {
        // Shipped in this artifact: if it cannot be loaded, the artifact is broken.
        String path = "/org/apache/cxf/tools/common/toolspec/tool-specification.xsd";
        URL url = ExtendedDocumentBuilder.class.getResource(path);
        if (url == null) {
            throw new IllegalStateException("Missing tool specification schema " + path);
        }
        try {
            TOOLSPEC_SCHEMA = SecureSchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
        } catch (SAXException e) {
            throw new IllegalStateException("Invalid tool specification schema " + path, e);
        }
    }

    private Schema schema;

    public ExtendedDocumentBuilder() {
    }

    public void setValidating(boolean validate) {
        this.schema = validate ? TOOLSPEC_SCHEMA : null;
    }

    public Document parse(InputStream in) throws SAXException, IOException, XMLStreamException {
        if (in == null && LOG.isLoggable(Level.FINE)) {
            LOG.fine("ExtendedDocumentBuilder trying to parse a null inputstream");
        }
        if (this.schema != null) {
            // validating, which only the DOM path does
            return DOMUtils.createNSDocumentBuilder(this.schema).parse(in);
        }
        return StaxUtils.read(in);
    }

}
