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
package org.apache.cxf.jaxrs.microprofile.openapi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Application;
import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.common.openapi.DefaultApplicationFactory;
import org.apache.cxf.jaxrs.common.openapi.SwaggerProperties;
import org.apache.cxf.jaxrs.model.AbstractResourceInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiSupport;
import org.apache.geronimo.microprofile.openapi.config.GeronimoOpenAPIConfig;
import org.apache.geronimo.microprofile.openapi.impl.model.ContactImpl;
import org.apache.geronimo.microprofile.openapi.impl.model.InfoImpl;
import org.apache.geronimo.microprofile.openapi.impl.model.LicenseImpl;
import org.apache.geronimo.microprofile.openapi.impl.model.OpenAPIImpl;
import org.apache.geronimo.microprofile.openapi.impl.processor.AnnotationProcessor;
import org.apache.geronimo.microprofile.openapi.impl.processor.reflect.ClassElement;
import org.apache.geronimo.microprofile.openapi.impl.processor.reflect.MethodElement;
import org.apache.geronimo.microprofile.openapi.impl.processor.spi.NamingStrategy;
import org.eclipse.microprofile.openapi.models.OpenAPI;


@Provider(value = Type.Feature, scope = Scope.Server)
public class OpenApiFeature extends DelegatingFeature<OpenApiFeature.Portable>
        implements SwaggerUiSupport, SwaggerProperties {

    public OpenApiFeature() {
        super(new Portable());
    }

    public boolean isScan() {
        return delegate.isScan();
    }

    public void setScan(boolean scan) {
        delegate.setScan(scan);
    }

    public String getFilterClass() {
        return delegate.getFilterClass();
    }

    public void setFilterClass(String filterClass) {
        delegate.setFilterClass(filterClass);
    }
    
    public Set<String> getResourcePackages() {
        return delegate.getResourcePackages();
    }
    
    public void setResourcePackages(Set<String> resourcePackages) {
        delegate.setResourcePackages(resourcePackages);
    }

    public String getVersion() {
        return delegate.getVersion();
    }

    public void setVersion(String version) {
        delegate.setVersion(version);
    }

    public String getTitle() {
        return delegate.getTitle();
    }

    public void setTitle(String title) {
        delegate.setTitle(title);
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public void setDescription(String description) {
        delegate.setDescription(description);
    }

    public String getContactName() {
        return delegate.getContactName();
    }

    public void setContactName(String contactName) {
        delegate.setContactName(contactName);
    }

    public String getContactEmail() {
        return delegate.getContactEmail();
    }

    public void setContactEmail(String contactEmail) {
        delegate.setContactEmail(contactEmail);
    }

    public String getContactUrl() {
        return delegate.getContactUrl();
    }

    public void setContactUrl(String contactUrl) {
        delegate.setContactUrl(contactUrl);
    }

    public String getLicense() {
        return delegate.getLicense();
    }

    public void setLicense(String license) {
        delegate.setLicense(license);
    }

    public String getLicenseUrl() {
        return delegate.getLicenseUrl();
    }

    public void setLicenseUrl(String licenseUrl) {
        delegate.setLicenseUrl(licenseUrl);
    }

    public String getTermsOfServiceUrl() {
        return delegate.getTermsOfServiceUrl();
    }

    public void setTermsOfServiceUrl(String termsOfServiceUrl) {
        delegate.setTermsOfServiceUrl(termsOfServiceUrl);
    }

    public boolean isReadAllResources() {
        return delegate.isReadAllResources();
    }

    public void setReadAllResources(boolean readAllResources) {
        delegate.setReadAllResources(readAllResources);
    }

    public Set<String> getResourceClasses() {
        return delegate.getResourceClasses();
    }

    public void setResourceClasses(Set<String> resourceClasses) {
        delegate.setResourceClasses(resourceClasses);
    }

    public Collection<String> getIgnoredRoutes() {
        return delegate.getIgnoredRoutes();
    }

    public void setIgnoredRoutes(Collection<String> ignoredRoutes) {
        delegate.setIgnoredRoutes(ignoredRoutes);
    }

    public boolean isPrettyPrint() {
        return delegate.isPrettyPrint();
    }

    public void setPrettyPrint(boolean prettyPrint) {
        delegate.setPrettyPrint(prettyPrint);
    }
    
    public boolean isRunAsFilter() {
        return delegate.isRunAsFilter();
    }
    
    @Override
    public Boolean isSupportSwaggerUi() {
        return delegate.isSupportSwaggerUi();
    }

    public void setSupportSwaggerUi(Boolean supportSwaggerUi) {
        delegate.setSupportSwaggerUi(supportSwaggerUi);
    }

    public String getSwaggerUiVersion() {
        return delegate.getSwaggerUiVersion();
    }

    public void setSwaggerUiVersion(String swaggerUiVersion) {
        delegate.setSwaggerUiVersion(swaggerUiVersion);
    }

    public String getSwaggerUiMavenGroupAndArtifact() {
        return delegate.getSwaggerUiMavenGroupAndArtifact();
    }

    public void setSwaggerUiMavenGroupAndArtifact(
            String swaggerUiMavenGroupAndArtifact) {
        delegate.setSwaggerUiMavenGroupAndArtifact(swaggerUiMavenGroupAndArtifact);
    }

    @Override
    public Map<String, String> getSwaggerUiMediaTypes() {
        return delegate.getSwaggerUiMediaTypes();
    }

    public void setSwaggerUiMediaTypes(Map<String, String> swaggerUiMediaTypes) {
        delegate.setSwaggerUiMediaTypes(swaggerUiMediaTypes);
    }

    public String getConfigLocation() {
        return delegate.getConfigLocation();
    }

    public void setConfigLocation(String configLocation) {
        delegate.setConfigLocation(configLocation);
    }

    public String getPropertiesLocation() {
        return delegate.getPropertiesLocation();
    }

    public void setPropertiesLocation(String propertiesLocation) {
        delegate.setPropertiesLocation(propertiesLocation);
    }

    public void setRunAsFilter(boolean runAsFilter) {
        delegate.setRunAsFilter(runAsFilter);
    }

    public void setScanKnownConfigLocations(boolean scanKnownConfigLocations) {
        delegate.setScanKnownConfigLocations(scanKnownConfigLocations);
    }
    
    public boolean isScanKnownConfigLocations() {
        return delegate.isScanKnownConfigLocations();
    }
    
    public void setSwaggerUiConfig(final SwaggerUiConfig swaggerUiConfig) {
        delegate.setSwaggerUiConfig(swaggerUiConfig);
    }
    
    @Override
    public SwaggerUiConfig getSwaggerUiConfig() {
        return delegate.getSwaggerUiConfig();
    }

    @Override
    public String findSwaggerUiRoot() {
        return delegate.findSwaggerUiRoot();
    }
    
    protected Properties getUserProperties(final Map<String, Object> userDefinedOptions) {
        return delegate.getUserProperties(userDefinedOptions);
    }

    protected void registerOpenApiResources(
            final JAXRSServiceFactoryBean sfb,
            final OpenAPI openApiDefinition) {

        delegate.registerOpenApiResources(sfb, openApiDefinition);
    }

    protected void registerSwaggerUiResources(JAXRSServiceFactoryBean sfb, Properties properties,
            ServerProviderFactory factory, Bus bus) {
        
        delegate.registerSwaggerUiResources(sfb, properties, factory, bus);
    }

    public static class Portable implements AbstractPortableFeature, SwaggerUiSupport, SwaggerProperties {
        private static final Logger LOG = LogUtils.getL7dLogger(OpenApiFeature.class);

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

        // Allows to pass the configuration location, usually openapi-configuration.json
        // or openapi-configuration.yml file.
        private String configLocation;
        // Allows to pass the properties location, by default swagger.properties
        private String propertiesLocation = DEFAULT_PROPS_LOCATION;
        // Allows to disable automatic scan of known configuration locations (enabled by default)
        private boolean scanKnownConfigLocations = true;
        // Swagger UI configuration parameters (to be passed as query string).
        private SwaggerUiConfig swaggerUiConfig;

        @Override
        public void initialize(Server server, Bus bus) {
            final JAXRSServiceFactoryBean sfb = (JAXRSServiceFactoryBean)server
                    .getEndpoint()
                    .get(JAXRSServiceFactoryBean.class.getName());

            final ServerProviderFactory factory = (ServerProviderFactory)server
                    .getEndpoint()
                    .get(ServerProviderFactory.class.getName());

            final Set<String> packages = new HashSet<>();
            if (resourcePackages != null) {
                packages.addAll(resourcePackages);
            }

            final Application application = DefaultApplicationFactory.createApplicationOrDefault(server, factory,
                    sfb, bus, resourcePackages, isScan());

            final AnnotationProcessor processor = new AnnotationProcessor(GeronimoOpenAPIConfig.create(),
                    new NamingStrategy.Http(), null /* default JsonReaderFactory */);

            final OpenAPIImpl api = new OpenAPIImpl();

            if (isScan()) {
                packages.addAll(scanResourcePackages(sfb));
            }

            final Set<Class<?>> resources = new HashSet<>();
            if (application != null) {
                processor.processApplication(api, new ClassElement(application.getClass()));
                LOG.fine("Processed application " + application);

                if (application.getClasses() != null) {
                    resources.addAll(application.getClasses());
                }
            }

            resources.addAll(sfb
                    .getClassResourceInfo()
                    .stream()
                    .map(AbstractResourceInfo::getServiceClass)
                    .filter(cls -> filterByPackage(cls, packages))
                    .filter(cls -> filterByClassName(cls, resourceClasses))
                    .collect(Collectors.toSet()));

            if (!resources.isEmpty()) {
                final String binding = (application == null) ? ""
                        : processor.getApplicationBinding(application.getClass());

                resources
                        .stream()
                        .peek(c -> LOG.info("Processing class " + c.getName()))
                        .forEach(c -> processor.processClass(binding, api, new ClassElement(c),
                                Stream.of(c.getMethods()).map(MethodElement::new)));
            } else {
                LOG.warning("No resource classes registered, the OpenAPI will not contain any endpoints.");
            }

            Properties swaggerProps = getSwaggerProperties(propertiesLocation, bus);
            if (api.getInfo() == null) {
                api.setInfo(getInfo(swaggerProps));
            }

            registerOpenApiResources(sfb, api);
            registerSwaggerUiResources(sfb, swaggerProps, factory, bus);
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

        @Override
        public Map<String, String> getSwaggerUiMediaTypes() {
            return swaggerUiMediaTypes;
        }

        public void setSwaggerUiMediaTypes(Map<String, String> swaggerUiMediaTypes) {
            this.swaggerUiMediaTypes = swaggerUiMediaTypes;
        }

        public String getConfigLocation() {
            return configLocation;
        }

        public void setConfigLocation(String configLocation) {
            this.configLocation = configLocation;
        }

        public String getPropertiesLocation() {
            return propertiesLocation;
        }

        public void setPropertiesLocation(String propertiesLocation) {
            this.propertiesLocation = propertiesLocation;
        }

        public void setRunAsFilter(boolean runAsFilter) {
            this.runAsFilter = runAsFilter;
        }

        public void setScanKnownConfigLocations(boolean scanKnownConfigLocations) {
            this.scanKnownConfigLocations = scanKnownConfigLocations;
        }

        public boolean isScanKnownConfigLocations() {
            return scanKnownConfigLocations;
        }

        public void setSwaggerUiConfig(final SwaggerUiConfig swaggerUiConfig) {
            this.swaggerUiConfig = swaggerUiConfig;
        }

        @Override
        public SwaggerUiConfig getSwaggerUiConfig() {
            return swaggerUiConfig;
        }

        @Override
        public String findSwaggerUiRoot() {
            return SwaggerUi.findSwaggerUiRoot(swaggerUiMavenGroupAndArtifact, swaggerUiVersion);
        }

        protected Properties getUserProperties(final Map<String, Object> userDefinedOptions) {
            final Properties properties = new Properties();

            if (userDefinedOptions != null) {
                userDefinedOptions
                        .entrySet()
                        .stream()
                        .filter(entry -> entry.getValue() != null)
                        .forEach(entry -> properties.setProperty(entry.getKey(), entry.getValue().toString()));
            }

            return properties;
        }

        protected void registerOpenApiResources(
                final JAXRSServiceFactoryBean sfb,
                final OpenAPI openApiDefinition) {

            sfb.setResourceClassesFromBeans(Collections.singletonList(new OpenApiEndpoint(openApiDefinition)));
        }

        protected void registerSwaggerUiResources(JAXRSServiceFactoryBean sfb, Properties properties,
                                                  ServerProviderFactory factory, Bus bus) {

            final Registration swaggerUiRegistration = getSwaggerUi(bus, properties, isRunAsFilter());

            if (!isRunAsFilter()) {
                sfb.setResourceClassesFromBeans(swaggerUiRegistration.getResources());
            }

            factory.setUserProviders(swaggerUiRegistration.getProviders());
        }

        /**
         * The info will be used only if there is no @OpenAPIDefinition annotation is present.
         */
        private org.eclipse.microprofile.openapi.models.info.Info getInfo(final Properties properties) {
            org.eclipse.microprofile.openapi.models.info.Info info = new InfoImpl()
                    .title(getOrFallback(getTitle(), properties, TITLE_PROPERTY))
                    .version(getOrFallback(getVersion(), properties, VERSION_PROPERTY))
                    .description(getOrFallback(getDescription(), properties, DESCRIPTION_PROPERTY))
                    .termsOfService(getOrFallback(getTermsOfServiceUrl(), properties, TERMS_URL_PROPERTY))
                    .contact(new ContactImpl()
                            .name(getOrFallback(getContactName(), properties, CONTACT_PROPERTY))
                            .email(getContactEmail())
                            .url(getContactUrl()));

            String licenseName = getOrFallback(getLicense(), properties, LICENSE_PROPERTY);
            if (licenseName != null) {
                info = info.license(new LicenseImpl()
                        .name(getOrFallback(getLicense(), properties, LICENSE_PROPERTY))
                        .url(getOrFallback(getLicenseUrl(), properties, LICENSE_URL_PROPERTY)));
            }
            return info;
        }

        private String getOrFallback(String value, Properties properties, String property) {
            if (value == null && properties != null) {
                return properties.getProperty(property);
            } else {
                return value;
            }
        }

        private Collection<String> scanResourcePackages(JAXRSServiceFactoryBean sfb) {
            return sfb
                    .getClassResourceInfo()
                    .stream()
                    .map(cri -> cri.getServiceClass().getPackage().getName())
                    .collect(Collectors.toSet());
        }

        private static boolean filterByPackage(final Class<?> cls, final Set<String> packages) {
            return (packages == null || packages.isEmpty())
                    || packages.stream().anyMatch(pkg -> cls.getPackage().getName().startsWith(pkg));
        }

        private static boolean filterByClassName(final Class<?> cls, final Set<String> classes) {
            return (classes == null || classes.isEmpty())
                    || classes.stream().anyMatch(cls.getName()::equalsIgnoreCase);
        }
    }
}
