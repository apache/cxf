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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.stream.StreamSource;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.spi.Provider;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import jakarta.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.wsn.wsdl.WSNWSDLLocator;

public class WSNHelper {
    private static volatile WSNHelper instance;
    protected boolean setClassLoader = true;


    public static WSNHelper getInstance() {
        if (instance == null) {
            createInstance();
        }
        return instance;
    }
    public static void clearInstance() {
        instance = null;
    }

    private static synchronized void createInstance() {
        if (instance != null) {
            return;
        }
        Provider p = Provider.provider();
        if (p.getClass().getName().contains("apache.cxf")) {
            instance = new CXFWSNHelper();
        } else {
            instance = new WSNHelper();
        }
    }

    public boolean setClassLoader() {
        return setClassLoader;
    }
    public void setClassLoader(boolean cl) {
        setClassLoader = cl;
    }

    public boolean supportsExtraClasses() {
        return false;
    }

    public Endpoint publish(String address, Object o, Class<?> ... extraClasses) {
        if (extraClasses != null && extraClasses.length > 0) {
            throw new UnsupportedOperationException("Pure JAX-WS does not support the extraClasses");
        }
        Endpoint endpoint = Endpoint.create(o);
        URL wsdlLocation = WSNWSDLLocator.getWSDLUrl();
        if (wsdlLocation != null) {
            try {
                if (endpoint.getProperties() == null) {
                    endpoint.setProperties(new HashMap<String, Object>());
                }
                endpoint.getProperties().put(Message.WSDL_DESCRIPTION, wsdlLocation.toExternalForm());
                List<Source> mt = new ArrayList<>();
                StreamSource src = new StreamSource(wsdlLocation.openStream(), wsdlLocation.toExternalForm());
                mt.add(src);
                endpoint.setMetadata(mt);
            } catch (IOException e) {
                //ignore, no wsdl really needed
            }
        }

        endpoint.publish(address);
        return endpoint;
    }

    public <T> T getPort(EndpointReference ref,
                         Class<T> serviceInterface,
                         Class<?> ... extraClasses) {
        if (!(ref instanceof W3CEndpointReference)) {
            throw new IllegalArgumentException("Unsupported endpoint reference: "
                + (ref != null ? ref.toString() : "null"));
        }
        W3CEndpointReference w3cEpr = (W3CEndpointReference) ref;
        String address = getWSAAddress(w3cEpr);
        return getPort(address, serviceInterface, extraClasses);
    }

    public <T> T getPort(String address,
                         Class<T> serviceInterface,
                         Class<?> ... extraClasses) {
        if (extraClasses != null && extraClasses.length > 0) {
            throw new UnsupportedOperationException("Pure JAX-WS does not support the extraClasses");
        }

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

    public W3CEndpointReference createWSA(String address) {
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

    public String getWSAAddress(W3CEndpointReference ref) {
        Element element = DOMUtils.getEmptyDocument().createElement("elem");
        ref.writeTo(new DOMResult(element));
        NodeList nl = element.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "Address");
        if (nl != null && nl.getLength() > 0) {
            Element e = (Element) nl.item(0);
            return DOMUtils.getContent(e).trim();
        }
        return null;
    }
}
