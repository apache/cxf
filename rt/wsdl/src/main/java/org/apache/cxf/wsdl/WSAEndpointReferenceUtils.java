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

package org.apache.cxf.wsdl;

import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.MetadataType;

/**
 * Provides utility methods for obtaining endpoint references, wsdl definitions, etc.
 */
public final class WSAEndpointReferenceUtils {

    public static final String ANONYMOUS_ADDRESS = "http://www.w3.org/2005/08/addressing/anonymous";
    static final org.apache.cxf.ws.addressing.ObjectFactory WSA_OBJECT_FACTORY =
        new org.apache.cxf.ws.addressing.ObjectFactory();


    private WSAEndpointReferenceUtils() {
        // Utility class - never constructed
    }

    public static EndpointReferenceType createEndpointReferenceWithMetadata() {
        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setMetadata(WSA_OBJECT_FACTORY.createMetadataType());
        return reference;
    }

    public static MetadataType getSetMetadata(EndpointReferenceType ref) {
        MetadataType mt = ref.getMetadata();
        if (null == mt) {
            mt = WSA_OBJECT_FACTORY.createMetadataType();
            ref.setMetadata(mt);
        }
        return mt;
    }


    /**
     * Set the address of the provided endpoint reference.
     * @param ref - the endpoint reference
     * @param address - the address
     */
    public static void setAddress(EndpointReferenceType ref, String address) {
        AttributedURIType a = WSA_OBJECT_FACTORY.createAttributedURIType();
        a.setValue(address);
        ref.setAddress(a);
    }

    /**
     * Get the address from the provided endpoint reference.
     * @param ref - the endpoint reference
     * @return String the address of the endpoint
     */
    public static String getAddress(EndpointReferenceType ref) {
        AttributedURIType a = ref.getAddress();

        if (null != a) {
            return a.getValue();
        }
        // should wsdl be parsed for an address now?
        return null;
    }

    /**
     * Create a duplicate endpoint reference sharing all atributes
     * @param ref the reference to duplicate
     * @return EndpointReferenceType - the duplicate endpoint reference
     */
    public static EndpointReferenceType duplicate(EndpointReferenceType ref) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setMetadata(ref.getMetadata());
        reference.getAny().addAll(ref.getAny());
        reference.setAddress(ref.getAddress());
        return reference;
    }

    /**
     * Create an endpoint reference for the provided address.
     * @param address - address URI
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getEndpointReference(String address) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        setAddress(reference, address);
        return reference;
    }

    public static EndpointReferenceType getEndpointReference(AttributedURIType address) {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        reference.setAddress(address);
        return reference;
    }

    /**
     * Create an anonymous endpoint reference.
     * @return EndpointReferenceType - the endpoint reference
     */
    public static EndpointReferenceType getAnonymousEndpointReference() {

        EndpointReferenceType reference = WSA_OBJECT_FACTORY.createEndpointReferenceType();
        setAddress(reference, ANONYMOUS_ADDRESS);
        return reference;
    }

}


