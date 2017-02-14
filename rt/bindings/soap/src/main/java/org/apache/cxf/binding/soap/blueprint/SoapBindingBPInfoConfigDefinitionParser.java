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
package org.apache.cxf.binding.soap.blueprint;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.binding.soap.SoapBindingConfiguration;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.osgi.service.blueprint.reflect.Metadata;

public class SoapBindingBPInfoConfigDefinitionParser extends AbstractBPBeanDefinitionParser {

    public Metadata parse(Element element, ParserContext context) {
        if (!context.getComponentDefinitionRegistry()
                .containsComponentDefinition(SoapVersionTypeConverter.class.getName())) {
            MutablePassThroughMetadata md = context.createMetadata(MutablePassThroughMetadata.class);
            md.setObject(new SoapVersionTypeConverter());
            md.setId(SoapVersionTypeConverter.class.getName());
            context.getComponentDefinitionRegistry().registerTypeConverter(md);
        }

        MutableBeanMetadata cxfBean = context.createMetadata(MutableBeanMetadata.class);
        cxfBean.setRuntimeClass(SoapBindingConfiguration.class);
        parseAttributes(element, context, cxfBean);
        parseChildElements(element, context, cxfBean);

        return cxfBean;
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {

        if ("version".equals(name)
            || "mtomEnabled".equals(name)
            || "style".equals(name)
            || "use".equals(name)) {
            bean.addProperty(name, parseMapData(ctx, bean, el));
        }
    }
}
