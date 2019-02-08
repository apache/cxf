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
package org.apache.cxf.transport.http_undertow.spring;



import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.BusWiringBeanFactoryPostProcessor;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.jsse.TLSServerParametersConfig;
import org.apache.cxf.configuration.security.CertificateConstraintsType;
import org.apache.cxf.configuration.security.CipherSuites;
import org.apache.cxf.configuration.security.ClientAuthentication;
import org.apache.cxf.configuration.security.ExcludeProtocols;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.configuration.security.IncludeProtocols;
import org.apache.cxf.configuration.security.KeyManagersType;
import org.apache.cxf.configuration.security.SecureRandomParameters;
import org.apache.cxf.configuration.security.TLSServerParametersType;
import org.apache.cxf.configuration.security.TrustManagersType;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.http_undertow.ThreadingParameters;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngine;
import org.apache.cxf.transport.http_undertow.UndertowHTTPServerEngineFactory;
import org.apache.cxf.transports.http_undertow.configuration.TLSServerParametersIdentifiedType;
import org.apache.cxf.transports.http_undertow.configuration.ThreadingParametersIdentifiedType;
import org.apache.cxf.transports.http_undertow.configuration.ThreadingParametersType;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;





public class UndertowHTTPServerEngineBeanDefinitionParser extends AbstractBeanDefinitionParser {
    private static final String SECURITY_NS =
        "http://cxf.apache.org/configuration/security";

    public void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {

        String portStr = element.getAttribute("port");
        bean.addPropertyValue("port", portStr);

        String hostStr = element.getAttribute("host");
        if (hostStr != null && !"".equals(hostStr.trim())) {
            bean.addPropertyValue("host", hostStr);
        }

        String continuationsStr = element.getAttribute("continuationsEnabled");
        if (continuationsStr != null && continuationsStr.length() > 0) {
            bean.addPropertyValue("continuationsEnabled", continuationsStr);
        }

        String maxIdleTimeStr = element.getAttribute("maxIdleTime");
        if (maxIdleTimeStr != null && !"".equals(maxIdleTimeStr.trim())) {
            bean.addPropertyValue("maxIdleTime", maxIdleTimeStr);
        }


        ValueHolder busValue = ctx.getContainingBeanDefinition()
            .getConstructorArgumentValues().getArgumentValue(0, Bus.class);
        bean.addPropertyValue("bus", busValue.getValue());
        try {
            Element elem = DOMUtils.getFirstElement(element);
            while (elem != null) {
                String name = elem.getLocalName();
                if ("tlsServerParameters".equals(name)) {
                    mapTLSServerParameters(elem, bean);
                } else if ("threadingParameters".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "threadingParameters",
                                                    ThreadingParametersType.class,
                                                    UndertowHTTPServerEngineBeanDefinitionParser.class,
                                                    "createThreadingParameters");
                } else if ("tlsServerParametersRef".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "tlsServerParametersRef",
                                                    TLSServerParametersIdentifiedType.class,
                                                    UndertowHTTPServerEngineBeanDefinitionParser.class,
                                                    "createTLSServerParametersConfigRef");
                } else if ("threadingParametersRef".equals(name)) {
                    mapElementToJaxbPropertyFactory(elem,
                                                    bean,
                                                    "threadingParametersRef",
                                                    ThreadingParametersIdentifiedType.class,
                                                    UndertowHTTPServerEngineBeanDefinitionParser.class,
                                                    "createThreadingParametersRef"
                                                    );
                } else if ("handlers".equals(name)) {
                    List<?> handlers =
                        ctx.getDelegate().parseListElement(elem, bean.getBeanDefinition());
                    bean.addPropertyValue("handlers", handlers);
                }
                elem = org.apache.cxf.helpers.DOMUtils.getNextElement(elem);
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }

