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

package org.apache.cxf.ws.rm.blueprint;

import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.rm.manager.DeliveryAssuranceType;
import org.apache.cxf.ws.rm.manager.DestinationPolicyType;
import org.apache.cxf.ws.rm.manager.ObjectFactory;
import org.apache.cxf.ws.rm.manager.SourcePolicyType;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 * This class provides some common functions used by the two BP bean definition parsers
 * in this package. 
 * 
 */
public class RMBPBeanDefinitionParser extends AbstractBPBeanDefinitionParser {
    protected static final String RM_NS = "http://cxf.apache.org/ws/rm/manager";

    private static final Logger LOG = LogUtils.getL7dLogger(RMBPBeanDefinitionParser.class);

    private JAXBContext jaxbContext;

    private Class<?> beanClass;
    
    public RMBPBeanDefinitionParser(Class<?> beanClass) {
        this.beanClass = beanClass;
    }
    
    protected Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);

        bean.setRuntimeClass(beanClass);

        String bus = element.getAttribute("bus");
        if (StringUtils.isEmpty(bus)) {
            bus = "cxf";
        }

        mapElementToJaxbProperty(context, bean, element,
                new QName(RM_NS, "deliveryAssurance"), "deliveryAssurance", DeliveryAssuranceType.class);
        mapElementToJaxbProperty(context, bean, element,
                new QName(RM_NS, "sourcePolicy"), "sourcePolicy", SourcePolicyType.class);
        mapElementToJaxbProperty(context, bean, element, 
                new QName(RM_NS, "destinationPolicy"), "destinationPolicy", DestinationPolicyType.class);
        mapElementToJaxbProperty(context, bean, element, 
                new QName("http://schemas.xmlsoap.org/ws/2005/02/rm/policy", "RMAssertion"), 
                "RMAssertion",
                org.apache.cxf.ws.rm.policy.RMAssertion.class);
        mapElementToJaxbProperty(context, bean, element,
                new QName("http://docs.oasis-open.org/ws-rx/wsrmp/200702", "RMAssertion"), 
                "RMAssertion",
                org.apache.cxf.ws.rm.policy.RMAssertion.class);

        parseAttributes(element, context, bean);
        parseChildElements(element, context, bean);

        bean.setId(beanClass.getName() + context.generateId());

        return bean;
    }



    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("store".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, name);
        } else if ("addressingNamespace".equals(name)) {
            bean.addProperty("addressingNamespace", createValue(ctx, el.getTextContent()));
        }
    }

    protected void mapElementToJaxbProperty(ParserContext ctx,
                                            MutableBeanMetadata bean, Element parent, 
                                            QName name,
                                            String propertyName, 
                                            Class<?> c) {

        Element data = DOMUtils.getFirstElement(parent);
        
        try {
            Unmarshaller unmarshaller = getJAXBContext().createUnmarshaller();
            MutablePassThroughMetadata value = ctx.createMetadata(MutablePassThroughMetadata.class);
            value.setObject(unmarshaller.unmarshal(data, c).getValue());
            bean.addProperty(propertyName, value);
        } catch (JAXBException e) {
            LOG.warning("Unable to parse property " + propertyName + " due to " + e);
        }
    }

    private synchronized JAXBContext getJAXBContext() throws JAXBException {
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(PackageUtils.getPackageName(ObjectFactory.class), 
                                                  ObjectFactory.class.getClassLoader());
        }
        return jaxbContext;
    }
}
