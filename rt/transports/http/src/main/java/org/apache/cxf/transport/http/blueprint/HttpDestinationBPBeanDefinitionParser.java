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

package org.apache.cxf.transport.http.blueprint;

import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.osgi.service.blueprint.reflect.Metadata;

public class HttpDestinationBPBeanDefinitionParser extends AbstractBPBeanDefinitionParser {
    private static final Logger LOG = LogUtils.getL7dLogger(HttpDestinationBPBeanDefinitionParser.class);
    
    private static final String HTTP_NS =
        "http://cxf.apache.org/transports/http/configuration";

    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        
        bean.setRuntimeClass(AbstractHTTPDestination.class);

        mapElementToHolder(context, bean, element, new QName(HTTP_NS, "server"), "server", 
                           HTTPServerPolicyHolder.class);
        mapElementToJaxbProperty(context, bean, element, new QName(HTTP_NS, "fixedParameterOrder"),
                                 "fixedParameterOrder", Boolean.class);
        mapElementToJaxbProperty(context, bean, element, new QName(HTTP_NS, "contextMatchStrategy"),
                                 "contextMatchStrategy", String.class);
        
        parseAttributes(element, context, bean);
        parseChildElements(element, context, bean);

        bean.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
        
        return bean;
    }

    @Override
    protected void processNameAttribute(Element element, ParserContext context, MutableBeanMetadata bean,
                                        String val) {
        bean.setId(val);
    }
    
    @Override
    protected void mapElementToJaxbProperty(ParserContext ctx,
                                            MutableBeanMetadata bean, Element data, 
                                            String propertyName, 
                                            Class<?> c) {
        try {
            Unmarshaller unmarshaller = getContext(c).createUnmarshaller();
            MutableValueMetadata value = ctx.createMetadata(MutableValueMetadata.class);
            value.setStringValue(unmarshaller.unmarshal(data, c).getValue().toString());
            bean.addProperty(propertyName, value);
        } catch (JAXBException e) {
            LOG.warning("Unable to parse property " + propertyName + " due to " + e);
        }
    }
    
    
}
