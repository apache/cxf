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
package org.apache.cxf.databinding.source;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

public class NodeDataReader implements DataReader<Node> {
    private static final Logger LOG = LogUtils.getL7dLogger(NodeDataReader.class);

    public Object read(MessagePartInfo part, Node input) {
        return read(input);
    }

    public Object read(QName name, Node input, Class type) {
        if (SAXSource.class.isAssignableFrom(type)) {
            XMLStreamReader reader = StaxUtils.createXMLStreamReader((Element)input);
            return new StaxSource(reader);
        } else if (StreamSource.class.isAssignableFrom(type)) {
            try {
                CachedOutputStream out = new CachedOutputStream();                
                DOMUtils.writeXml(input, out);
                InputStream is = out.getInputStream();
                out.close();
                
                return new StreamSource(is);
            } catch (IOException e) {
                throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
            } catch (TransformerException e) {
                throw new Fault(new Message("COULD_NOT_READ_XML_STREAM", LOG), e);
            } 
        }  
        return read(input);
    }

    public Object read(Node n) {
        return new DOMSource(n);
    }
    
    public void setSchema(Schema s) {
        // TODO Auto-generated method stub

    }

    public void setAttachments(Collection<Attachment> attachments) {
        // TODO Auto-generated method stub
        
    }

    public void setProperty(String prop, Object value) {
        // TODO Auto-generated method stub
        
    }
    
}
