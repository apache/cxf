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

package org.apache.cxf.ws.policy.attachment.external;

import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.extension.RegistryImpl;
import org.apache.cxf.ws.policy.AssertionBuilderRegistry;
import org.apache.cxf.ws.policy.PolicyException;

/**
 *
 */
@NoJSR250Annotations(unlessNull = "bus")
public class DomainExpressionBuilderRegistry extends RegistryImpl<QName, DomainExpressionBuilder>
    implements BusExtension {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AssertionBuilderRegistry.class);

    private boolean dynamicLoaded;
    private Bus bus;
    public DomainExpressionBuilderRegistry() {
        super(null);
    }
    public DomainExpressionBuilderRegistry(Bus b) {
        super(null);
        setBus(b);
    }

    public DomainExpressionBuilderRegistry(Map<QName, DomainExpressionBuilder> builders) {
        super(builders);
    }
    @Resource
    public final void setBus(Bus b) {
        bus = b;
        if (b != null) {
            b.setExtension(this, DomainExpressionBuilderRegistry.class);
        }
    }

    public Class<?> getRegistrationType() {
        return DomainExpressionBuilderRegistry.class;
    }

    protected synchronized void loadDynamic() {
        if (!dynamicLoaded && bus != null) {
            dynamicLoaded = true;
            ConfiguredBeanLocator c = bus.getExtension(ConfiguredBeanLocator.class);
            if (c != null) {
                for (DomainExpressionBuilder b : c.getBeansOfType(DomainExpressionBuilder.class)) {
                    for  (QName q : b.getDomainExpressionTypes()) {
                        register(q, b);
                    }
                }
            }
        }
    }

    public DomainExpression build(Element element) {
        loadDynamic();
        DomainExpressionBuilder builder;

        QName qname = new QName(element.getNamespaceURI(), element.getLocalName());
        builder = get(qname);

        if (null == builder) {
            throw new PolicyException(new Message("NO_DOMAINEXPRESSIONBUILDER_EXC",
                                                  BUNDLE, qname.toString()));
        }

        return builder.build(element);

    }
}
