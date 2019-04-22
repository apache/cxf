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
package org.apache.cxf.configuration.spring;

import java.util.Collections;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.w3c.dom.Element;

import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.staxutils.transform.OutTransformWriter;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;

public class MappingBeanDefinitionParser
    extends org.springframework.beans.factory.xml.AbstractBeanDefinitionParser {

    private final Map<String, String> transformMap;
    public MappingBeanDefinitionParser(String oldns, String newns) {
        transformMap = Collections.singletonMap("{" + oldns + "}*", "{" + newns + "}*");
    }

    @Override
    protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
        return (AbstractBeanDefinition)parserContext.getDelegate().parseCustomElement(transformElement(element));
    }
    private Element transformElement(Element element) {

        W3CDOMStreamWriter domWriter = new W3CDOMStreamWriter();
        OutTransformWriter transformWriter = new OutTransformWriter(domWriter, transformMap);
        try {
            StaxUtils.copy(element, transformWriter);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return domWriter.getDocument().getDocumentElement();
    }

}
