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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import jakarta.xml.ws.WebServiceException;
import jakarta.xml.ws.handler.Handler;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.staxutils.StaxUtils;

@SuppressWarnings("rawtypes")
abstract class BaseHandlerChainBuilder {
    private static final String HANDLER_CHAINS_E = "handler-chains";
    private static final String HANDLER_CHAIN_E = "handler-chain";
    private final ResourceBundle bundle;
    private final URL handlerFileURL;

    protected BaseHandlerChainBuilder(ResourceBundle bundle, URL handlerFileURL) {
        this.bundle = bundle;
        this.handlerFileURL = handlerFileURL;
    }

    protected List<Handler> build(String namespace, Element el, QName portQName, QName serviceQName, String bindingID) {
        if (!HANDLER_CHAINS_E.equals(el.getLocalName())) {
            String xml = StaxUtils.toString(el);
            throw new WebServiceException(
                    BundleUtils.getFormattedString(bundle,
                                                   "NOT_VALID_ROOT_ELEMENT",
                                                   namespace.equals(el.getNamespaceURI()),
                                                   HANDLER_CHAINS_E.equals(el.getLocalName()),
                                                   xml, handlerFileURL));
        }

        List<Handler> chain = new ArrayList<>();
        Node node = el.getFirstChild();
        while (node != null) {
            if (node instanceof Element) {
                el = (Element)node;

                if (!namespace.equals(el.getNamespaceURI())) {
                    throw new WebServiceException(
                            BundleUtils.getFormattedString(bundle,
                                                           "NOT_VALID_NAMESPACE",
                                                           el.getNamespaceURI()));
                }

                if (!HANDLER_CHAIN_E.equals(el.getLocalName())) {
                    String xml = StaxUtils.toString(el);
                    throw new WebServiceException(
                            BundleUtils.getFormattedString(bundle,
                                                           "NOT_VALID_ELEMENT_IN_HANDLER",
                                                           xml));
                }
                processHandlerChainElement(namespace, el, chain,
                                           portQName, serviceQName, bindingID);
            }
            node = node.getNextSibling();
        }

        return chain;
    }

    private void processHandlerChainElement(String namespace, Element el, List<Handler> chain,
                                            QName portQName, QName serviceQName, String bindingID) {
        Node node = el.getFirstChild();
        while (node != null) {
            Node cur = node;
            node = node.getNextSibling();
            if (cur instanceof Element) {
                el = (Element)cur;
                if (!namespace.equals(el.getNamespaceURI())) {
                    String xml = StaxUtils.toString(el);
                    throw new WebServiceException(
                            BundleUtils.getFormattedString(bundle,
                                                           "NOT_VALID_ELEMENT_IN_HANDLER",
                                                           xml));
                }
                String name = el.getLocalName();
                if ("port-name-pattern".equals(name)) {
                    if (!patternMatches(el, portQName)) {
                        return;
                    }
                } else if ("service-name-pattern".equals(name)) {
                    if (!patternMatches(el, serviceQName)) {
                        return;
                    }
                } else if ("protocol-bindings".equals(name)) {
                    if (!protocolMatches(el, bindingID)) {
                        return;
                    }
                } else if ("handler".equals(name)) {
                    processHandlerElement(el, chain);
                }
            }
        }
    }

    private boolean protocolMatches(Element el, String id) {
        if (id == null) {
            return true;
        }
        String name = el.getTextContent().trim();
        StringTokenizer st = new StringTokenizer(name, " ", false);
        boolean result = false;
        while (st.hasMoreTokens() && !result) {
            result = result || singleProtocolMatches(st.nextToken(), id);
        }
        return result;
    }
    private boolean singleProtocolMatches(String name, String id) {
        if ("##SOAP11_HTTP".equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http".contains(id)
                   || "http://schemas.xmlsoap.org/soap/".contains(id);
        } else if ("##SOAP11_HTTP_MTOM".equals(name)) {
            return "http://schemas.xmlsoap.org/wsdl/soap/http?mtom=true".contains(id)
                   || "http://schemas.xmlsoap.org/soap/?mtom=true".contains(id);
        } else if ("##SOAP12_HTTP".equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/".contains(id)
                   || "http://schemas.xmlsoap.org/wsdl/soap12/".contains(id);
        } else if ("##SOAP12_HTTP_MTOM".equals(name)) {
            return "http://www.w3.org/2003/05/soap/bindings/HTTP/?mtom=true".contains(id)
                   || "http://schemas.xmlsoap.org/wsdl/soap12/?mtom=true".contains(id);
        } else if ("##XML_HTTP".equals(name)) {
            name = "http://www.w3.org/2004/08/wsdl/http";
        }
        return name.contains(id);
    }
    private boolean patternMatches(Element el, QName comp) {
        if (comp == null) {
            return true;
        }
        String namePattern = el.getTextContent().trim();
        if ("*".equals(namePattern)) {
            return true;
        }
        final int idx = namePattern.indexOf(':');
        if (idx < 0) {
            String xml = StaxUtils.toString(el);
            throw new WebServiceException(
                    BundleUtils.getFormattedString(bundle,
                                                   "NOT_A_QNAME_PATTER",
                                                   namePattern, xml));
        }
        String pfx = namePattern.substring(0, idx);
        String ns = el.lookupNamespaceURI(pfx);
        if (ns == null) {
            ns = pfx;
        }
        if (!ns.equals(comp.getNamespaceURI())) {
            return false;
        }
        String localPart = namePattern.substring(idx + 1,
                                                 namePattern.length());
        if (localPart.contains("*")) {
            //wildcard pattern matching
            return Pattern.matches(mapPattern(localPart), comp.getLocalPart());
        } else if (!localPart.equals(comp.getLocalPart())) {
            return false;
        }
        return true;
    }

    private String mapPattern(String s) {
        StringBuilder buf = new StringBuilder(s);
        for (int x = 0; x < buf.length(); x++) {
            switch (buf.charAt(x)) {
            case '*':
                buf.insert(x, '.');
                x++;
                break;
            case '.':
            case '\\':
            case '^':
            case '$':
            case '{':
            case '}':
            case '(':
            case ')':
                buf.insert(x, '\\');
                x++;
                break;
            default:
                //nothing to do
            }
        }
        return buf.toString();
    }

    abstract void processHandlerElement(Element el, List<Handler> chain);

}
