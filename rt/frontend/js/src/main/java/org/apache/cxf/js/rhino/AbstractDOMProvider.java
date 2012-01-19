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

package org.apache.cxf.js.rhino;


import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Endpoint;

import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.xml.XMLObject;


public abstract class AbstractDOMProvider {
    public static class JSDOMProviderException extends Exception {
        public JSDOMProviderException(String reason) {
            super(reason);
        }
    }

    public static final String NO_WSDL_LOCATION = "no wsdlLocation property found";
    public static final String NO_SERVICE_NAME = "no serviceName property found";
    public static final String NO_PORT_NAME = "no portName property found";
    public static final String NO_TGT_NAMESPACE = "no targetNamespace property found";
    public static final String NO_INVOKE = "no invoke property found";
    public static final String ILLEGAL_INVOKE_TYPE = "invoke property is not a Function type";
    public static final String NO_EP_ADDR = "no endpoint address specified";

    private Scriptable scriptScope;
    private Scriptable webSvcProviderVar;
    private String epAddress;
    private boolean isBaseAddr;
    private boolean isE4X;
    private Function invokeFunc;

    protected AbstractDOMProvider(Scriptable scope,
                                  Scriptable wspVar, String epAddr,
                                  boolean isBase, boolean e4x) {
        scriptScope = scope;
        webSvcProviderVar = wspVar;
        epAddress = epAddr;
        isBaseAddr = isBase;
        isE4X = e4x;
    }

    public void publish() throws Exception {
        String addr = epAddress;
        String wsdlLoc = null;
        String svcNm = null;
        String portNm = null;
        String tgtNmspc = null;
        String binding = null;
        Object obj = webSvcProviderVar.get("wsdlLocation", webSvcProviderVar);
        if (obj == Scriptable.NOT_FOUND) {
            throw new JSDOMProviderException(NO_WSDL_LOCATION);
        }
        if (obj instanceof String) {
            wsdlLoc = (String)obj;
        }
        obj = webSvcProviderVar.get("serviceName", webSvcProviderVar);
        if (obj == Scriptable.NOT_FOUND) {
            throw new JSDOMProviderException(NO_SERVICE_NAME);
        }
        if (obj instanceof String) {
            svcNm = (String)obj;
        }
        obj = webSvcProviderVar.get("portName", webSvcProviderVar);
        if (obj == Scriptable.NOT_FOUND) {
            throw new JSDOMProviderException(NO_PORT_NAME);
        }
        if (obj instanceof String) {
            portNm = (String)obj;
        }
        obj = webSvcProviderVar.get("targetNamespace", webSvcProviderVar);
        if (obj == Scriptable.NOT_FOUND) {
            throw new JSDOMProviderException(NO_TGT_NAMESPACE);
        }
        if (obj instanceof String) {
            tgtNmspc = (String)obj;
        }
        if (addr == null) {
            obj = webSvcProviderVar.get("EndpointAddress", scriptScope);
            if (obj != Scriptable.NOT_FOUND && obj instanceof String) {
                addr = (String)obj;
                isBaseAddr = false;
            }
        }
        if (addr == null) {
            throw new JSDOMProviderException(NO_EP_ADDR);
        }
        if (isBaseAddr) {
            if (addr.endsWith("/")) {
                addr += portNm;
            } else {
                addr = addr + "/" + portNm;
            }
        }
        obj = webSvcProviderVar.get("BindingType", scriptScope);
        if (obj != Scriptable.NOT_FOUND && obj instanceof String) {
            binding = (String)obj;
        }
        obj = webSvcProviderVar.get("invoke", webSvcProviderVar);
        if (obj == Scriptable.NOT_FOUND) {
            throw new JSDOMProviderException(NO_INVOKE);
        }
        if (obj instanceof Function) {
            invokeFunc = (Function)obj;
        } else {
            throw new JSDOMProviderException(ILLEGAL_INVOKE_TYPE);
        }
        
        Bus bus = BusFactory.getThreadDefaultBus();
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setWsdlLocation(wsdlLoc);
        factory.setBindingId(binding); 
        factory.setServiceName(new QName(tgtNmspc, svcNm));
        factory.setEndpointName(new QName(tgtNmspc, portNm));
        Endpoint ep = new EndpointImpl(bus, this, factory);
        ep.publish(addr);
    }

    public DOMSource invoke(DOMSource request) {
        DOMSource response = new DOMSource();
        Context cx = ContextFactory.getGlobal().enterContext();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            Node node = request.getNode();
            Object inDoc = null;
            if (isE4X) {
                try {
                    inDoc = Context.toObject(node, scope);
                    Object[] args = {inDoc};
                    inDoc = cx.newObject(scope, "XML", args);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            } else {
                inDoc = Context.toObject(node, scope);
            }
            Object[] args = {inDoc};
            Object jsResp = invokeFunc.call(cx, scope, scope, args);
            if (jsResp instanceof Wrapper) {
                jsResp = ((Wrapper)jsResp).unwrap();
            }
            if (jsResp instanceof XMLObject) {
                jsResp = org.mozilla.javascript.xmlimpl.XMLLibImpl.toDomNode(jsResp);
            }
            if (jsResp instanceof Node) {
                node = (Node)jsResp;
                response.setNode(node);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            Context.exit();
        }
        return response;
    }
}
