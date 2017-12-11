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
package org.apache.cxf.jaxrs.openapi;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Application;

import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.model.ApplicationInfo;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.swagger.SwaggerUiSupport;

import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

@Provider(value = Type.Feature, scope = Scope.Server)
public class OpenApiFeature extends AbstractFeature implements SwaggerUiSupport {
    private String version;
    private String title;
    private String description;
    private String contactName;
    private String contactEmail;
    private String contactUrl;
    private String license;
    private String licenseUrl;
    private String termsOfServiceUrl;
    // Read all operations also with no @Operation
    private boolean readAllResources = true; 
    // Scan all JAX-RS resources automatically
    private boolean scan = true;
    private boolean prettyPrint = true;
    private boolean runAsFilter;
    private Collection<String> ignoredRoutes;
    private Set<String> resourcePackages;
    private Set<String> resourceClasses;
    private String filterClass;

    private Boolean supportSwaggerUi;
    private String swaggerUiVersion;
    private String swaggerUiMavenGroupAndArtifact;
    private Map<String, String> swaggerUiMediaTypes;

    protected static class DefaultApplication extends Application {
        private final Set<Class<?>> serviceClasses;
        
        DefaultApplication(Set<Class<?>> serviceClasses) {
            this.serviceClasses = serviceClasses;
        }
        @Override
        public Set<Class<?>> getClasses() {
            return serviceClasses;
        }
    }

    @Override
    public void initialize(Server server, Bus bus) {
        final JAXRSServiceFactoryBean sfb = (JAXRSServiceFactoryBean)server
            .getEndpoint()
            .get(JAXRSServiceFactoryBean.class.getName());

        final ServerProviderFactory factory = (ServerProviderFactory)server
            .getEndpoint()
            .get(ServerProviderFactory.class.getName());

        final OpenAPI oas = new OpenAPI().info(getInfo());
        
        if (isScan()) {
            resourcePackages = merge(resourcePackages, scanResourcePackages(sfb));
        }
        
        // Read configuration from properties first (openapi-configuration.json)
        final SwaggerConfiguration config = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(isPrettyPrint())
            .readAllResources(isReadAllResources())
            .ignoredRoutes(getIgnoredRoutes())
            .filterClass(filterClass)
            .resourceClasses(getResourceClasses())
            .resourcePackages(resourcePackages);

        final GenericOpenApiContextBuilder<?> openApiConfiguration = new JaxrsOpenApiContextBuilder<>()
            .application(getApplicationOrDefault(server, factory, sfb, bus))
            .openApiConfiguration(config);

        try {
            openApiConfiguration.buildContext(true);
        } catch (OpenApiConfigurationException ex) {
            throw new RuntimeException("Unable to initialize OpenAPI context", ex);
        }
        
        registerOpenApiResources(sfb, config);
        registerSwaggerUiResources(sfb, factory, bus);
    }

    public boolean isScan() {
        return scan;
    }

    public void setScan(boolean scan) {
        this.scan = scan;
    }

    public String getFilterClass() {
        return filterClass;
    }

    public void setFilterClass(String filterClass) {
        this.filterClass = filterClass;
    }
    
    public Set<String> getResourcePackages() {
        return resourcePackages;
    }
    
