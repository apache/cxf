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
package org.apache.cxf.ws.policy.spring;


import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.ws.policy.AlternativeSelector;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;


public class PolicyEngineBeanDefinitionParser extends AbstractBeanDefinitionParser {
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        bean.addConstructorArgReference(PolicyEngine.class.getName());
        super.doParse(element, ctx, bean);
    }
    
    
    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
        if ("alternativeSelector".equals(name)) {            
            setFirstChildAsProperty(e, ctx, bean, name);
        }
    }

    @Override
    protected Class getBeanClass(Element el) {
        return PolicyEngineConfig.class;
    }

    @Override
    protected String resolveId(Element e, AbstractBeanDefinition abd, ParserContext ctx) 
        throws BeanDefinitionStoreException {
        return PolicyEngineConfig.class.getName();
    }

    
    public static class PolicyEngineConfig {
        
        private PolicyEngineImpl engine;
        
        public PolicyEngineConfig(PolicyEngine e) {
            engine = (PolicyEngineImpl)e;
        }
        
        public boolean getEnabled() {
            return engine.isEnabled();
        }
        public void setEnabled(boolean enabled) {
            engine.setEnabled(enabled);
        }
        public boolean getIgnoreUnknownAssertions() {
            return engine.isIgnoreUnknownAssertions();
        }
        public void setIgnoreUnknownAssertions(boolean ignoreUnknownAssertions) {
            engine.setIgnoreUnknownAssertions(ignoreUnknownAssertions);
        }
        public AlternativeSelector getAlternativeSelector() {
            return engine.getAlternativeSelector();
        }
        public void setAlternativeSelector(AlternativeSelector alternativeSelector) {
            engine.setAlternativeSelector(alternativeSelector);
        }
    }

}