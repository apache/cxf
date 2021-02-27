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

package org.apache.cxf.jaxws.handler.javaee;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;
import javax.xml.ws.handler.Handler;

import org.w3c.dom.Element;

import org.apache.cxf.jaxws.handler.internal.BaseHandlerChainBuilder;

@SuppressWarnings("rawtypes")
public class JavaeeHandlerChainBuilder extends BaseHandlerChainBuilder {
    public static final String JAVAEE_NS = "http://java.sun.com/xml/ns/javaee";
    
    public JavaeeHandlerChainBuilder(ResourceBundle bundle, URL handlerFileURL, 
            HandlerChainBuilderDelegate delegate) {
        super(bundle, handlerFileURL, delegate);
    }
    
    public List<Handler> build(Element el, QName portQName, QName serviceQName, String bindingID) {
        return build(JAVAEE_NS, el, portQName, serviceQName, bindingID);
    }
}
