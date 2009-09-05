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


package org.apache.cxf.systest.provider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.activation.DataHandler;
import javax.annotation.Resource;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.message.Message;

@WebServiceProvider(serviceName = "AttachmentStreamSourceXMLProvider")
@ServiceMode(value = Service.Mode.PAYLOAD)
@javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")
public class AttachmentStreamSourceXMLProvider implements Provider<StreamSource> {

    @Resource
    protected WebServiceContext wsContext;
    
    public StreamSource invoke(StreamSource source) {
        
        MessageContext mc = wsContext.getMessageContext();
        
        String httpMethod = (String)mc.get(MessageContext.HTTP_REQUEST_METHOD);
        if ("POST".equals(httpMethod)) {
            
            int count = 0;
            // we really want to verify that a root part is a proper XML as expected
            DOMResult result = new DOMResult();
            try {
                Transformer transformer = XMLUtils.newTransformer();
                transformer.transform(source, result);
                count = 
                    Integer.parseInt(((Document)result.getNode()).getDocumentElement().getAttribute("count"));
            } catch (Exception ex) {
                // ignore
            }
            
            Map<String, DataHandler> dataHandlers = CastUtils.cast(
                (Map)mc.get(MessageContext.INBOUND_MESSAGE_ATTACHMENTS));
            StringBuilder buf = new StringBuilder();
            buf.append("<response>");
            int i = 0;
            for (Map.Entry<String, DataHandler> entry : dataHandlers.entrySet()) {
                if (i++ > count) {
                    break;
                }
                try {
                    InputStream is = entry.getValue().getInputStream();
                    ByteArrayOutputStream bous = new ByteArrayOutputStream();
                    IOUtils.copy(is, bous);
            
                    buf.append("<att contentId=\"" + entry.getKey() + "\">");
                    buf.append(Base64Utility.encode(bous.toByteArray()));
                    buf.append("</att>");
                    
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
            buf.append("</response>");
            
            Map<String, List<String>> respHeaders = CastUtils
                .cast((Map<?, ?>)mc.get(MessageContext.HTTP_RESPONSE_HEADERS));
            if (respHeaders == null) {
                respHeaders = new HashMap<String, List<String>>();
                mc.put(MessageContext.HTTP_RESPONSE_HEADERS, respHeaders);
            }

            
            List<String> contentTypeValues = new ArrayList<String>();
            contentTypeValues.add("application/xml+custom");
            respHeaders.put(Message.CONTENT_TYPE, contentTypeValues);

            Map<String, DataHandler> outDataHandlers 
                = CastUtils.cast((Map)mc.get(MessageContext.OUTBOUND_MESSAGE_ATTACHMENTS));
            byte[] data = new byte[50];
            for (int x = 0; x < data.length; x++) {
                data[x] = (byte)(x + (int)'0');
            }
            DataHandler foo = new DataHandler(new ByteArrayDataSource(data, "application/octet-stream"));
            outDataHandlers.put("foo", foo);
            
            return new StreamSource(new StringReader(buf.toString()));
        }
        return source;
        
    }

}
