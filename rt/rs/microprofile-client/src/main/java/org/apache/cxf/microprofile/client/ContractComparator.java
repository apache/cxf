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
package org.apache.cxf.microprofile.client;

import java.util.Comparator;
import java.util.Map;

import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;

import static org.apache.cxf.microprofile.client.MicroProfileClientConfigurableImpl.CONTRACTS;

class ContractComparator implements Comparator<ProviderInfo<?>> {
    private final MicroProfileClientFactoryBean microProfileClientFactoryBean;
    private final Comparator<ProviderInfo<?>> parent;

    ContractComparator(MicroProfileClientFactoryBean microProfileClientFactoryBean,
                       Comparator<ProviderInfo<?>> parent) {
        this.microProfileClientFactoryBean = microProfileClientFactoryBean;
        this.parent = parent;
    }

    @Override
    public int compare(ProviderInfo<?> oLeft, ProviderInfo<?> oRight) {
        int parentResult = parent.compare(oLeft, oRight);
        if (parentResult != 0) {
            return parentResult;
        }
        int left = getPriority(oLeft.getResourceClass());
        int right = getPriority(oRight.getResourceClass());
        return left - right;
    }

    private int getPriority(Class<?> clazz) {
        Map<Class<?>, Integer> contracts = microProfileClientFactoryBean.getConfiguration()
                .getContracts(clazz);
        if (contracts != null && !contracts.isEmpty()) {
            for (Class<?> providerClass : CONTRACTS) {
                Integer priority = contracts.get(providerClass);
                if (priority != null) {
                    return priority;
                }
            }
        }
        return AnnotationUtils.getBindingPriority(clazz);
    }
}
