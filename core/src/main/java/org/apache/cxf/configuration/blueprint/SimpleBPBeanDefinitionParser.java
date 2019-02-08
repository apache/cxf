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

package org.apache.cxf.configuration.blueprint;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.Metadata;

/**
 *
 */
public class SimpleBPBeanDefinitionParser extends AbstractBPBeanDefinitionParser {
    protected Class<?> cls;

    public SimpleBPBeanDefinitionParser(Class<?> cls) {
        this.cls = cls;
    }

    public String getFactorySuffix() {
        return null;
    }
    public String getFactoryCreateType(Element element) {
        return null;
    }

    public String getId(Element element, ParserContext context) {
        return element.hasAttribute("id") ? element.getAttribute("id") : null;
    }

    public Metadata parse(Element element, ParserContext context) {

        MutableBeanMetadata cxfBean = context.createMetadata(MutableBeanMetadata.class);
        cxfBean.setRuntimeClass(cls);
        String fact = getFactorySuffix();
        if (fact == null) {
            cxfBean.setId(getId(element, context));
        } else {
            cxfBean.setId(getId(element, context) + fact);
        }
        parseAttributes(element, context, cxfBean);
        parseChildElements(element, context, cxfBean);
        if (hasBusProperty()) {
            boolean foundBus = false;
            for (BeanProperty bp : cxfBean.getProperties()) {
                if ("bus".equals(bp.getName())) {
                    foundBus = true;
                }
            }
            if (!foundBus) {
                cxfBean.addProperty("bus", getBusRef(context, "cxf"));
            }
        }
        if (fact != null) {
            context.getComponentDefinitionRegistry().registerComponentDefinition(cxfBean);

            MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
            bean.setId(getId(element, context));
            bean.setFactoryComponent(cxfBean);
            bean.setFactoryMethod("create");
            bean.setClassName(getFactoryCreateType(element));
            return bean;
        }
        return cxfBean;
    }


}
