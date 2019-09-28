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
package org.apache.cxf.transport.http_undertow.blueprint;

import java.util.List;
import java.util.StringTokenizer;
import java.util.UUID;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableMapMetadata;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

public class UndertowServerEngineFactoryParser extends AbstractBPBeanDefinitionParser {

    public static final String UNDERTOW_TRANSPORT = "http://cxf.apache.org/transports/http-undertow/configuration";

    public static final String UNDERTOW_THREADING = "http://cxf.apache.org/configuration/parameterized-types";

    public static String getIdOrName(Element elem) {
        String id = elem.getAttribute("id");

        if (null == id || "".equals(id)) {
            String names = elem.getAttribute("name");
            if (null != names) {
                StringTokenizer st = new StringTokenizer(names, ",");
                if (st.countTokens() > 0) {
                    id = st.nextToken();
                }
            }
        }
        return id;
    }

    public Metadata parse(Element element, ParserContext context) {

        //Endpoint definition
        MutableBeanMetadata ef = context.createMetadata(MutableBeanMetadata.class);
        if (!StringUtils.isEmpty(getIdOrName(element))) {
            ef.setId(getIdOrName(element));
        } else {
            ef.setId("undertow.engine.factory-holder-" + UUID.randomUUID().toString());
        }
        ef.setRuntimeClass(UndertowHTTPServerEngineFactoryHolder.class);

        // setup the HandlersMap property for the UndertowHTTPServerEngineFactoryHolder

        try {
            // Print the DOM node
            String xmlString = StaxUtils.toString(element);
            ef.addProperty("parsedElement", createValue(context, xmlString));
            ef.setInitMethod("init");
            ef.setActivation(ComponentMetadata.ACTIVATION_EAGER);
            ef.setDestroyMethod("destroy");

            // setup the EngineConnector
            List<Element> engines = DOMUtils
                .getChildrenWithName(element, HTTPUndertowTransportNamespaceHandler.UNDERTOW_TRANSPORT, "engine");
            ef.addProperty("handlersMap", parseEngineHandlers(engines, ef, context));
            return ef;
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
    }


    protected Metadata parseEngineHandlers(List<Element> engines, ComponentMetadata enclosingComponent,
                                           ParserContext context) {
        MutableMapMetadata map = context.createMetadata(MutableMapMetadata.class);
        map.setKeyType("java.lang.String");
        map.setValueType("java.util.List");

        for (Element engine : engines) {
            String port = engine.getAttribute("port");
            ValueMetadata keyValue = createValue(context, port);
            Element handlers = DOMUtils
                .getFirstChildWithName(engine, HTTPUndertowTransportNamespaceHandler.UNDERTOW_TRANSPORT,
                                       "handlers");
            if (handlers != null) {
                Metadata valValue = parseListData(context, enclosingComponent, handlers);
                map.addEntry(keyValue, valValue);
            }
        }
        return map;
    }


}
