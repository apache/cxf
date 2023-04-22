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

package org.apache.cxf.jaxws.handler;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.jaxws.handler.types.PortComponentHandlerType;

@SuppressWarnings("rawtypes")
final class JavaeeHandlerChainBuilder extends BaseHandlerChainBuilder {
    static final String JAVAEE_NS = "http://java.sun.com/xml/ns/javaee";
    private static JAXBContext context;
    private final DelegatingHandlerChainBuilder delegate;

    JavaeeHandlerChainBuilder(ResourceBundle bundle, URL handlerFileURL, DelegatingHandlerChainBuilder delegate) {
        super(bundle, handlerFileURL);
        this.delegate = delegate;
    }

    public List<Handler> build(Element el, QName portQName, QName serviceQName, String bindingID) {
        return build(JAVAEE_NS, el, portQName, serviceQName, bindingID);
    }

    void processHandlerElement(Element el, List<Handler> chain) {
        try {
            JAXBContext ctx = getContextForPortComponentHandlerType();
            PortComponentHandlerType pt = JAXBUtils.unmarshall(ctx, el, PortComponentHandlerType.class).getValue();
            chain.addAll(delegate.buildHandlerChain(pt));
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Could not unmarshal handler chain", e);
        }
    }

    private static synchronized JAXBContext getContextForPortComponentHandlerType()
            throws JAXBException {
        if (context == null) {
            context = JAXBContext.newInstance(PortComponentHandlerType.class);
        }
        return context;
    }
}
