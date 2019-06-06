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
package org.apache.cxf.jaxrs.swagger;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;

public abstract class AbstractSwaggerFeature<T extends AbstractSwaggerFeature.Portable>
        extends DelegatingFeature<T> {
    protected AbstractSwaggerFeature(final T d) {
        super(d);
    }

    public static boolean isSwaggerJaxRsAvailable() {
        return Portable.isSwaggerJaxRsAvailable();
    }

    public void addSwaggerResource(Server server, Bus bus) {
        getDelegate().addSwaggerResource(server, bus);
    }

    public void setBasePathByAddress(String address) {
        getDelegate().setBasePathByAddress(address);
    }

    public void calculateDefaultResourcePackage(Server server) {
        getDelegate().calculateDefaultResourcePackage(server);
    }

    public void calculateDefaultBasePath(Server server) {
        getDelegate().calculateDefaultBasePath(server);
    }

    public String getResourcePackage() {
        return getDelegate().getResourcePackage();
    }

    public void setResourcePackage(String resourcePackage) {
        getDelegate().setResourcePackage(resourcePackage);
    }

    public String getVersion() {
        return getDelegate().getVersion();
    }

    public void setVersion(String version) {
        getDelegate().setVersion(version);
    }

    public String getBasePath() {
        return getDelegate().getBasePath();
    }

    public void setBasePath(String basePath) {
        getDelegate().setBasePath(basePath);
    }

    public String getTitle() {
        return getDelegate().getTitle();
    }

    public void setTitle(String title) {
        getDelegate().setTitle(title);
    }

    public String getDescription() {
        return getDelegate().getDescription();
    }

    public void setDescription(String description) {
        getDelegate().setDescription(description);
    }

    public String getContact() {
        return getDelegate().getContact();
    }

    public void setContact(String contact) {
        getDelegate().setContact(contact);
    }

    public String getLicense() {
        return getDelegate().getLicense();
    }

    public void setLicense(String license) {
        getDelegate().setLicense(license);
    }

    public String getLicenseUrl() {
        return getDelegate().getLicenseUrl();
    }

    public void setLicenseUrl(String licenseUrl) {
        getDelegate().setLicenseUrl(licenseUrl);
    }

    public String getTermsOfServiceUrl() {
        return getDelegate().getTermsOfServiceUrl();
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        getDelegate().setTermsOfServiceUrl(termsOfServiceUrl);
    }

    public String getFilterClass() {
        return getDelegate().getFilterClass();
    }

    public void setFilterClass(String filterClass) {
        getDelegate().setFilterClass(filterClass);
    }

    public boolean isRunAsFilter() {
        return getDelegate().isRunAsFilter();
    }

    public void setRunAsFilter(boolean runAsFilter) {
        getDelegate().setRunAsFilter(runAsFilter);
    }

    public boolean isActivateOnlyIfJaxrsSupported() {
        return getDelegate().isActivateOnlyIfJaxrsSupported();
    }

    public void setActivateOnlyIfJaxrsSupported(boolean activateOnlyIfJaxrsSupported) {
        getDelegate().setActivateOnlyIfJaxrsSupported(activateOnlyIfJaxrsSupported);
    }

    public abstract static class Portable implements AbstractPortableFeature {
        private static final boolean SWAGGER_JAXRS_AVAILABLE;

        static {
            SWAGGER_JAXRS_AVAILABLE = isSwaggerJaxRsAvailable();
        }

        protected boolean licenseWasSet;
        private boolean runAsFilter;
        private boolean activateOnlyIfJaxrsSupported;
        private String resourcePackage;
        private String version;
        private String basePath;
        private String title;
        private String description;
        private String contact;
        private String license;
        private String licenseUrl;
        private String termsOfServiceUrl;
        private String filterClass;

        private static boolean isSwaggerJaxRsAvailable() {
            try {
                Class.forName("io.swagger.jaxrs.DefaultParameterExtension");
                return true;
            } catch (Throwable ex) {
                return false;
            }
        }

        @Override
        public void initialize(Server server, Bus bus) {
            if (!activateOnlyIfJaxrsSupported || SWAGGER_JAXRS_AVAILABLE) {
                calculateDefaultResourcePackage(server);
                calculateDefaultBasePath(server);
                addSwaggerResource(server, bus);

                doInitializeProvider(server.getEndpoint(), bus);
                bus.setProperty("swagger.service.description.available", "true");
            }
        }

        protected abstract void addSwaggerResource(Server server, Bus bus);

        protected abstract void setBasePathByAddress(String address);

        void calculateDefaultResourcePackage(Server server) {
            if (!StringUtils.isEmpty(getResourcePackage())) {
                return;
            }
            JAXRSServiceFactoryBean serviceFactoryBean =
                    (JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
            List<ClassResourceInfo> resourceInfos = serviceFactoryBean.getClassResourceInfo();

            if (resourceInfos.size() == 1) {
                setResourcePackage(resourceInfos.get(0).getServiceClass().getPackage().getName());
            } else {
                List<Class<?>> serviceClasses = new ArrayList<>(resourceInfos.size());
                for (ClassResourceInfo cri : resourceInfos) {
                    serviceClasses.add(cri.getServiceClass());
                }
                String sharedPackage = PackageUtils.getSharedPackageName(serviceClasses);
                if (!StringUtils.isEmpty(sharedPackage)) {
                    setResourcePackage(sharedPackage);
                }
            }
        }

        protected void calculateDefaultBasePath(Server server) {
            if (getBasePath() == null || getBasePath().length() == 0) {
                String address = server.getEndpoint().getEndpointInfo().getAddress();
                setBasePathByAddress(address);
            }
        }

        public String getResourcePackage() {
            return resourcePackage;
        }
        public void setResourcePackage(String resourcePackage) {
            this.resourcePackage = resourcePackage;
        }
        public String getVersion() {
            return version;
        }
        public void setVersion(String version) {
            this.version = version;
        }
        public String getBasePath() {
            return basePath;
        }
        public void setBasePath(String basePath) {
            this.basePath = basePath;
        }
        public String getTitle() {
            return title;
        }
        public void setTitle(String title) {
            this.title = title;
        }
        public String getDescription() {
            return description;
        }
        public void setDescription(String description) {
            this.description = description;
        }
        public String getContact() {
            return contact;
        }
        public void setContact(String contact) {
            this.contact = contact;
        }
        public String getLicense() {
            return license;
        }
        public void setLicense(String license) {
            this.licenseWasSet = true;
            this.license = license;
        }
        public String getLicenseUrl() {
            return licenseUrl;
        }
        public void setLicenseUrl(String licenseUrl) {
            this.licenseUrl = licenseUrl;
        }
        public String getTermsOfServiceUrl() {
            return termsOfServiceUrl;
        }
        public void setTermsOfServiceUrl(String termsOfServiceUrl) {
            this.termsOfServiceUrl = termsOfServiceUrl;
        }
        public String getFilterClass() {
            return filterClass;
        }
        public void setFilterClass(String filterClass) {
            this.filterClass = filterClass;
        }

        public boolean isRunAsFilter() {
            return runAsFilter;
        }
        public void setRunAsFilter(boolean runAsFilter) {
            this.runAsFilter = runAsFilter;
        }

        public boolean isActivateOnlyIfJaxrsSupported() {
            return activateOnlyIfJaxrsSupported;
        }

        public void setActivateOnlyIfJaxrsSupported(boolean activateOnlyIfJaxrsSupported) {
            this.activateOnlyIfJaxrsSupported = activateOnlyIfJaxrsSupported;
        }
    }

}
