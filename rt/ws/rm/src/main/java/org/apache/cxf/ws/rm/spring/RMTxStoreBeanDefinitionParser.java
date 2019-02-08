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
package org.apache.cxf.ws.rm.spring;

import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.ws.rm.persistence.jdbc.RMTxStore;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class RMTxStoreBeanDefinitionParser extends AbstractBeanDefinitionParser {
    @Override
    protected void doParse(Element element, ParserContext ctx, BeanDefinitionBuilder bean) {
        super.doParse(element, ctx, bean);
        bean.setInitMethodName("init");
        bean.setDestroyMethodName("destroy");
    }
    @Override
    protected void mapAttribute(BeanDefinitionBuilder bean, Element e, String name, String val) {
        if ("dataSource".equals(name)) {
            if (val != null && val.trim().length() > 0) {
                bean.addPropertyReference("dataSource", val);
            }
        } else {
            super.mapAttribute(bean, e, name, val);
        }
    }

    @Override
    protected Class<?> getBeanClass(Element element) {
        return RMTxStore.class;
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

}
