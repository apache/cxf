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
import javax.ws.rs.core.Configuration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.utils.AnnotationUtils;
import static org.apache.cxf.microprofile.client.MicroProfileClientConfigurableImpl.CONTRACTS;

public class MicroProfileClientFactoryBean extends JAXRSClientFactoryBean {
    private Configuration configuration;

    public MicroProfileClientFactoryBean(Configuration configuration, String baseUri, Class<?> aClass) {
        super();
        this.configuration = configuration;
        super.setAddress(baseUri);
        super.setServiceClass(aClass);
        super.setProviderComparator(new ContractComparator());
    }

    private class ContractComparator implements Comparator<Object> {
        @Override
        public int compare(Object o1, Object o2) {
            int left = getPriority(o1.getClass());
            int right = getPriority(o2.getClass());
            return right - left;
        }

        private int getPriority(Class<?> clazz) {
            for (Class<?> providerClass : CONTRACTS) {
                Map<Class<?>, Integer> contracts = MicroProfileClientFactoryBean.this.
                        configuration.getContracts(providerClass);
                if (contracts != null) {
                    Integer priority = contracts.get(clazz);
                    if (priority != null) {
                        return priority;
                    }
                }
            }
            return AnnotationUtils.getBindingPriority(clazz);
        }
    }

}
