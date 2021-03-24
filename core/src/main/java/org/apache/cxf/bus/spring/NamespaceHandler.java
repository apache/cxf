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

import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.SimpleBeanDefinitionParser;
import org.apache.cxf.feature.FastInfosetFeature;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.ParserContext;

public class NamespaceHandler extends NamespaceHandlerSupport {
    @SuppressWarnings("deprecation")
    public void init() {
        registerBeanDefinitionParser("bus",
                                     new BusDefinitionParser());
        registerBeanDefinitionParser("logging",
                                     new SimpleBeanDefinitionParser(org.apache.cxf.feature.LoggingFeature.class));
        registerBeanDefinitionParser("fastinfoset",
                                     new SimpleBeanDefinitionParser(FastInfosetFeature.class));

        registerBeanDefinitionParser("workqueue",
                                     new SimpleBeanDefinitionParser(AutomaticWorkQueueImpl.class) {

                @Override
                protected void processNameAttribute(Element element,
                                                ParserContext ctx,
                                                BeanDefinitionBuilder bean,
                                                String val) {
                    bean.addPropertyValue("name", val);
                    element.removeAttribute("name");
                    if (!element.hasAttribute("id")) {
                        element.setAttribute("id", "cxf.workqueue." + val);
                    }

                }
            });
    }
}
