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

package org.apache.cxf.ws.rm;

import java.io.OutputStream;
import java.text.MessageFormat;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingConstants;
import org.apache.cxf.ws.addressing.AddressingConstantsImpl;
import org.apache.cxf.ws.addressing.VersionTransformer;

public final class RMUtils {
   
    private static final org.apache.cxf.ws.addressing.v200408.ObjectFactory WSA_FACTORY;
    private static final org.apache.cxf.ws.rm.ObjectFactory WSRM_FACTORY;
    private static final AddressingConstants WSA_CONSTANTS; 
    
    static {
        WSA_FACTORY = new org.apache.cxf.ws.addressing.v200408.ObjectFactory();
        WSRM_FACTORY = new org.apache.cxf.ws.rm.ObjectFactory();        
        WSA_CONSTANTS = new AddressingConstantsImpl();      
    }
    
    protected RMUtils() {        
    }
    
    public static org.apache.cxf.ws.addressing.v200408.ObjectFactory getWSAFactory() {
        return WSA_FACTORY;
    }
    
    public static org.apache.cxf.ws.rm.ObjectFactory getWSRMFactory() {
        return WSRM_FACTORY;
    }
    
    public static AddressingConstants getAddressingConstants() {
        return WSA_CONSTANTS;
    }
    
    public static org.apache.cxf.ws.addressing.EndpointReferenceType createAnonymousReference() {
        return createReference(org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS);
    }
    
    public static org.apache.cxf.ws.addressing.v200408.EndpointReferenceType createAnonymousReference2004() {
        return VersionTransformer.convert(createAnonymousReference());
    }
    
    public static org.apache.cxf.ws.addressing.EndpointReferenceType createNoneReference() {
        return createReference(org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
    }
    
    public static org.apache.cxf.ws.addressing.v200408.EndpointReferenceType createNoneReference2004() {
        return VersionTransformer.convert(createNoneReference());
    }
    
    public static org.apache.cxf.ws.addressing.EndpointReferenceType createReference(String address) {
        org.apache.cxf.ws.addressing.ObjectFactory factory = 
            new org.apache.cxf.ws.addressing.ObjectFactory();
        org.apache.cxf.ws.addressing.EndpointReferenceType epr = factory.createEndpointReferenceType();
        org.apache.cxf.ws.addressing.AttributedURIType uri = factory.createAttributedURIType();
        uri.setValue(address);
        epr.setAddress(uri);        
        return epr;        
    }
    
    public static org.apache.cxf.ws.addressing.v200408.EndpointReferenceType 
    createReference2004(String address) {
        org.apache.cxf.ws.addressing.v200408.ObjectFactory factory = 
            new org.apache.cxf.ws.addressing.v200408.ObjectFactory();
        org.apache.cxf.ws.addressing.v200408.EndpointReferenceType epr = 
            factory.createEndpointReferenceType();
        org.apache.cxf.ws.addressing.v200408.AttributedURI uri = factory.createAttributedURI();
        uri.setValue(address);
        epr.setAddress(uri);
        return epr;
    } 
    
    public static String getEndpointIdentifier(Endpoint endpoint) {
        return MessageFormat.format("{0}.{1}", new Object[] {
            endpoint.getEndpointInfo().getService().getName(),
            endpoint.getEndpointInfo().getName()
        });
    }
    
    public static WriteOnCloseOutputStream createCachedStream(Message message, OutputStream os) {
        // We need to ensure that we have an output stream which won't start writing the 
        // message until we have a chance to send a createsequence
        if (!(os instanceof WriteOnCloseOutputStream)) {
            WriteOnCloseOutputStream cached = new WriteOnCloseOutputStream(os);
            message.setContent(OutputStream.class, cached);
            os = cached;
        }
        return (WriteOnCloseOutputStream) os;
    }
}
