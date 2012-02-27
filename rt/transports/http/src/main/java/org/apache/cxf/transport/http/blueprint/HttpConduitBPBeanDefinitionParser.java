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

import java.io.StringWriter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.configuration.jsse.spring.TLSClientParametersConfig;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.configuration.security.ProxyAuthorizationPolicy;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transport.http.MessageTrustDecider;
import org.apache.cxf.transport.http.auth.HttpAuthSupplier;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.osgi.service.blueprint.reflect.Metadata;

public class HttpConduitBPBeanDefinitionParser extends AbstractBPBeanDefinitionParser {
    private static final String HTTP_NS =
        "http://cxf.apache.org/transports/http/configuration";

    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        
        bean.setRuntimeClass(HTTPConduit.class);

        mapElementToJaxbProperty(context, bean, element,
                new QName(HTTP_NS, "client"), "client", HTTPClientPolicy.class);
        mapElementToJaxbProperty(context, bean, element,
                new QName(HTTP_NS, "proxyAuthorization"), "proxyAuthorization", 
                ProxyAuthorizationPolicy.class);
        mapElementToJaxbProperty(context, bean, element,
                new QName(HTTP_NS, "authorization"), "authorization", AuthorizationPolicy.class);
        
        parseAttributes(element, context, bean);
        parseChildElements(element, context, bean);

        bean.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
        
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
        }
    }

    private void mapTLSClientParameters(ParserContext ctx, MutableBeanMetadata bean, Element el) {
        StringWriter writer = new StringWriter();
        XMLStreamWriter xmlWriter = StaxUtils.createXMLStreamWriter(writer);
        try {
            StaxUtils.copy(el, xmlWriter);
            xmlWriter.flush();
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        Object v = TLSClientParametersConfig.createTLSClientParameters(writer.toString());
        MutablePassThroughMetadata value = ctx.createMetadata(MutablePassThroughMetadata.class);
        value.setObject(v);
        bean.addProperty("tlsClientParameters", value);
    }

    
    private void mapBeanOrClassElement(ParserContext ctx, MutableBeanMetadata bean, Element el, 
                                       Class<?> cls) {
        String elementName = el.getLocalName();
        String classProperty = el.getAttribute("class");
        String beanref = el.getAttribute("bean");
        if (classProperty != null && !classProperty.equals("")) {
            bean.addProperty(elementName, createObjectOfClass(ctx, classProperty));
        } else if (beanref != null && !beanref.equals("")) {
            bean.addProperty(elementName, createRef(ctx, beanref));
        }
    }
}
