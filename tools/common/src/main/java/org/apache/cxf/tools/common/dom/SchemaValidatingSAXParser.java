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

import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
    
import org.apache.cxf.common.logging.LogUtils;

/**
 * (not thread safe)
 * 
 */
public final class SchemaValidatingSAXParser {

    private static final Logger LOG = LogUtils.getL7dLogger(SchemaValidatingSAXParser.class);

    private final SAXParserFactory parserFactory = SAXParserFactory.newInstance();
    private SAXParser parser;
    private SchemaFactory schemaFactory;
    private Schema schema;

    public SchemaValidatingSAXParser() {
        try {
            parserFactory.setNamespaceAware(true);
            parser = parserFactory.newSAXParser();
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            LOG.log(Level.SEVERE, "SAX_PARSER_CONFIG_ERR_MSG");
        } catch (org.xml.sax.SAXException saxe) {
            LOG.log(Level.SEVERE, "SAX_PARSER_EXCEPTION_MSG");
        }
        setValidating(true);
    }

    private InputStream getSchemaLocation() {
        String toolspec = "/org/apache/cxf/tools/common/toolspec/tool-specification.xsd";
        return getClass().getResourceAsStream(toolspec);
    }
    
    public void setValidating(boolean validate) {
        if (validate) {
            this.schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                this.schema = schemaFactory.newSchema(new StreamSource(getSchemaLocation()));
            } catch (org.xml.sax.SAXException e) {
                LOG.log(Level.SEVERE, "SCHEMA_FACTORY_EXCEPTION_MSG");
            }
            try {
                this.parserFactory.setSchema(this.schema);
            } catch (UnsupportedOperationException e) {
                LOG.log(Level.WARNING, "SAX_PARSER_NOT_SUPPORTED", e);
            }
        }
    }

    public SAXParser getSAXParser() {
        return parser;
    }
}
