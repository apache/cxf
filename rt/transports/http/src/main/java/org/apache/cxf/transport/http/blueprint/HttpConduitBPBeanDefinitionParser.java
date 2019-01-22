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

package org.apache.cxf.transport.http.blueprint;


import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.configuration.jsse.TLSClientParametersConfig;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.CipherSuites;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.configuration.security.KeyManagersType;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.configuration.security.SecureRandomParameters;
import org.apache.cxf.configuration.security.TLSClientParametersType;
import org.apache.cxf.configuration.security.TrustManagersType;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.Metadata;

public class HttpConduitBPBeanDefinitionParser extends AbstractBPBeanDefinitionParser {
    private static final String SECURITY_NS =
        "http://cxf.apache.org/configuration/security";

    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);

        bean.setRuntimeClass(HTTPConduit.class);

        parseAttributes(element, context, bean);
        parseChildElements(element, context, bean);

        bean.setScope(BeanMetadata.SCOPE_PROTOTYPE);

        return bean;
    }


    @Override
    protected void processNameAttribute(Element element, ParserContext context, MutableBeanMetadata bean,
                                        String val) {
        bean.setId(val);
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("tlsClientParameters".equals(name)) {
            mapTLSClientParameters(ctx, bean, el);
        } else if ("trustDecider".equals(name)) {
            mapBeanOrClassElement(ctx, bean, el, MessageTrustDecider.class);
        } else if ("authSupplier".equals(name)) {
            mapBeanOrClassElement(ctx, bean, el, HttpAuthSupplier.class);
        } else if ("client".equals(name)) {
            mapElementToJaxbProperty(ctx, bean, el, name,
                                     HTTPClientPolicy.class);
        } else if ("proxyAuthorization".equals(name)) {
            mapElementToJaxbProperty(ctx, bean, el, name,
                                     ProxyAuthorizationPolicy.class);
        } else if ("authorization".equals(name)) {
            mapElementToJaxbProperty(ctx, bean, el, name,
                                     AuthorizationPolicy.class);
        }
    }

    private void mapTLSClientParameters(ParserContext ctx, MutableBeanMetadata bean, Element el) {
        MutableBeanMetadata paramsbean = ctx.createMetadata(MutableBeanMetadata.class);
        paramsbean.setRuntimeClass(TLSClientParametersConfig.TLSClientParametersTypeInternal.class);

        // read the attributes
        NamedNodeMap as = el.getAttributes();
        for (int i = 0; i < as.getLength(); i++) {
            Attr a = (Attr) as.item(i);
            if (a.getNamespaceURI() == null) {
                String aname = a.getLocalName();
                if ("useHttpsURLConnectionDefaultSslSocketFactory".equals(aname)
                    || "useHttpsURLConnectionDefaultHostnameVerifier".equals(aname)
                    || "disableCNCheck".equals(aname)
                    || "enableRevocation".equals(aname)
                    || "jsseProvider".equals(aname)
                    || "secureSocketProtocol".equals(aname)
                    || "sslCacheTimeout".equals(aname)) {
                    paramsbean.addProperty(aname, createValue(ctx, a.getValue()));
                }
            }
        }

        // read the child elements
        Node n = el.getFirstChild();
        while (n != null) {
            if (Node.ELEMENT_NODE != n.getNodeType()
                || !SECURITY_NS.equals(n.getNamespaceURI())) {
                n = n.getNextSibling();
                continue;
            }
            String ename = n.getLocalName();
            // Schema should require that no more than one each of these exist.
            String ref = ((Element)n).getAttribute("ref");

            if ("keyManagers".equals(ename)) {
                if (ref != null && ref.length() > 0) {
                    paramsbean.addProperty("keyManagersRef", createRef(ctx, ref));
                } else {
                    mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                             KeyManagersType.class);
                }
            } else if ("trustManagers".equals(ename)) {
                if (ref != null && ref.length() > 0) {
                    paramsbean.addProperty("trustManagersRef", createRef(ctx, ref));
                } else {
                    mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                             TrustManagersType.class);
                }
            } else if ("cipherSuites".equals(ename)) {
                mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                         CipherSuites.class);
            } else if ("cipherSuitesFilter".equals(ename)) {
                mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                         FiltersType.class);
            } else if ("secureRandomParameters".equals(ename)) {
                mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                         SecureRandomParameters.class);
            } else if ("certConstraints".equals(ename)) {
                mapElementToJaxbProperty(ctx, paramsbean, (Element)n, ename,
                                         CertificateConstraintsType.class);
            } else if ("certAlias".equals(ename)) {
                paramsbean.addProperty(ename, createValue(ctx, n.getTextContent()));
            }
            n = n.getNextSibling();
        }

        MutableBeanMetadata jaxbbean = ctx.createMetadata(MutableBeanMetadata.class);
        jaxbbean.setRuntimeClass(TLSClientParametersConfig.class);
        jaxbbean.setFactoryMethod("createTLSClientParametersFromType");
        jaxbbean.addArgument(paramsbean, TLSClientParametersType.class.getName(), 0);
        bean.addProperty("tlsClientParameters", jaxbbean);
    }

    private void mapBeanOrClassElement(ParserContext ctx, MutableBeanMetadata bean, Element el,
                                       Class<?> cls) {
        String elementName = el.getLocalName();
        String classProperty = el.getAttribute("class");
        String beanref = el.getAttribute("bean");
        if (classProperty != null && !classProperty.isEmpty()) {
            bean.addProperty(elementName, createObjectOfClass(ctx, classProperty));
        } else if (beanref != null && !beanref.isEmpty()) {
            bean.addProperty(elementName, createRef(ctx, beanref));
        }
    }


}
