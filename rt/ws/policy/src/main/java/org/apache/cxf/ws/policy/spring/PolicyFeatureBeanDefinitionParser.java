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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;



public class PolicyFeatureBeanDefinitionParser extends AbstractBeanDefinitionParser {

    @Override
    protected void parseChildElements(Element e, ParserContext ctx, BeanDefinitionBuilder bean) {
        List<Element> ps = new ArrayList<Element>();
        List<Element> prs = new ArrayList<Element>();     
        
        Element elem = DOMUtils.getFirstElement(e);
        while (elem != null) {
            if ("Policy".equals(elem.getLocalName())) {
                ps.add(elem);
            } else if ("PolicyReference".equals(elem.getLocalName())) {
                prs.add(elem);
            }
            elem = DOMUtils.getNextElement(elem);
        }
        bean.addPropertyValue("policyElements", ps);
        bean.addPropertyValue("policyReferenceElements", prs);
        
        super.parseChildElements(e, ctx, bean);
    }
    
    @Override
    protected void mapElement(ParserContext ctx, BeanDefinitionBuilder bean, Element e, String name) {
        if ("alternativeSelector".equals(name)) {            
            setFirstChildAsProperty(e, ctx, bean, name);
        }
    }

    @Override
    protected Class getBeanClass(Element el) {
        return WSPolicyFeature.class;
    }


}
