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

package org.apache.cxf.configuration.spring;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.apache.cxf.common.util.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * This class makes it easy to create two simultaneous beans - a factory bean and the bean
 * that the factory produces.
 */
public abstract class AbstractFactoryBeanDefinitionParser extends AbstractBeanDefinitionParser {
    private static boolean factoriesAreAbstract = true;
    public static void setFactoriesAreAbstract(boolean b) {
        factoriesAreAbstract = b;
    }
    
    @SuppressWarnings("deprecation")
    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        Class factoryClass = getFactoryClass();
        BeanDefinitionBuilder factoryBean = bean;
        if (!FactoryBean.class.isAssignableFrom(factoryClass)) {
            factoryBean = BeanDefinitionBuilder.rootBeanDefinition(getFactoryClass());
        }

        NamedNodeMap atts = element.getAttributes();        
        boolean createdFromAPI = false;
        boolean setBus = false;
        for (int i = 0; i < atts.getLength(); i++) {
            Attr node = (Attr) atts.item(i);
            String val = node.getValue();
            String pre = node.getPrefix();
            String name = node.getLocalName();
            
            if ("createdFromAPI".equals(name)) {
                factoryBean.setAbstract(true);
                bean.setAbstract(true);
                createdFromAPI = true;
            } else if ("abstract".equals(name)) {
                factoryBean.setAbstract(true);
                bean.setAbstract(true);
            } else if (!"id".equals(name) && !"name".equals(name) && isAttribute(pre, name)) {
                if ("bus".equals(name)) {
                    setBus = true;
                }
                mapAttribute(factoryBean, element, name, val);
            } 
        }
        
        if (!setBus) {
            addBusWiringAttribute(factoryBean, BusWiringType.PROPERTY);
        }
        
        Node node = element.getFirstChild();
        while (node != null) {
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                String name = node.getLocalName();
                mapElement(ctx, factoryBean, (Element) node, name);
            }
            node = node.getNextSibling();
        }
        
        String id = getIdOrName(element);
        BeanDefinition container = ctx.getContainingBeanDefinition();
        boolean noFactory = false;
        if (StringUtils.isEmpty(id)) {
            if (container == null) {
                id = BeanDefinitionReaderUtils.generateBeanName(bean.getBeanDefinition(),
                                                                ctx.getRegistry(),
                                                                false);
            } else {
                id = BeanDefinitionReaderUtils.generateBeanName(bean.getBeanDefinition(),
                                                                ctx.getRegistry(),
                                                                true);
                noFactory = true;
                //inner bean, no need for the factory to be public at all
            }
        }
        if (createdFromAPI) {
            id = id + getSuffix();
        }
        
        if (FactoryBean.class.isAssignableFrom(getFactoryClass())) {
            if (!noFactory) {
                AbstractBeanDefinition def = factoryBean.getRawBeanDefinition().cloneBeanDefinition();
                def.setBeanClass(getRawFactoryClass());
                def.setAbstract(factoriesAreAbstract);
                def.setLazyInit(true);
                ctx.getRegistry().registerBeanDefinition(id + getFactoryIdSuffix(),
                                                         def);
            }
            bean.getBeanDefinition().setAttribute("id", id);
        } else {
            String factoryId = id + getFactoryIdSuffix();
            ctx.getRegistry().registerBeanDefinition(factoryId, factoryBean.getBeanDefinition());
            bean.getBeanDefinition().setAttribute("id", id);
            bean.setFactoryBean(factoryId, "create");
        }
    }

    protected abstract Class getFactoryClass();

    protected Class getRawFactoryClass() {
        return getFactoryClass();
    }

    /**
     * @return The Spring ID of the factory bean.
     */
    protected abstract String getFactoryIdSuffix();
}
