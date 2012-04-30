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
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.W3CDOMStreamReader;
import org.apache.ws.security.WSSConfig;


public abstract class AbstractXmlSecInHandler {
    private static final Logger LOG = 
        LogUtils.getL7dLogger(AbstractXmlSecInHandler.class);
    
    static {
        WSSConfig.init();
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
                doc = DOMUtils.readXml(new InputStreamReader(is, "UTF-8"));
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
        Response response = Response.status(400).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }

    protected Element getNode(Element parent, String ns, String name, int index) {
        NodeList list = parent.getElementsByTagNameNS(ns, name);
        if (list != null && list.getLength() >= index + 1) {
            return (Element)list.item(index);
        } 
        return null;
    }
    
}
