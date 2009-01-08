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
package org.apache.cxf.transport.jms.spring;

import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.transport.jms.AddressType;
import org.apache.cxf.transport.jms.JMSDestination;
import org.apache.cxf.transport.jms.ServerBehaviorPolicyType;
import org.apache.cxf.transport.jms.ServerConfig;
import org.apache.cxf.transport.jms.SessionPoolType;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class JMSDestinationBeanDefinitionParser extends AbstractBeanDefinitionParser {

    private static final String JMS_NS = "http://cxf.apache.org/transports/jms";

    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        bean.setAbstract(true);
        mapElementToJaxbProperty(element, bean, new QName(JMS_NS, "serverConfig"), "serverConfig",
                                 ServerConfig.class);
        mapElementToJaxbProperty(element, bean, new QName(JMS_NS, "runtimePolicy"), "serverBehavior",
                                 ServerBehaviorPolicyType.class);
        mapElementToJaxbProperty(element, bean, new QName(JMS_NS, "address"), "address",
                                 AddressType.class);
        mapElementToJaxbProperty(element, bean, new QName(JMS_NS, "sessionPool"), "sessionPool",
                                 SessionPoolType.class);        
        List<Element> elemList = 
            DOMUtils.findAllElementsByTagNameNS(element, JMS_NS, "jmsConfig-ref");

        if (elemList.size() == 1) {
            Node el1 = elemList.get(0);
            bean.addPropertyReference("jmsConfig", el1.getTextContent());
        }
    }

    @Override
    protected Class getBeanClass(Element arg0) {
        return JMSDestination.class;
    }

}
