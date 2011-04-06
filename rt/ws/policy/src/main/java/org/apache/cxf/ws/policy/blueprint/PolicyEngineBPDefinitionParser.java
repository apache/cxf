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

package org.apache.cxf.ws.policy.blueprint;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.ws.policy.AlternativeSelector;
import org.apache.cxf.ws.policy.PolicyEngine;
import org.apache.cxf.ws.policy.PolicyEngineImpl;
import org.osgi.service.blueprint.reflect.Metadata;

public class PolicyEngineBPDefinitionParser extends AbstractBPBeanDefinitionParser {

    public Metadata parse(Element element, ParserContext context) {
        MutableBeanMetadata policyEngineConfig = context.createMetadata(MutableBeanMetadata.class);

        policyEngineConfig.setRuntimeClass(PolicyEngineConfig.class);

        String bus = element.getAttribute("bus");
        if (StringUtils.isEmpty(bus)) {
            bus = "cxf";
        }
        policyEngineConfig.addArgument(getBusRef(context, bus), Bus.class.getName(), 0);

        parseAttributes(element, context, policyEngineConfig);
        parseChildElements(element, context, policyEngineConfig);

        policyEngineConfig.setId(PolicyEngineConfig.class.getName() + context.generateId());

        return policyEngineConfig;
    }

    @Override
    protected void mapElement(ParserContext ctx, MutableBeanMetadata bean, Element el, String name) {
        if ("alternativeSelector".equals(name)) {
            setFirstChildAsProperty(el, ctx, bean, name);
        }
    }

    public static class PolicyEngineConfig extends AbstractFeature {

        private PolicyEngineImpl engine;

        public PolicyEngineConfig(Bus bus) {
            engine = (PolicyEngineImpl) bus.getExtension(PolicyEngine.class);
        }

        public boolean getEnabled() {
            return engine.isEnabled();
        }

        public void setEnabled(boolean enabled) {
            engine.setEnabled(enabled);
        }

        public boolean getIgnoreUnknownAssertions() {
            return engine.isIgnoreUnknownAssertions();
        }

        public void setIgnoreUnknownAssertions(boolean ignoreUnknownAssertions) {
            engine.setIgnoreUnknownAssertions(ignoreUnknownAssertions);
        }

        public AlternativeSelector getAlternativeSelector() {
            return engine.getAlternativeSelector();
        }

        public void setAlternativeSelector(AlternativeSelector alternativeSelector) {
            engine.setAlternativeSelector(alternativeSelector);
        }
    }
}
