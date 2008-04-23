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

package org.apache.cxf.bus.spring;

import java.util.List;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class BusDefinitionParser extends AbstractBeanDefinitionParser {

    public BusDefinitionParser() {
        super();
        setBeanClass(CXFBusImpl.class);
    }

    @Override
    protected void mapElement(ParserContext ctx, 
                              BeanDefinitionBuilder bean, 
                              Element e, 
                              String name) {
        if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name)) {
            List list = ctx.getDelegate().parseListElement(e, bean.getBeanDefinition());
            bean.addPropertyValue(name, list);
        } 
    }
    
    protected String getIdOrName(Element elem) {
        String id = super.getIdOrName(elem); 
        if (StringUtils.isEmpty(id)) {
            id = Bus.DEFAULT_BUS_ID;
        }
        return id;
    }
}
