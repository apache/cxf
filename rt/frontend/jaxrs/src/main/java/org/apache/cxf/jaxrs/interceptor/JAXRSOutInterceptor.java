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
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.jaxrs.ext.ResponseHandler;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
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

    public void handleMessage(Message message) {
        
        try {
            processResponse(message);
        } finally {
            ProviderFactory.getInstance().cleatThreadLocalProxies();
            ClassResourceInfo cri =
                (ClassResourceInfo)message.getExchange().get(JAXRSInInterceptor.ROOT_RESOURCE_CLASS);
            if (cri != null) {
                cri.clearThreadLocalProxies();
            }
        }
            

    }
    
    @SuppressWarnings("unchecked")
    private void processResponse(Message message) {
        
        MessageContentsList objs = MessageContentsList.getContentsList(message);
        if (objs == null || objs.size() == 0) {
            return;
        }
        
        if (objs.get(0) != null) {
            Object responseObj = objs.get(0);
            Response response = null;
            if (objs.get(0) instanceof Response) {
                response = (Response)responseObj;
            } else {    
                response = Response.ok(responseObj).build();
            }
            
            Exchange exchange = message.getExchange();
            OperationResourceInfo operation = (OperationResourceInfo)exchange.get(OperationResourceInfo.class
                .getName());

            List<ProviderInfo<ResponseHandler>> handlers = 
                ProviderFactory.getInstance().getResponseHandlers();
            for (ProviderInfo<ResponseHandler> rh : handlers) {
                Response r = rh.getProvider().handleResponse(message, operation, response);
                if (r != null) {
                    response = r;
                }
            }
            
            message.put(Message.RESPONSE_CODE, response.getStatus());
            message.put(Message.PROTOCOL_HEADERS, response.getMetadata());
                            
            responseObj = response.getEntity();
            if (responseObj == null) {
                return;
            }
            
            Class targetType = responseObj.getClass();
            List<MediaType> availableContentTypes = 
                computeAvailableContentTypes(message, response);  
            
            Method invoked = operation == null ? null : operation.getMethodToInvoke();
            
            MessageBodyWriter writer = null;
            MediaType responseType = null;
            for (MediaType type : availableContentTypes) { 
                writer = ProviderFactory.getInstance()
                    .createMessageBodyWriter(targetType, 
                          invoked != null ? invoked.getGenericReturnType() : null, 
                          invoked != null ? invoked.getAnnotations() : new Annotation[]{}, 
                          type,
                          exchange.getInMessage());
                
                if (writer != null) {
                    responseType = type;
                    break;
                }
            }
        
            OutputStream out = message.getContent(OutputStream.class);
            if (writer == null) {
                message.put(Message.RESPONSE_CODE, 406);
                writeResponseErrorMessage(out, 
                      "NO_MSG_WRITER",
                      invoked != null ? invoked.getReturnType().getSimpleName() : "");
                return;
            }
            
            try {
                
                responseType = checkFinalContentType(responseType);
                LOG.fine("Response content type is: " + responseType.toString());
                message.put(Message.CONTENT_TYPE, responseType.toString());
                
                LOG.fine("Response EntityProvider is: " + writer.getClass().getName());
                writer.writeTo(responseObj, targetType, invoked.getGenericReturnType(), 
                               invoked != null ? invoked.getAnnotations() : new Annotation[]{}, 
                               responseType, 
                               response.getMetadata(), 
                               out);
                
            } catch (IOException e) {
                e.printStackTrace();
                message.put(Message.RESPONSE_CODE, 500);
                writeResponseErrorMessage(out, "SERIALIZE_ERROR", 
                                          responseObj.getClass().getSimpleName());
            }        
            
        } else {
            message.put(Message.RESPONSE_CODE, 204);
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
    
    @SuppressWarnings("unchecked")
    private List<MediaType> computeAvailableContentTypes(Message message, Response response) {
        
        Object contentType = 
            response.getMetadata().getFirst(HttpHeaders.CONTENT_TYPE);
        Exchange exchange = message.getExchange();
        List<MediaType> produceTypes = null;
        OperationResourceInfo operation = exchange.get(OperationResourceInfo.class);
        if (contentType != null) {
            produceTypes = Collections.singletonList(MediaType.valueOf(contentType.toString()));
        } else if (operation != null) {
            produceTypes = operation.getProduceTypes();
        } else {
            produceTypes = Collections.singletonList(MediaType.APPLICATION_OCTET_STREAM_TYPE);
        }
        List<MediaType> acceptContentTypes = 
            (List<MediaType>)exchange.get(Message.ACCEPT_CONTENT_TYPE);
        
        return JAXRSUtils.intersectMimeTypes(acceptContentTypes, produceTypes);
        
    }
    
    private MediaType checkFinalContentType(MediaType mt) {
        if (mt.isWildcardType() && mt.isWildcardSubtype()) {
            return MediaType.APPLICATION_OCTET_STREAM_TYPE;
        } else {
            return mt;
        }
    }
}
