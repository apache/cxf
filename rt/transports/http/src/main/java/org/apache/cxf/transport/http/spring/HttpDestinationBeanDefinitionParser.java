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
package org.apache.cxf.transport.http.spring;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.configuration.spring.AbstractBeanDefinitionParser;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;

public class HttpDestinationBeanDefinitionParser 
    extends AbstractBeanDefinitionParser {

    private static final String HTTP_NS = 
        "http://cxf.apache.org/transports/http/configuration";

    @Override
    public void doParse(Element element, ParserContext ctc, BeanDefinitionBuilder bean) {
        bean.setAbstract(true);
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "server"), "server");
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "fixedParameterOrder"),
                                   "fixedParameterOrder");
        mapElementToJaxbProperty(element, bean, 
                new QName(HTTP_NS, "contextMatchStrategy"),
                                   "contextMatchStrategy");
    }
    
    @Override
    protected String getJaxbPackage() {
        return "org.apache.cxf.transports.http.configuration";
    }
    
    @Override
    protected Class getBeanClass(Element arg0) {
        return AbstractHTTPDestination.class;
    }

}
