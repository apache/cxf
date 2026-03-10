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

package org.apache.cxf.tools.validator.internal;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.common.util.URIParserUtil;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolException;

public class Stax2DOM {
    static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    public Stax2DOM() {
    }

    public Document getDocument(String wsdl) throws ToolException {
        try {
            URI wsdlURI = new URI(URIParserUtil.getAbsoluteURI(wsdl));
            return getDocument(wsdlURI.toURL());
        } catch (ToolException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }

    public Document getDocument(URL url) throws ToolException {
        XMLStreamReader reader = null;
        try (InputStream input = url.openStream()) {
            StreamSource src = new StreamSource(input, url.toExternalForm());
            reader = StaxUtils.createXMLStreamReader(src);
            return StaxUtils.read(reader, true);
        } catch (Exception e) {
            throw new ToolException(e);
        } finally {
            try {
                StaxUtils.close(reader);
            } catch (XMLStreamException e1) {
                throw new ToolException(e1);
            }
        }
    }

    public Document getDocument(File wsdl) throws ToolException {
        XMLStreamReader reader = null;
        try {
            StreamSource source = new StreamSource(wsdl);
            reader = StaxUtils.createXMLStreamReader(source);
            return StaxUtils.read(reader, true);
        } catch (Exception e) {
            throw new ToolException(e);
        } finally {
            try {
                if (reader != null) {
                    try {
                        //on woodstox, calling closeCompletely will allow any
                        //cached things like dtds and such to be completely
                        //closed and cleaned up.
                        reader.getClass().getMethod("closeCompletely").invoke(reader);
                    } catch (Throwable t) {
                        //ignore
                    }
                    reader.close();
                }
            } catch (XMLStreamException e) {
                // throw or change do nothing.
                throw new ToolException(e);
            }
        }
    }

}
