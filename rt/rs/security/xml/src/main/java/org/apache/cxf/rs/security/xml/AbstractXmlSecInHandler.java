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

package org.apache.cxf.rs.security.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.wss4j.common.crypto.WSProviderConfig;


public abstract class AbstractXmlSecInHandler {
    /**
     * A key used to reference the cert that was used to verify the signed request
     */
    public static final String SIGNING_CERT = "xml.security.signing.cert";
    /**
     * A key used to reference the public key that was used to verify the signed request
     */
    public static final String SIGNING_PUBLIC_KEY = "xml.security.signing.public.key";
    
    protected static final String SIG_NS = "http://www.w3.org/2000/09/xmldsig#";
    protected static final String SIG_PREFIX = "ds";
    protected static final String ENC_NS = "http://www.w3.org/2001/04/xmlenc#";
    protected static final String ENC_PREFIX = "xenc";
    protected static final String WSU_NS = 
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
    
    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractXmlSecInHandler.class);
    
    static {
        WSProviderConfig.init();
    }
    
    private boolean allowEmptyBody;
    
    public void setAllowEmptyBody(boolean allow) {
        this.allowEmptyBody = allow;
    }
    
    protected Document getDocument(Message message) {
        String method = (String)message.get(Message.HTTP_REQUEST_METHOD);
        if ("GET".equals(method)) {
            return null;
        }
        
        Document doc = null;
        InputStream is = message.getContent(InputStream.class);
        if (is != null) {
            try {
                doc = StaxUtils.read(new InputStreamReader(is, StandardCharsets.UTF_8));
            } catch (Exception ex) {
                throwFault("Invalid XML payload", ex);
            }
        } else {
            XMLStreamReader reader = message.getContent(XMLStreamReader.class);
            if (reader instanceof W3CDOMStreamReader) {
                doc = ((W3CDOMStreamReader)reader).getDocument();
            }
        }
        if (doc == null && !allowEmptyBody) {
            throwFault("No payload is available", null);
        }
        return doc;
    }
    
    protected void throwFault(String error, Exception ex) {
        LOG.warning(error);
        Response response = JAXRSUtils.toResponseBuilder(400).entity(error).build();
        throw ExceptionUtils.toBadRequestException(null, response);
    }

    protected Element getNode(Element parent, String ns, String name, int index) {
        NodeList list = parent.getElementsByTagNameNS(ns, name);
        if (list != null && list.getLength() >= index + 1) {
            return (Element)list.item(index);
        } 
        return null;
    }
    
}
