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

package org.apache.cxf.systest.ws.policy;

import java.util.ArrayList;
import java.util.List;

import org.apache.cxf.bus.extension.ExtensionManager;
import org.apache.cxf.bus.extension.ExtensionManagerImpl;
import org.apache.cxf.service.factory.AbstractServiceFactoryBean;
import org.apache.cxf.service.factory.FactoryBeanListener;

public class UriDomainFactoryBeanListener implements FactoryBeanListener {

    @Override
    public void handleEvent(Event ev, AbstractServiceFactoryBean factory, Object... args) {
        if (ev.equals(Event.START_CREATE)) {
            // Remove original URIDomainExpressionBuilder to be replaced on custom one
            ExtensionManagerImpl orig = (ExtensionManagerImpl)factory.getBus().getExtension(ExtensionManager.class);
            List<String> names = new ArrayList<>();
            names.add(org.apache.cxf.ws.policy.attachment.external.URIDomainExpressionBuilder.class.getName());
            orig.removeBeansOfNames(names);
        }
    }
}
