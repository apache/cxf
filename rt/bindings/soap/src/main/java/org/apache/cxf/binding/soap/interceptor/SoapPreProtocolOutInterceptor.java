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

package org.apache.cxf.binding.soap.interceptor;

import java.util.HashMap;
import java.util.List;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;

import static org.apache.cxf.message.Message.MIME_HEADERS;

/**
 * This interceptor is responsible for setting up the SOAP version
 * and header, so that this is available to any pre-protocol interceptors
 * that require these to be available.
 */
public class SoapPreProtocolOutInterceptor extends AbstractSoapInterceptor {

    public SoapPreProtocolOutInterceptor() {
        super(Phase.PRE_STREAM);
        getBefore().add(AttachmentOutInterceptor.class.getName());
    }

    /**
     * Mediate a message dispatch.
     * 
     * @param message the current message
     * @throws Fault
     */
    public void handleMessage(SoapMessage message) throws Fault {
        ensureVersion(message);
        ensureMimeHeaders(message);
    }
    
    /**
     * Ensure the SOAP version is set for this message.
     * 
     * @param message the current message
     */
    private void ensureVersion(SoapMessage message) {
        SoapVersion soapVersion = message.getVersion();
        if (soapVersion == null
            && message.getExchange().getInMessage() instanceof SoapMessage) {
            soapVersion = ((SoapMessage)message.getExchange().getInMessage()).getVersion();
            message.setVersion(soapVersion);
        }
        
        if (soapVersion == null) {
            soapVersion = Soap11.getInstance();
            message.setVersion(soapVersion);
        }
    }
    
    /**
     * Ensure the SOAP header is set for this message.
     * 
     * @param message the current message
     */
    private void ensureMimeHeaders(SoapMessage message) {
        if (message.get(MIME_HEADERS) == null) {
            message.put(MIME_HEADERS, new HashMap<String, List<String>>());
        }
    }
}