        bean.setLazyInit(false);
    }

    private void mapTLSServerParameters(Element e, BeanDefinitionBuilder bean) {
        BeanDefinitionBuilder paramsbean
            = BeanDefinitionBuilder.rootBeanDefinition(TLSServerParametersConfig.TLSServerParametersTypeInternal.class);

        // read the attributes
        NamedNodeMap as = e.getAttributes();
        for (int i = 0; i < as.getLength(); i++) {
            Attr a = (Attr) as.item(i);
            if (a.getNamespaceURI() == null) {
                String aname = a.getLocalName();
                if ("jsseProvider".equals(aname)
                    || "secureSocketProtocol".equals(aname)) {
                    paramsbean.addPropertyValue(aname, a.getValue());
                }
            }
        }

        // read the child elements
        Node n = e.getFirstChild();
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
                    paramsbean.addPropertyReference("keyManagersRef", ref);
                } else {
                    mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                             KeyManagersType.class);
                }
            } else if ("trustManagers".equals(ename)) {
                if (ref != null && ref.length() > 0) {
                    paramsbean.addPropertyReference("trustManagersRef", ref);
                } else {
                    mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                             TrustManagersType.class);
                }
            } else if ("cipherSuites".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         CipherSuites.class);
            } else if ("cipherSuitesFilter".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         FiltersType.class);
            } else if ("excludeProtocols".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         ExcludeProtocols.class);
            } else if ("includeProtocols".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         IncludeProtocols.class);
            } else if ("secureRandomParameters".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         SecureRandomParameters.class);
            } else if ("clientAuthentication".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         ClientAuthentication.class);
            } else if ("certConstraints".equals(ename)) {
                mapElementToJaxbProperty((Element)n, paramsbean, ename,
                                         CertificateConstraintsType.class);
            } else if ("certAlias".equals(ename)) {
                paramsbean.addPropertyValue(ename, n.getTextContent());
            }
            n = n.getNextSibling();
        }

        BeanDefinitionBuilder jaxbbean
            = BeanDefinitionBuilder.rootBeanDefinition(TLSServerParametersConfig.class);
        jaxbbean.addConstructorArgValue(paramsbean.getBeanDefinition());
        bean.addPropertyValue("tlsServerParameters", jaxbbean.getBeanDefinition());
    }

    private static ThreadingParameters toThreadingParameters(
                                    ThreadingParametersType paramtype) {
        ThreadingParameters params = new ThreadingParameters();
        if (paramtype.getMaxThreads() != null) {
            params.setMaxThreads(paramtype.getMaxThreads());
        }
        if (paramtype.getMinThreads() != null) {
            params.setMinThreads(paramtype.getMinThreads());
        }
        if (paramtype.getWorkerIOThreads() != null) {
            params.setWorkerIOThreads(paramtype.getWorkerIOThreads());
        }

        return params;
    }


    /*
     * We do not require an id from the configuration.
     *
     * (non-Javadoc)
     * @see org.springframework.beans.factory.xml.AbstractBeanDefinitionParser#shouldGenerateId()
     */
    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    @Override
    protected Class<?> getBeanClass(Element arg0) {
        return SpringUndertowHTTPServerEngine.class;
    }

    @NoJSR250Annotations
    public static class SpringUndertowHTTPServerEngine extends UndertowHTTPServerEngine
        implements ApplicationContextAware, InitializingBean {

        String threadingRef;
        String tlsRef;
        Bus bus;
        UndertowHTTPServerEngineFactory factory;

        public SpringUndertowHTTPServerEngine(
            UndertowHTTPServerEngineFactory fac,
            Bus b,
            String host,
            int port) {
            super(host, port);
            bus = b;
            factory = fac;
        }

        public SpringUndertowHTTPServerEngine() {
            super();
        }

        public void setBus(Bus b) {
            bus = b;
            if (null != bus && null == factory) {
                factory = bus.getExtension(UndertowHTTPServerEngineFactory.class);
            }
        }

        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            if (bus == null) {
                bus = BusWiringBeanFactoryPostProcessor.addDefaultBus(ctx);
            }
        }

        public void setThreadingParametersRef(String s) {
            threadingRef = s;
        }
        public void setTlsServerParametersRef(String s) {
            tlsRef = s;
        }

        @PostConstruct
        public void finalizeConfig()
            throws GeneralSecurityException,
                   IOException {
            if (tlsRef != null || threadingRef != null) {

                if (threadingRef != null) {
                    setThreadingParameters(factory.getThreadingParametersMap().get(threadingRef));
                }
                if (tlsRef != null) {
                    setTlsServerParameters(factory.getTlsServerParametersMap().get(tlsRef));
                }
            }
            super.finalizeConfig();
        }

        public void afterPropertiesSet() throws Exception {
            finalizeConfig();
        }

    }



    public static TLSServerParametersConfig createTLSServerParametersConfig(String s,
                                                                            JAXBContext context)
        throws GeneralSecurityException, IOException {

        TLSServerParametersType parametersType = unmarshalFactoryString(s, context,
                                                                        TLSServerParametersType.class);

        return new TLSServerParametersConfig(parametersType);
    }
    public static String createTLSServerParametersConfigRef(String s, JAXBContext context)

        throws GeneralSecurityException, IOException {

        TLSServerParametersIdentifiedType parameterTypeRef
            = unmarshalFactoryString(s, context, TLSServerParametersIdentifiedType.class);

        return parameterTypeRef.getId();
    }

    public static ThreadingParameters createThreadingParameters(String s, JAXBContext context) {

        ThreadingParametersType parametersType = unmarshalFactoryString(s, context,
                                                                        ThreadingParametersType.class);

        return toThreadingParameters(parametersType);
    }
    public static String createThreadingParametersRef(String s, JAXBContext context) {
        ThreadingParametersIdentifiedType parametersType
            = unmarshalFactoryString(s, context, ThreadingParametersIdentifiedType.class);
        return parametersType.getId();
    }
}
