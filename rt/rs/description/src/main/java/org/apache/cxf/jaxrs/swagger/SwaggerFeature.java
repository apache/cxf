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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import com.wordnik.swagger.jaxrs.config.BeanConfig;
import com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider;
import com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON;
import com.wordnik.swagger.jaxrs.listing.ResourceListingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;

public class SwaggerFeature extends AbstractFeature {
    
    private String resourcePackage;
    private String version = "1.0.0";
    private String basePath;
    private String title = "Sample REST Application";
    private String description = "The Application";
    private String contact = "committer@apache.org";
    private String license = "Apache 2.0 License";
    private String licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html";
    private boolean scan = true;
    private boolean runAsFilter;
    
    @Override
    public void initialize(Server server, Bus bus) {
        calculateDefaultResourcePackage(server);
        calculateDefaultBasePath(server);
        ApiListingResourceJSON apiListingResource = new ApiListingResourceJSON();
        if (!runAsFilter) {
            List<Object> serviceBeans = new ArrayList<Object>();
            serviceBeans.add(apiListingResource);
            ((JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName())).
                setResourceClassesFromBeans(serviceBeans);
        }
        List<Object> providers = new ArrayList<Object>();
        if (runAsFilter) {
            providers.add(new SwaggerContainerRequestFilter(apiListingResource));
        }
        providers.add(new ResourceListingProvider());
        providers.add(new ApiDeclarationProvider());
        ((ServerProviderFactory)server.getEndpoint().get(
                ServerProviderFactory.class.getName())).setUserProviders(providers);
        
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setResourcePackage(getResourcePackage());
        beanConfig.setVersion(getVersion());
        beanConfig.setBasePath(getBasePath());
        beanConfig.setTitle(getTitle());
        beanConfig.setDescription(getDescription());
        beanConfig.setContact(getContact());
        beanConfig.setLicense(getLicense());
        beanConfig.setLicenseUrl(getLicenseUrl());
        beanConfig.setScan(isScan());
        initializeProvider(server.getEndpoint(), bus);
    }
    private void calculateDefaultResourcePackage(Server server) {
        JAXRSServiceFactoryBean serviceFactoryBean = 
            (JAXRSServiceFactoryBean)server.getEndpoint().get(JAXRSServiceFactoryBean.class.getName());
        AbstractResourceInfo resourceInfo = serviceFactoryBean.getClassResourceInfo().get(0);
        
        if ((resourceInfo != null) 
            && (getResourcePackage() == null || getResourcePackage().length() == 0)) {
            setResourcePackage(resourceInfo.getServiceClass().getPackage().getName());
        }
    }
    
    private void calculateDefaultBasePath(Server server) {
        if (getBasePath() == null || getBasePath().length() == 0) {
            String address = server.getEndpoint().getEndpointInfo().getAddress();
            setBasePath(address);
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
        this.license = license;
    }
    public String getLicenseUrl() {
        return licenseUrl;
    }
    public void setLicenseUrl(String licenseUrl) {
        this.licenseUrl = licenseUrl;
    }
    public boolean isScan() {
        return scan;
    }
    public void setScan(boolean scan) {
        this.scan = scan;
    }

    public boolean isRunAsFilter() {
        return runAsFilter;
    }
    public void setRunAsFilter(boolean runAsFilter) {
        this.runAsFilter = runAsFilter;
    }

    @PreMatching
    private static class SwaggerContainerRequestFilter implements ContainerRequestFilter {
        private static final String APIDOCS_LISTING_PATH = "api-docs";
        private static final Pattern APIDOCS_RESOURCE_PATH = Pattern.compile(APIDOCS_LISTING_PATH + "(/.+)");
        
        private ApiListingResourceJSON apiListingResource;
        @Context
        private MessageContext mc;
        public SwaggerContainerRequestFilter(ApiListingResourceJSON apiListingResource) {
            this.apiListingResource = apiListingResource;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH)) {
                Response r = 
                    apiListingResource.resourceListing(null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                requestContext.abortWith(r);
            } else {
                final Matcher matcher = APIDOCS_RESOURCE_PATH.matcher(ui.getPath());
                
                if (matcher.find()) {
                    Response r = 
                        apiListingResource.apiDeclaration(matcher.group(1), 
                            null, mc.getServletConfig(), mc.getHttpHeaders(), ui);
                    requestContext.abortWith(r);                
                }
            }
        }
        
    }
}
