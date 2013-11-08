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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ProviderFactory;

public class SwaggerFeature extends AbstractFeature {
    
    private String resourcePackage;
    private String version = "1.0.0";
    private String basePath;
    private String title = "Rest sample app";
    private String description = "This is a app.";
    private String contact = "freeman.fang@gmail.com";
    private String license = "Apache 2.0 License";
    private String licenseUrl = "http://www.apache.org/licenses/LICENSE-2.0.html";
    private boolean scan = true;
    @Override
    public void initialize(Server server, Bus bus) {
        List<Object> serviceBeans = new ArrayList<Object>();
        serviceBeans.add(new com.wordnik.swagger.jaxrs.listing.ApiListingResourceJSON());
        calulateDefaultResourcePackage(bus);
        calulateDefaultBasePath(server);
        ((JAXRSServiceFactoryBean)bus.getProperty(JAXRSServiceFactoryBean.class.getName())).
            setResourceClassesFromBeans(serviceBeans);
        List<Object> providers = new ArrayList<Object>();
        providers.add(new com.wordnik.swagger.jaxrs.listing.ResourceListingProvider());
        providers.add(new com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider());
        ((ProviderFactory)bus.getProperty(ProviderFactory.class.getName())).setUserProviders(providers);
        com.wordnik.swagger.jaxrs.config.BeanConfig beanConfig = new com.wordnik.swagger.jaxrs.config.BeanConfig();
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
    private void calulateDefaultResourcePackage(Bus bus) {
        JAXRSServiceFactoryBean serviceFactoryBean = 
            (JAXRSServiceFactoryBean)bus.getProperty(JAXRSServiceFactoryBean.class.getName());
        AbstractResourceInfo resourceInfo = serviceFactoryBean.getClassResourceInfo().get(0);
        
        if ((resourceInfo != null) 
            && (getResourcePackage() == null || getResourcePackage().length() == 0)) {
            setResourcePackage(resourceInfo.getResourceClass().getPackage().getName());
        }
    }
    
    private void calulateDefaultBasePath(Server server) {
        String address = server.getEndpoint().getEndpointInfo().getAddress();
        if (getBasePath() == null || getBasePath().length() == 0) {
            if (address.startsWith("http")) {
                setBasePath(address + "/api-docs");
            } else {
                setBasePath("http://localhost:8181/cxf" + address + "/api-docs");
            }
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

}