    public void setResourcePackages(Set<String> resourcePackages) {
        this.resourcePackages = (resourcePackages == null) ? null : new HashSet<>(resourcePackages);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactUrl() {
        return contactUrl;
    }

    public void setContactUrl(String contactUrl) {
        this.contactUrl = contactUrl;
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

    public String getTermsOfServiceUrl() {
        return termsOfServiceUrl;
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        this.termsOfServiceUrl = termsOfServiceUrl;
    }

    public boolean isReadAllResources() {
        return readAllResources;
    }

    public void setReadAllResources(boolean readAllResources) {
        this.readAllResources = readAllResources;
    }

    public Set<String> getResourceClasses() {
        return resourceClasses;
    }

    public void setResourceClasses(Set<String> resourceClasses) {
        this.resourceClasses = (resourceClasses == null) ? null : new HashSet<>(resourceClasses);
    }

    public Collection<String> getIgnoredRoutes() {
        return ignoredRoutes;
    }

    public void setIgnoredRoutes(Collection<String> ignoredRoutes) {
        this.ignoredRoutes = (ignoredRoutes == null) ? null : new HashSet<>(ignoredRoutes);
    }

    public boolean isPrettyPrint() {
        return prettyPrint;
    }

    public void setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }
    
    public boolean isRunAsFilter() {
        return runAsFilter;
    }
    
    @Override
    public Boolean isSupportSwaggerUi() {
        return supportSwaggerUi;
    }

    public void setSupportSwaggerUi(Boolean supportSwaggerUi) {
        this.supportSwaggerUi = supportSwaggerUi;
    }

    public String getSwaggerUiVersion() {
        return swaggerUiVersion;
    }

    public void setSwaggerUiVersion(String swaggerUiVersion) {
        this.swaggerUiVersion = swaggerUiVersion;
    }

    public String getSwaggerUiMavenGroupAndArtifact() {
        return swaggerUiMavenGroupAndArtifact;
    }

    public void setSwaggerUiMavenGroupAndArtifact(
            String swaggerUiMavenGroupAndArtifact) {
        this.swaggerUiMavenGroupAndArtifact = swaggerUiMavenGroupAndArtifact;
    }

    public Map<String, String> getSwaggerUiMediaTypes() {
        return swaggerUiMediaTypes;
    }

    public void setSwaggerUiMediaTypes(Map<String, String> swaggerUiMediaTypes) {
        this.swaggerUiMediaTypes = swaggerUiMediaTypes;
    }
    
    @Override
    public String findSwaggerUiRoot() {
        return SwaggerUi.findSwaggerUiRoot(swaggerUiMavenGroupAndArtifact, swaggerUiVersion);
    }

    protected void registerOpenApiResources(JAXRSServiceFactoryBean sfb, OpenAPIConfiguration config) {
        sfb.setResourceClassesFromBeans(Arrays.asList(new OpenApiResource().openApiConfiguration(config)));
    }

    protected void registerSwaggerUiResources(JAXRSServiceFactoryBean sfb, ServerProviderFactory factory, Bus bus) {
        final Properties swaggerProps = getSwaggerProperties(bus);
        final Registration swaggerUiRegistration = getSwaggerUi(bus, swaggerProps, isRunAsFilter());
        
        if (!isRunAsFilter()) {
            sfb.setResourceClassesFromBeans(swaggerUiRegistration.getResources());
        } 

        factory.setUserProviders(swaggerUiRegistration.getProviders());
    }

    protected Properties getSwaggerProperties(Bus bus) {
        // TODO: Read from OpenAPI properties
        return new Properties();
    }

    /**
     * Detects the application (if present) or creates the default application (in case the
     * scan is disabled)
     */
    protected Application getApplicationOrDefault(Server server, ServerProviderFactory factory, 
            JAXRSServiceFactoryBean sfb, Bus bus) {

        ApplicationInfo appInfo = null;
        if (!isScan()) {
            appInfo = factory.getApplicationProvider();
            
            if (appInfo == null) {
                Set<Class<?>> serviceClasses = new HashSet<>();
                for (ClassResourceInfo cri : sfb.getClassResourceInfo()) {
                    serviceClasses.add(cri.getServiceClass());
                }
                appInfo = new ApplicationInfo(new DefaultApplication(serviceClasses), bus);
                server.getEndpoint().put(Application.class.getName(), appInfo);
            }
        }
        
        return (appInfo == null) ? null : appInfo.getProvider();
    }

    /**
     * The info will be used only if there is no @OpenAPIDefinition annotation is present. 
     */
    private Info getInfo() {
        return new Info()
            .title(getTitle())
            .version(getVersion())
            .description(getDescription())
            .termsOfService(getTermsOfServiceUrl())
            .contact(new Contact()
                .name(getContactName())
                .email(getContactEmail())
                .url(getContactUrl()))
            .license(new License()
                .name(getLicense())
                .url(getLicenseUrl()));
    }
        
    private Collection<String> scanResourcePackages(JAXRSServiceFactoryBean sfb) {
        return sfb
            .getClassResourceInfo()
            .stream()
            .map(cri -> cri.getServiceClass().getPackage().getName())
            .collect(Collectors.toSet());
    }
    
    private Set<String> merge(Set<String> destination, Collection<String> source) {
        final Set<String> dst = (destination == null) ? new HashSet<>() : destination;
        dst.addAll(source);
        return dst;
    }
}
