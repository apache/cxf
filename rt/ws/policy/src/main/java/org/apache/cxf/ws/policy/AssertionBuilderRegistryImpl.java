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

package org.apache.cxf.ws.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.spring.MapProvider;
import org.apache.cxf.extension.BusExtension;
import org.apache.cxf.extension.RegistryImpl;
import org.apache.cxf.ws.policy.builder.xml.XmlPrimitiveAssertion;

/**
 * 
 */
public class AssertionBuilderRegistryImpl extends RegistryImpl<QName, AssertionBuilder> implements
    AssertionBuilderRegistry, BusExtension {

    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(AssertionBuilderRegistryImpl.class);
    private static final Logger LOG 
        = LogUtils.getL7dLogger(AssertionBuilderRegistryImpl.class);
    private static final int IGNORED_CACHE_SIZE = 10;
    private boolean ignoreUnknownAssertions = true; 
    private List<QName> ignored = new ArrayList<QName>(IGNORED_CACHE_SIZE);
    private Bus bus;
    
    public AssertionBuilderRegistryImpl() {
        super(null);
    }

    public AssertionBuilderRegistryImpl(Map<QName, AssertionBuilder> builders) {
        super(builders);
    }
    public AssertionBuilderRegistryImpl(MapProvider<QName, AssertionBuilder> builders) {
        super(builders.createMap());
    }

    public void setBus(Bus b) {
        bus = b;
    }

    public Class<?> getRegistrationType() {
        return AssertionBuilderRegistry.class;
    }
    
    public boolean isIgnoreUnknownAssertions() {
        return ignoreUnknownAssertions;
    }

    public void setIgnoreUnknownAssertions(boolean ignore) {
        ignoreUnknownAssertions = ignore;
    }

    public PolicyAssertion build(Element element) {

        AssertionBuilder builder;

        QName qname = new QName(element.getNamespaceURI(), element.getLocalName());
        builder = get(qname);

        if (null == builder) {
            Message m = new Message("NO_ASSERTIONBUILDER_EXC", BUNDLE, qname.toString());
            if (ignoreUnknownAssertions) {
                boolean alreadyWarned = ignored.contains(qname);
                if (alreadyWarned) {
                    ignored.remove(qname);
                } else if (ignored.size() == IGNORED_CACHE_SIZE) {
                    ignored.remove(IGNORED_CACHE_SIZE - 1);
                }
                ignored.add(0, qname);
                if (!alreadyWarned) {
                    LOG.warning(m.toString());
                }
                return new XmlPrimitiveAssertion(element, bus.getExtension(PolicyConstants.class));
            } else {
                throw new PolicyException(m);
            }
        }

        return builder.build(element);

    }
}
