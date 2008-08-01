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

package org.apache.cxf.service.invoker;

import java.lang.reflect.Modifier;
import java.util.ResourceBundle;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.injection.ResourceInjector;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.resource.ResourceManager;

/**
 * Creates a new instance of the service object for each call to create().
 */
public class PerRequestFactory implements Factory {
    private static final ResourceBundle BUNDLE = BundleUtils.getBundle(PerRequestFactory.class);

    private final Class svcClass;

    public PerRequestFactory(final Class svcClass) {
        super();
        this.svcClass = svcClass;
    }

    public Object create(Exchange ex) throws Throwable {
        try {
            if (svcClass.isInterface()) {
                throw new Fault(new Message("SVC_CLASS_IS_INTERFACE", BUNDLE, svcClass.getName()));
            }

            if (Modifier.isAbstract(svcClass.getModifiers())) {
                throw new Fault(new Message("SVC_CLASS_IS_ABSTRACT", BUNDLE, svcClass.getName()));
            }
            Object o = svcClass.newInstance();
            Bus b = ex.get(Bus.class);
            ResourceManager resourceManager = b.getExtension(ResourceManager.class);
            if (resourceManager != null) {
                ResourceInjector injector = new ResourceInjector(resourceManager);
                injector.inject(o);
                injector.construct(o);
            }
            return o;
        } catch (InstantiationException e) {
            throw new Fault(new Message("COULD_NOT_INSTANTIATE", BUNDLE), e);
        } catch (IllegalAccessException e) {
            throw new Fault(new Message("ILLEGAL_ACCESS", BUNDLE), e);
        }
    }

    public void release(Exchange ex, Object o) {
        //nothing to do
    }
}
