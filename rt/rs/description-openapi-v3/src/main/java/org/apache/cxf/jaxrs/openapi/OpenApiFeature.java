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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.apache.cxf.Bus;
import org.apache.cxf.annotations.Provider;
import org.apache.cxf.annotations.Provider.Scope;
import org.apache.cxf.annotations.Provider.Type;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.jaxrs.JAXRSServiceFactoryBean;
import org.apache.cxf.jaxrs.common.openapi.DefaultApplicationFactory;
import org.apache.cxf.jaxrs.common.openapi.SwaggerProperties;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiConfig;
import org.apache.cxf.jaxrs.swagger.ui.SwaggerUiSupport;


import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.GenericOpenApiContextBuilder;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.integration.api.OpenAPIConfiguration;
import io.swagger.v3.oas.integration.api.OpenApiContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;


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

    public void setSwaggerUiMavenGroupAndArtifact(String swaggerUiMavenGroupAndArtifact) {
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

    public Map<String, SecurityScheme> getSecurityDefinitions() {
        return delegate.getSecurityDefinitions();
    }

    public void setSecurityDefinitions(Map<String, SecurityScheme> securityDefinitions) {
        delegate.setSecurityDefinitions(securityDefinitions);
    }

    public OpenApiCustomizer getCustomizer() {
        return delegate.getCustomizer();
    }

    public void setCustomizer(OpenApiCustomizer customizer) {
        delegate.setCustomizer(customizer);
    }

    public void setScanKnownConfigLocations(boolean scanKnownConfigLocations) {
        delegate.setScanKnownConfigLocations(scanKnownConfigLocations);
    }

    public boolean isScanKnownConfigLocations() {
        return delegate.isScanKnownConfigLocations();
    }

    public void setSwaggerUiConfig(SwaggerUiConfig swaggerUiConfig) {
        delegate.setSwaggerUiConfig(swaggerUiConfig);
    }

    public void setUseContextBasedConfig(boolean useContextBasedConfig) {
        delegate.setUseContextBasedConfig(useContextBasedConfig);
    }

    public boolean isUseContextBasedConfig() {
        return delegate.isUseContextBasedConfig();
    }
    
    public String getScannerClass() {
        return delegate.getScannerClass();
    }

    public void setScannerClass(String scannerClass) {
        delegate.setScannerClass(scannerClass);
    }

    @Override
    public SwaggerUiConfig getSwaggerUiConfig() {
        return delegate.getSwaggerUiConfig();
    }

    @Override
    public String findSwaggerUiRoot() {
        return delegate.findSwaggerUiRoot();
    }

    public Properties getUserProperties(Map<String, Object> userDefinedOptions) {
        return delegate.getUserProperties(userDefinedOptions);
    }

    public void registerOpenApiResources(JAXRSServiceFactoryBean sfb, Set<String> packages,
                                         OpenAPIConfiguration config) {
        delegate.registerOpenApiResources(sfb, packages, config);
    }

    public void registerServletConfigProvider(ServerProviderFactory factory) {
        delegate.registerServletConfigProvider(factory);
    }

    public void registerSwaggerUiResources(JAXRSServiceFactoryBean sfb, Properties properties,
                                           ServerProviderFactory factory, Bus bus) {
        delegate.registerSwaggerUiResources(sfb, properties, factory, bus);
    }

    public Info getInfo(Properties properties) {
        return delegate.getInfo(properties);
    }

    public String getOrFallback(String value, Properties properties, String property) {
        return delegate.getOrFallback(value, properties, property);
    }

    public Boolean getOrFallback(Boolean value, Properties properties, String property) {
        return delegate.getOrFallback(value, properties, property);
    }

    public Set<String> getOrFallback(Set<String> collection, Properties properties, String property) {
        return delegate.getOrFallback(collection, properties, property);
    }

    public Collection<String> scanResourcePackages(JAXRSServiceFactoryBean sfb) {
        return delegate.scanResourcePackages(sfb);
    }

    public static Properties combine(Properties primary, Properties secondary) {
        return Portable.combine(primary, secondary);
    }

    public static void setOrReplace(Properties source, Properties destination) {
        Portable.setOrReplace(source, destination);
    }

    public static Optional<Components> registerComponents(Map<String, SecurityScheme> securityDefinitions) {
        return Portable.registerComponents(securityDefinitions);
    }

    public BaseOpenApiResource createOpenApiResource() {
        return delegate.createOpenApiResource();
    }

    public static class Portable implements AbstractPortableFeature, SwaggerUiSupport, SwaggerProperties {
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

        // Additional components
        private Map<String, SecurityScheme> securityDefinitions;
        private OpenApiCustomizer customizer;

        // Allows to pass the configuration location, usually openapi-configuration.json
        // or openapi-configuration.yml file.
        private String configLocation;
        // Allows to pass the properties location, by default swagger.properties
        private String propertiesLocation = DEFAULT_PROPS_LOCATION;
        // Allows to disable automatic scan of known configuration locations (enabled by default)
        private boolean scanKnownConfigLocations = true;
        // Swagger UI configuration parameters (to be passed as query string).
        private SwaggerUiConfig swaggerUiConfig;
        // Generates the Swagger Context ID (instead of using the default one). It is
        // necessary when more than one JAXRS Server Factory Bean or OpenApiFeature instance
        // are co-located in the same application.
        private boolean useContextBasedConfig;
        private String ctxId;
        // The API Scanner class to use 
        private String scannerClass;

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

            // Generate random Context ID for Swagger
            if (useContextBasedConfig) {
                ctxId = UUID.randomUUID().toString();
            }

            Properties swaggerProps = null;
            GenericOpenApiContextBuilder<?> openApiConfiguration;
            final Application application = DefaultApplicationFactory.createApplicationOrDefault(server, factory,
                    sfb, bus, resourcePackages, isScan());

            String defaultConfigLocation = getConfigLocation();
            if (scanKnownConfigLocations && StringUtils.isEmpty(defaultConfigLocation)) {
                defaultConfigLocation = OpenApiDefaultConfigurationScanner.locateDefaultConfiguration().orElse(null);
            }

            if (StringUtils.isEmpty(defaultConfigLocation)) {
                swaggerProps = getSwaggerProperties(propertiesLocation, bus);

                if (isScan()) {
                    packages.addAll(scanResourcePackages(sfb));
                }

                final OpenAPI oas = new OpenAPI().info(getInfo(swaggerProps));
                registerComponents(securityDefinitions).ifPresent(oas::setComponents);

                final SwaggerConfiguration config = new SwaggerConfiguration()
                        .openAPI(oas)
                        .prettyPrint(getOrFallback(isPrettyPrint(), swaggerProps, PRETTY_PRINT_PROPERTY))
                        .readAllResources(isReadAllResources())
                        .ignoredRoutes(getIgnoredRoutes())
                        .filterClass(getOrFallback(getFilterClass(), swaggerProps, FILTER_CLASS_PROPERTY))
                        .resourceClasses(getResourceClasses())
                        .resourcePackages(getOrFallback(packages, swaggerProps, RESOURCE_PACKAGE_PROPERTY));
                
                if (!StringUtils.isEmpty(getScannerClass())) {
                    config.setScannerClass(getScannerClass());
                }

                openApiConfiguration = new JaxrsOpenApiContextBuilder<>()
                        .application(application)
                        .openApiConfiguration(config)
                        .ctxId(ctxId); /* will be null if not used */
            } else {
                openApiConfiguration = new JaxrsOpenApiContextBuilder<>()
                        .application(application)
                        .configLocation(defaultConfigLocation)
                        .ctxId(ctxId); /* will be null if not used */
            }

            try {
                final OpenApiContext context = openApiConfiguration.buildContext(true);
                final Properties userProperties = getUserProperties(
                        context
                                .getOpenApiConfiguration()
                                .getUserDefinedOptions());
                registerOpenApiResources(sfb, packages, context.getOpenApiConfiguration());
                registerSwaggerUiResources(sfb, combine(swaggerProps, userProperties), factory, bus);
                registerSwaggerContainerRequestFilter(factory, application, context.getOpenApiConfiguration());

                if (useContextBasedConfig) {
                    registerServletConfigProvider(factory);
                }

                if (customizer != null) {
                    customizer.setApplicationInfo(factory.getApplicationProvider());
                }

                bus.setProperty("openapi.service.description.available", "true");
            } catch (OpenApiConfigurationException ex) {
                throw new RuntimeException("Unable to initialize OpenAPI context", ex);
            }
        }

        private void registerSwaggerContainerRequestFilter(ServerProviderFactory factory, Application application,
                                                           OpenAPIConfiguration config) {
            if (isRunAsFilter()) {
                List<Object> providers = new ArrayList<>();
                BaseOpenApiResource filter = createOpenApiRequestFilter(application).openApiConfiguration(config)
                        .configLocation(configLocation);
                providers.add(filter);
                factory.setUserProviders(providers);
            }
            
            
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

        public Map<String, SecurityScheme> getSecurityDefinitions() {
            return securityDefinitions;
        }

        public void setSecurityDefinitions(Map<String, SecurityScheme> securityDefinitions) {
            this.securityDefinitions = securityDefinitions;
        }

        public OpenApiCustomizer getCustomizer() {
            return customizer;
        }

        public void setCustomizer(OpenApiCustomizer customizer) {
            this.customizer = customizer;
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

        public void setUseContextBasedConfig(final boolean useContextBasedConfig) {
            this.useContextBasedConfig = useContextBasedConfig;
        }

        public boolean isUseContextBasedConfig() {
            return useContextBasedConfig;
        }

        public String getScannerClass() {
            return scannerClass;
        }

        public void setScannerClass(String scannerClass) {
            this.scannerClass = scannerClass;
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
                final Set<String> packages,
                final OpenAPIConfiguration config) {

            if (customizer != null) {
                customizer.setClassResourceInfos(sfb.getClassResourceInfo());
            }

            sfb.setResourceClassesFromBeans(Arrays.asList(
                    createOpenApiResource()
                            .openApiConfiguration(config)
                            .configLocation(configLocation)
                            .resourcePackages(packages)));
        }

        protected void registerServletConfigProvider(ServerProviderFactory factory) {
            factory.setUserProviders(Arrays.asList(new ServletConfigProvider(ctxId)));
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
        private Info getInfo(final Properties properties) {
            final Info info = new Info()
                    .title(getOrFallback(getTitle(), properties, TITLE_PROPERTY))
                    .version(getOrFallback(getVersion(), properties, VERSION_PROPERTY))
                    .description(getOrFallback(getDescription(), properties, DESCRIPTION_PROPERTY))
                    .termsOfService(getOrFallback(getTermsOfServiceUrl(), properties, TERMS_URL_PROPERTY))
                    .contact(new Contact()
                            .name(getOrFallback(getContactName(), properties, CONTACT_PROPERTY))
                            .email(getContactEmail())
                            .url(getContactUrl()))
                    .license(new License()
                            .name(getOrFallback(getLicense(), properties, LICENSE_PROPERTY))
                            .url(getOrFallback(getLicenseUrl(), properties, LICENSE_URL_PROPERTY)));

            if (info.getLicense().getName() == null) {
                info.getLicense().setName(DEFAULT_LICENSE_VALUE);
            }

            if (info.getLicense().getUrl() == null && DEFAULT_LICENSE_VALUE.equals(info.getLicense().getName())) {
                info.getLicense().setUrl(DEFAULT_LICENSE_URL);
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

        private Boolean getOrFallback(Boolean value, Properties properties, String property) {
            Boolean fallback = value;
            if (value == null && properties != null) {
                fallback = PropertyUtils.isTrue(properties.get(PRETTY_PRINT_PROPERTY));
            }

            if (fallback == null) {
                return false;
            }

            return fallback;
        }

        private Set<String> getOrFallback(Set<String> collection, Properties properties, String property) {
            if (collection.isEmpty() && properties != null) {
                final String value = properties.getProperty(property);
                if (!StringUtils.isEmpty(value)) {
                    collection.add(value);
                }
            }

            return collection;
        }

        private Collection<String> scanResourcePackages(JAXRSServiceFactoryBean sfb) {
            return sfb
                    .getClassResourceInfo()
                    .stream()
                    .map(cri -> cri.getServiceClass().getPackage().getName())
                    .collect(Collectors.toSet());
        }

        private static Properties combine(final Properties primary, final Properties secondary) {
            if (primary == null) {
                return secondary;
            } else if (secondary == null) {
                return primary;
            } else {
                final Properties combined = new Properties();
                setOrReplace(secondary, combined);
                setOrReplace(primary, combined);
                return combined;
            }
        }

        private static void setOrReplace(final Properties source, final Properties destination) {
            final Enumeration<?> enumeration = source.propertyNames();
            while (enumeration.hasMoreElements()) {
                final String name = (String)enumeration.nextElement();
                destination.setProperty(name, source.getProperty(name));
            }
        }

        private static Optional<Components> registerComponents(Map<String, SecurityScheme> securityDefinitions) {
            final Components components = new Components();

            boolean hasComponents = false;
            if (securityDefinitions != null && !securityDefinitions.isEmpty()) {
                securityDefinitions.forEach(components::addSecuritySchemes);
                hasComponents |= true;
            }

            return hasComponents ? Optional.of(components) : Optional.empty();
        }

        private BaseOpenApiResource createOpenApiResource() {
            return (customizer == null) ? new OpenApiResource() : new OpenApiCustomizedResource(customizer);
        }

        private BaseOpenApiResource createOpenApiRequestFilter(Application application) {
            return (customizer == null) ? new SwaggerContainerRequestFilter(application)
                    : new CustomizedSwaggerContainerRequestFilter(application, customizer);
        }
    }
    
    @PreMatching
    protected static class SwaggerContainerRequestFilter extends BaseOpenApiResource implements ContainerRequestFilter {

        protected static final String APIDOCS_LISTING_PATH_JSON = "openapi.json";
        protected static final String APIDOCS_LISTING_PATH_YAML = "openapi.yaml";
        
        
        @Context
        protected MessageContext mc;

        private Application app;
        public SwaggerContainerRequestFilter(Application app) {
            this.app = app;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();

            Response response = null;
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON)) {
                try {
                    response = super.getOpenApi(mc.getHttpHeaders(), mc.getServletConfig(), app, ui, "json");
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            } else if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {
                try {
                    response = super.getOpenApi(mc.getHttpHeaders(), mc.getServletConfig(), app, ui, "yaml");
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }

            if (response != null) {
                requestContext.abortWith(response);
            }
        }
    }

    @PreMatching
    protected static class CustomizedSwaggerContainerRequestFilter extends OpenApiCustomizedResource
            implements ContainerRequestFilter {

        protected static final String APIDOCS_LISTING_PATH_JSON = "openapi.json";
        protected static final String APIDOCS_LISTING_PATH_YAML = "openapi.yaml";


        @Context
        protected MessageContext mc;

        private Application app;
        public CustomizedSwaggerContainerRequestFilter(Application app, OpenApiCustomizer customizer) {
            super(customizer);
            this.app = app;
        }

        @Override
        public void filter(ContainerRequestContext requestContext) throws IOException {
            UriInfo ui = mc.getUriInfo();

            Response response = null;
            if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_JSON)) {
                try {
                    response = super.getOpenApi(app, mc.getServletConfig(), mc.getHttpHeaders(), ui, "json");
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            } else if (ui.getPath().endsWith(APIDOCS_LISTING_PATH_YAML)) {
                try {
                    response = super.getOpenApi(app, mc.getServletConfig(), mc.getHttpHeaders(), ui, "yaml");
                } catch (Exception ex) {
                    throw new IOException(ex);
                }
            }

            if (response != null) {
                requestContext.abortWith(response);
            }
        }
    }
}
