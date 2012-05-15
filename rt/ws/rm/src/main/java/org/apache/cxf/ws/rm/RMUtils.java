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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.io.WriteOnCloseOutputStream;
import org.apache.cxf.management.ManagementConstants;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.addressing.AddressingConstants;
import org.apache.cxf.ws.addressing.AddressingConstantsImpl;

public final class RMUtils {
   
    private static final org.apache.cxf.ws.rm.v200702.ObjectFactory WSRM_FACTORY;
    private static final org.apache.cxf.ws.rm.v200502.ObjectFactory WSRM200502_FACTORY;
    private static final org.apache.cxf.ws.rm.v200502wsa15.ObjectFactory WSRM200502_WSA200508_FACTORY;
    private static final AddressingConstants WSA_CONSTANTS;
    private static final Pattern GENERATED_BUS_ID_PATTERN = Pattern.compile(Bus.DEFAULT_BUS_ID + "\\d+$");
    
    static {
        WSRM_FACTORY = new org.apache.cxf.ws.rm.v200702.ObjectFactory();        
        WSRM200502_FACTORY = new org.apache.cxf.ws.rm.v200502.ObjectFactory();
        WSRM200502_WSA200508_FACTORY = new org.apache.cxf.ws.rm.v200502wsa15.ObjectFactory();
        WSA_CONSTANTS = new AddressingConstantsImpl();
    }
    
    private RMUtils() {        
    }
    
    /**
     * Get the factory for the internal representation of WS-RM data (WS-ReliableMessaging 1.1).
     * 
     * @return factory
     */
    public static org.apache.cxf.ws.rm.v200702.ObjectFactory getWSRMFactory() {
        return WSRM_FACTORY;
    }
    
    /**
     * Get the factory for WS-ReliableMessaging 1.0 using the standard 200408 WS-Addressing namespace.
     * 
     * @return factory
     */
    public static org.apache.cxf.ws.rm.v200502.ObjectFactory getWSRM200502Factory() {
        return WSRM200502_FACTORY;
    }
    
    /**
     * Get the factory for WS-ReliableMessaging 1.0 using the current 200508 WS-Addressing namespace.
     * 
     * @return factory
     */
    public static org.apache.cxf.ws.rm.v200502wsa15.ObjectFactory getWSRM200502WSA200508Factory() {
        return WSRM200502_WSA200508_FACTORY;
    }
    
    /**
     * Get the constants for a particular WS-ReliableMessaging namespace.
     * 
     * @param uri
     * @return constants
     */
    public static RMConstants getConstants(String uri) {
        if (RM10Constants.NAMESPACE_URI.equals(uri)) {
            return RM10Constants.INSTANCE;
        } else if (RM11Constants.NAMESPACE_URI.equals(uri)) {
            return RM11Constants.INSTANCE;
        } else {
            return null;
        }
    }
    
    public static AddressingConstants getAddressingConstants() {
        return WSA_CONSTANTS;
    }

    public static org.apache.cxf.ws.addressing.EndpointReferenceType createAnonymousReference() {
        return createReference(org.apache.cxf.ws.addressing.Names.WSA_ANONYMOUS_ADDRESS);
    }
    
    public static org.apache.cxf.ws.addressing.EndpointReferenceType createNoneReference() {
        return createReference(org.apache.cxf.ws.addressing.Names.WSA_NONE_ADDRESS);
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
    
    public static String getEndpointIdentifier(Endpoint endpoint) {
        return getEndpointIdentifier(endpoint, null);
    }

    public static String getEndpointIdentifier(Endpoint endpoint, Bus bus) {
        String busId = null;
        if (bus == null) {
            busId = Bus.DEFAULT_BUS_ID;
        } else {
            busId = bus.getId();
            // bus-ids of form cxfnnn or artifactid-cxfnnn must drop the variable part nnn
            Matcher m = GENERATED_BUS_ID_PATTERN.matcher(busId);
            if (m.find()) {
                busId = busId.substring(0, m.start() + Bus.DEFAULT_BUS_ID.length());
            }
        }
        return endpoint.getEndpointInfo().getService().getName() + "."
            + endpoint.getEndpointInfo().getName() + "@" + busId;
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
    
    public static ObjectName getManagedObjectName(RMManager manager) throws JMException {
        StringBuilder buffer = new StringBuilder();
        writeTypeProperty(buffer, manager.getBus(), "WSRM.Manager");
        return new ObjectName(buffer.toString());
    }
    
    public static ObjectName getManagedObjectName(RMEndpoint endpoint) throws JMException {
        StringBuilder buffer = new StringBuilder();
        writeTypeProperty(buffer, endpoint.getManager().getBus(), "WSRM.Endpoint");
        Endpoint ep = endpoint.getApplicationEndpoint();
        writeEndpointProperty(buffer, ep);
        return new ObjectName(buffer.toString());
    }
    
    public static ObjectName getManagedObjectName(RMManager manager, Endpoint ep) throws JMException {
        StringBuilder buffer = new StringBuilder();
        writeTypeProperty(buffer, manager.getBus(), "WSRM.Endpoint");
        writeEndpointProperty(buffer, ep);
        return new ObjectName(buffer.toString());
    }

    private static void writeTypeProperty(StringBuilder buffer, Bus bus, String type) {
        String busId = bus.getId();
        buffer.append(ManagementConstants.DEFAULT_DOMAIN_NAME + ":");
        buffer.append(ManagementConstants.BUS_ID_PROP + "=" + busId + ",");
        buffer.append(ManagementConstants.TYPE_PROP + "=" + type);
    }

    private static void writeEndpointProperty(StringBuilder buffer, Endpoint ep) {
        String serviceName = ObjectName.quote(ep.getService().getName().toString());
        buffer.append(",");
        buffer.append(ManagementConstants.SERVICE_NAME_PROP + "=" + serviceName + ",");
        String endpointName = ObjectName.quote(ep.getEndpointInfo().getName().toString());
        buffer.append(ManagementConstants.PORT_NAME_PROP + "=" + endpointName);
    }
}
