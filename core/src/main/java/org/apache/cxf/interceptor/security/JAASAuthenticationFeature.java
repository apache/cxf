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
package org.apache.cxf.interceptor.security;

import org.apache.cxf.Bus;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

/**
 * Feature to do JAAS authentication with defaults for karaf integration
 */
public class JAASAuthenticationFeature extends DelegatingFeature<JAASAuthenticationFeature.Portable> {
    public static final String ID = "jaas";

    public JAASAuthenticationFeature() {
        super(new Portable());
    }

    public void setContextName(String contextName) {
        delegate.setContextName(contextName);
    }

    public void setReportFault(boolean reportFault) {
        delegate.setReportFault(reportFault);
    }

    @Override
    public String getID() {
        return ID;
    }

    public static class Portable implements AbstractPortableFeature {
        private String contextName = "karaf";
        private boolean reportFault;

        @Override
        public void doInitializeProvider(InterceptorProvider provider, Bus bus) {
            JAASLoginInterceptor jaasLoginInterceptor = new JAASLoginInterceptor();
            jaasLoginInterceptor.setRoleClassifierType(JAASLoginInterceptor.ROLE_CLASSIFIER_CLASS_NAME);
            jaasLoginInterceptor.setRoleClassifier("org.apache.karaf.jaas.boot.principal.RolePrincipal");
            jaasLoginInterceptor.setContextName(contextName);
            jaasLoginInterceptor.setReportFault(reportFault);
            provider.getInInterceptors().add(jaasLoginInterceptor);
        }

        public void setContextName(String contextName) {
            this.contextName = contextName;
        }

        public void setReportFault(boolean reportFault) {
            this.reportFault = reportFault;
        }
    }

}
