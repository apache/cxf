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

import java.util.UUID;
import java.util.concurrent.Callable;

import org.w3c.dom.Element;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.cxf.configuration.blueprint.AbstractBPBeanDefinitionParser;
import org.apache.cxf.ws.policy.PolicyBean;
import org.apache.cxf.ws.policy.PolicyConstants;
import org.osgi.service.blueprint.reflect.Metadata;

public class PolicyBPDefinitionParser extends AbstractBPBeanDefinitionParser {

    public Metadata parse(Element element, ParserContext context) {
        MutablePassThroughMetadata factory = context.createMetadata(MutablePassThroughMetadata.class);
        factory.setId(resolveId(element, context) + UUID.randomUUID().toString());

        PolicyBean policyBean = new PolicyBean();
        policyBean.setElement(element);
        factory.setObject(new PassThroughCallable<Object>(policyBean));

        MutableBeanMetadata resourceBean = context.createMetadata(MutableBeanMetadata.class);
        resourceBean.setId(resolveId(element, context));
        resourceBean.setFactoryComponent(factory);
        resourceBean.setFactoryMethod("call");

        return resourceBean;
    }

    protected String resolveId(Element element, ParserContext ctx) {
        return element.getAttributeNS(PolicyConstants.WSU_NAMESPACE_URI, PolicyConstants.WSU_ID_ATTR_NAME);
    }

    public static class PassThroughCallable<T> implements Callable<T> {

        private T value;

        public PassThroughCallable(T value) {
            this.value = value;
        }

        public T call() throws Exception {
            return value;
        }
    }
}
