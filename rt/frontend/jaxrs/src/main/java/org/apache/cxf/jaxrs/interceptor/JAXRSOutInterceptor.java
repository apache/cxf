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

package org.apache.cxf.jaxrs.interceptor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.ProduceMime;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.jaxrs.JAXRSUtils;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;

public class JAXRSOutInterceptor extends AbstractOutDatabindingInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(JAXRSOutInterceptor.class);
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(JAXRSOutInterceptor.class);

    public JAXRSOutInterceptor() {
        super(Phase.MARSHAL);
    }

    @SuppressWarnings("unchecked")
    public void handleMessage(Message message) {
        Exchange exchange = message.getExchange();
        OperationResourceInfo operation = (OperationResourceInfo)exchange.get(OperationResourceInfo.class
            .getName());

        if (operation == null) {
            return;
        }

        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.size() == 0) {
            return;
        }

        OutputStream out = message.getContent(OutputStream.class);
        
        if (objs.get(0) != null) {
            Object responseObj = objs.get(0);
            if (objs.get(0) instanceof Response) {
                Response response = (Response)responseObj;
                
                message.put(Message.RESPONSE_CODE, response.getStatus());
                message.put(Message.PROTOCOL_HEADERS, response.getMetadata());
                                
                responseObj = response.getEntity();
                if (responseObj == null) {
                    return;
                }
            } 
            
            Class targetType = responseObj.getClass();
            List<MediaType> availableContentTypes = 
                computeAvailableContentTypes(message);  
            
            MessageBodyWriter writer = null;
            for (MediaType type : availableContentTypes) { 
                writer = ProviderFactory.getInstance()
                    .createMessageBodyWriter(targetType, type);
                 
                if (writer != null) {
                    break;
                }
            }
            
            if (writer == null) {
                message.put(Message.RESPONSE_CODE, 406);
                writeResponseErrorMessage(out, 
                                          "NO_MSG_WRITER",
                                          responseObj.getClass().getSimpleName());
                return;
            }
            
            try {
                LOG.fine("Response EntityProvider is: " + writer.getClass().getName());
                MediaType mt = computeFinalContentTypes(availableContentTypes, writer);
                LOG.fine("Response content type is: " + mt.toString());
                message.put(Message.CONTENT_TYPE, mt.toString());
                writer.writeTo(responseObj, mt, null, out);
            } catch (IOException e) {
                e.printStackTrace();
                message.put(Message.RESPONSE_CODE, 500);
                writeResponseErrorMessage(out, "SERIALIZE_ERROR", 
                                          responseObj.getClass().getSimpleName());
            }        
            
        }

    }
    
    private void writeResponseErrorMessage(OutputStream out, String errorString, 
                                           String parameter) {
        try {
            // TODO : make sure this message is picked up from a resource bundle
            out.write(new org.apache.cxf.common.i18n.Message(errorString,
                                                             BUNDLE,
                                                             parameter
                                                             ).toString().getBytes("UTF-8"));
        } catch (IOException another) {
            // ignore
        }
    }
    
    private List<MediaType> computeAvailableContentTypes(Message message) {
        Exchange exchange = message.getExchange();
        
        List<MediaType> methodMimeTypes = exchange.get(OperationResourceInfo.class).getProduceTypes();
        String acceptContentTypes = (String)exchange.get(Message.ACCEPT_CONTENT_TYPE);
        
        List<MediaType> acceptValues = JAXRSUtils.parseMediaTypes(acceptContentTypes);
        
        return JAXRSUtils.intersectMimeTypes(methodMimeTypes, acceptValues);        
    }
    
    private MediaType computeFinalContentTypes(List<MediaType> produceContentTypes, 
                                               MessageBodyWriter provider) {
        List<MediaType> providerMimeTypes = 
            JAXRSUtils.getProduceTypes(provider.getClass().getAnnotation(ProduceMime.class));
                
        List<MediaType> list = 
            JAXRSUtils.intersectMimeTypes(produceContentTypes, providerMimeTypes);
      
        return list.get(0);      
    }
}
