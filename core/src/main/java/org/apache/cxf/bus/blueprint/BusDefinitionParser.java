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

package org.apache.cxf.bus.blueprint;


import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.osgi.service.blueprint.reflect.Metadata;

public class BusDefinitionParser
    extends AbstractBPBeanDefinitionParser {

    public BusDefinitionParser() {
    }

    public Metadata parse(Element element, ParserContext context) {
        String bname = element.hasAttribute("bus") ? element.getAttribute("bus") : "cxf";
        String id = element.hasAttribute("id") ? element.getAttribute("id") : null;
        MutableBeanMetadata cxfBean = getBus(context, bname);
        parseAttributes(element, context, cxfBean);
        parseChildElements(element, context, cxfBean);
        context.getComponentDefinitionRegistry().removeComponentDefinition(bname);
        if (!StringUtils.isEmpty(id)) {
            cxfBean.addProperty("id", createValue(context, id));
        }
        return cxfBean;
    }
    
    @Override
    protected void processBusAttribute(Element element, ParserContext ctx,
                                       MutableBeanMetadata bean, String val) {
        //nothing
    }
    
    @Override
    protected boolean hasBusProperty() {
        return false;
    }


    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("inInterceptors".equals(name) || "inFaultInterceptors".equals(name)
            || "outInterceptors".equals(name) || "outFaultInterceptors".equals(name)
            || "features".equals(name)) {
            bean.addProperty(name, parseListData(ctx, bean, el));
        } else if ("properties".equals(name)) {
            bean.addProperty(name, parseMapData(ctx, bean, el));
        }
    }
}
