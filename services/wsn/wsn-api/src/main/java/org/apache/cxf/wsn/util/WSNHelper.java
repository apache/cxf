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
package org.apache.cxf.wsn.util;

import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMResult;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Service;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.wsn.wsdl.WSNWSDLLocator;

public abstract class WSNHelper {
    private static boolean setClassLoader = true;
    public static boolean setClassLoader() {
        return setClassLoader;
    }
    public static void setClassLoader(boolean cl) {
        setClassLoader = cl;
    }
    
    public static <T> T getPort(EndpointReference ref, Class<T> serviceInterface) {
        if (!(ref instanceof W3CEndpointReference)) {
            throw new IllegalArgumentException("Unsupported endpoint reference: " 
                + (ref != null ? ref.toString() : "null"));
        }
        W3CEndpointReference w3cEpr = (W3CEndpointReference) ref;
        String address = getWSAAddress(w3cEpr);
        return getPort(address, serviceInterface);
    }

    public static <T> T getPort(String address, Class<T> serviceInterface) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            if (setClassLoader) {
                Thread.currentThread().setContextClassLoader(WSNHelper.class.getClassLoader());
            }
            
            Service service = Service.create(WSNWSDLLocator.getWSDLUrl(),
                                             new QName("http://cxf.apache.org/wsn/jaxws", 
                                                       serviceInterface.getSimpleName() + "Service"));
            return service.getPort(createWSA(address), serviceInterface);
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public static W3CEndpointReference createWSA(String address) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            if (setClassLoader) {
                Thread.currentThread().setContextClassLoader(WSNHelper.class.getClassLoader());
            }
            
            return new W3CEndpointReferenceBuilder().address(address).build();
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }
    }

    public static String getWSAAddress(W3CEndpointReference ref) {
        Element element = DOMUtils.createDocument().createElement("elem");
        ref.writeTo(new DOMResult(element));
        NodeList nl = element.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "Address");
        if (nl != null && nl.getLength() > 0) {
            Element e = (Element) nl.item(0);
            return DOMUtils.getContent(e).trim();
        }
        return null;
    }


}
