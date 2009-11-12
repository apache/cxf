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
import java.net.URI;
import java.net.URL;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Document;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.tools.common.ToolException;
import org.apache.cxf.tools.util.URIParserUtil;

public class Stax2DOM {
    static final String XML_NS = "http://www.w3.org/2000/xmlns/";

    public Stax2DOM() {
    }



    public Document getDocument(String wsdl) throws ToolException {
        try {
            URI wsdlURI = new URI(URIParserUtil.getAbsoluteURI(wsdl));
            if (wsdlURI.toString().startsWith("http")) {
                return getDocument(wsdlURI.toURL());
            }
            return getDocument(wsdlURI.toURL());
        } catch (ToolException e) {
            throw e;
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }

    public Document getDocument(URL url) throws ToolException {
        try {
            StreamSource src = new StreamSource(url.openStream(), url.toExternalForm());
            return StaxUtils.read(StaxUtils.createXMLStreamReader(src), true);
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }

    public Document getDocument(File wsdl) throws ToolException {
        try {
            StreamSource src = new StreamSource(wsdl);
            return StaxUtils.read(StaxUtils.createXMLStreamReader(src), true);
        } catch (Exception e) {
            throw new ToolException(e);
        }
    }


}
