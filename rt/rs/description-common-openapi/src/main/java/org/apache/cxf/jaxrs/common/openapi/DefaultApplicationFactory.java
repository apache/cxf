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

package org.apache.cxf.jaxrs.common.openapi;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.core.Application;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

public final class DefaultApplicationFactory {
    protected static class DefaultApplication extends Application {
        private final Set<Class<?>> serviceClasses;
        
        DefaultApplication(final Set<Class<?>> serviceClasses) {
            this.serviceClasses = serviceClasses;
        }

        DefaultApplication(final List<ClassResourceInfo> cris, final Set<String> resourcePackages) {
            this.serviceClasses = cris.stream().map(ClassResourceInfo::getServiceClass).
                    filter(cls -> (resourcePackages == null || resourcePackages.isEmpty()) || resourcePackages.stream().
                            anyMatch(pkg -> cls.getPackage().getName().startsWith(pkg))).collect(Collectors.toSet());
        }

        @Override
        public Set<Class<?>> getClasses() {
            return serviceClasses;
        }
    }
    
    private DefaultApplicationFactory() {
    }
    
    /**
     * Detects the application (if present) or creates the default application (in case the scan is disabled).
     */
    public static Application createApplicationOrDefault(final Server server, final ServerProviderFactory factory, 
            final JAXRSServiceFactoryBean sfb, final Bus bus, final Set<String> resourcePackages, 
                final boolean scan) {

        ApplicationInfo appInfo = null;
        if (!scan) {
            appInfo = factory.getApplicationProvider();
            
            if (appInfo == null) {
                appInfo = createApplicationInfo(sfb, resourcePackages, bus);
                server.getEndpoint().put(Application.class.getName(), appInfo);
            }
        }
        
        return (appInfo == null) ? null : appInfo.getProvider();
    }
    
    
    /**
     * Detects the application (if present) or creates the default application (in case the scan is disabled).
     */
    public static ApplicationInfo createApplicationInfoOrDefault(final Server server, 
                final ServerProviderFactory factory, final JAXRSServiceFactoryBean sfb, final Bus bus, 
                    final boolean scan) {
        
        ApplicationInfo appInfo = null;
        if (!scan) {
            appInfo = factory.getApplicationProvider();
            if (appInfo == null) {
                Set<Class<?>> serviceClasses = new HashSet<>();
                for (ClassResourceInfo cri : sfb.getClassResourceInfo()) {
                    serviceClasses.add(cri.getServiceClass());
                }
                appInfo = createApplicationInfo(serviceClasses, bus);
                server.getEndpoint().put(Application.class.getName(), appInfo);
            }
        }
        
        return appInfo;
    }
    
    public static ApplicationInfo createApplicationInfo(final Set<Class<?>> serviceClasses, final Bus bus) {
        return new ApplicationInfo(createApplication(serviceClasses), bus);
    }
    public static ApplicationInfo createApplicationInfo(final JAXRSServiceFactoryBean sfb, 
            final Set<String> resourcePackages, final Bus bus) {
        return new ApplicationInfo(createApplication(sfb.getClassResourceInfo(), resourcePackages), bus);
    }

    public static Application createApplication(final Set<Class<?>> serviceClasses) {
        return new DefaultApplication(serviceClasses);
    }
    
    public static Application createApplication(final List<ClassResourceInfo> cris, 
            final Set<String> resourcePackages) {
        return new DefaultApplication(cris, resourcePackages);
    }
}

