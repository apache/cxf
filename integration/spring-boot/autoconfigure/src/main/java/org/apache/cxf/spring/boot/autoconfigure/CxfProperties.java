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
package org.apache.cxf.spring.boot.autoconfigure;

import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * {@link ConfigurationProperties} for Apache CXF.
 *
 * @author Vedran Pavic
 */
@ConfigurationProperties("cxf")
@Validated
public class CxfProperties {

    /**
     * Path that serves as the base URI for the services.
     */
    private String path = "/services";

    private final Servlet servlet = new Servlet();

    private final Metrics metrics = new Metrics();
    
    private final JaxrsScan jaxrs = new JaxrsScan();

    @NotNull
    @Pattern(regexp = "/[^?#]*", message = "Path must start with /")
    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Servlet getServlet() {
        return this.servlet;
    }

    public Metrics getMetrics() {
        return this.metrics;
    }
    
    public JaxrsScan getJaxrs() {
        return this.jaxrs;
    }

    public static class Servlet {

        /**
         * Servlet init parameters to pass to Apache CXF.
         */
        private Map<String, String> init = new HashMap<>();

        /**
         * Load on startup priority of the Apache CXF servlet.
         */
        private int loadOnStartup = -1;
        
        /**
         * Enables or disables the servlet registration
         */
        private boolean enabled = true;

        public Map<String, String> getInit() {
            return this.init;
        }

        public void setInit(Map<String, String> init) {
            this.init = init;
        }

        public int getLoadOnStartup() {
            return this.loadOnStartup;
        }

        public void setLoadOnStartup(int loadOnStartup) {
            this.loadOnStartup = loadOnStartup;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    public static class Metrics {
        private final Server server = new Server();
        private final Client client = new Client();
        
        /**
         * Enables or disables metrics instrumentation
         */
        private boolean enabled = true;

        public Server getServer() {
            return this.server;
        }
        
        public Client getClient() {
            return this.client;
        }

        public static class Server {

            /**
             * Whether requests handled by Cxf should be automatically timed. If the number of time series
             * emitted grows too large on account of request mapping timings, disable this and use 'Timed'
             * on a per request mapping basis as needed.
             */
            private boolean autoTimeRequests = true;

            /**
             * Name of the metric for received requests.
             */
            private String requestsMetricName = "cxf.server.requests";

            /**
             * Maximum number of unique URI tag values allowed. After the max number of tag values is
             * reached, metrics with additional tag values are denied by filter.
             */
            private int maxUriTags = 100;

            public boolean isAutoTimeRequests() {
                return this.autoTimeRequests;
            }

            public void setAutoTimeRequests(boolean autoTimeRequests) {
                this.autoTimeRequests = autoTimeRequests;
            }

            public String getRequestsMetricName() {
                return this.requestsMetricName;
            }

            public void setRequestsMetricName(String requestsMetricName) {
                this.requestsMetricName = requestsMetricName;
            }

            public int getMaxUriTags() {
                return this.maxUriTags;
            }

            public void setMaxUriTags(int maxUriTags) {
                this.maxUriTags = maxUriTags;
            }
        }
        
        public static class Client {
            /**
             * Name of the metric for sent requests.
             */
            private String requestsMetricName = "cxf.client.requests";
            
            /**
             * Maximum number of unique URI tag values allowed. After the max number of tag values is
             * reached, metrics with additional tag values are denied by filter.
             */
            private int maxUriTags = 100;

            public String getRequestsMetricName() {
                return this.requestsMetricName;
            }

            public void setRequestsMetricName(String requestsMetricName) {
                this.requestsMetricName = requestsMetricName;
            }
            
            public int getMaxUriTags() {
                return this.maxUriTags;
            }

            public void setMaxUriTags(int maxUriTags) {
                this.maxUriTags = maxUriTags;
            }
        }
        
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

    }
    
    public static class JaxrsScan {

        
        
        /**
         * property to create a JAX-RS endpoint from the auto-discovered JAX-RS 
         * root resources and provider classes. Such classes do not have to be 
         * annotated with Spring @Component. This property needs to be accompanied 
         * by a "cxf.jaxrs.classes-scan-packages" property which sets a comma-separated 
         * list of the packages to scan.
         */
        private boolean classesScan;
        
        /**
         * property to create a JAX-RS endpoint from the auto-discovered 
         * JAX-RS root resources and providers which are marked as Spring 
         * Components (annotated with Spring @Component or created and 
         * returned from @Bean methods).
         */
        private boolean componentScan;
        
        /**
         * property to restrict which of the auto-discovered Spring components
         * are accepted as JAX-RS resource or provider classes. It sets a 
         * comma-separated list of the packages that a given bean instance's 
         * class must be in. Note, this property, if set, is only effective
         * if a given bean is a singleton. It can be used alongside or as
         * an alternative to the "cxf.jaxrs.component-scan-beans" property. 
         */
        private String componentScanPackages;
        
        /**
         * property to restrict which of the auto-discovered Spring components 
         * are accepted as JAX-RS resource or provider classes. It sets a 
         * comma-separated list of the accepted bean names - the auto-discovered 
         * component will only be accepted if its bean name is in this list. 
         * It can be used alongside or as an alternative to the 
         * "cxf.jaxrs.component-scan-packages" property.  
         */
        private String componentScanBeans;
        
        private String classesScanPackages;
        

        public boolean isComponentScan() {
            return componentScan;
        }

        public void setComponentScan(boolean componentScan) {
            this.componentScan = componentScan;
        }

        public String getComponentScanPackages() {
            return componentScanPackages;
        }

        public void setComponentScanPackages(String componentScanPackages) {
            this.componentScanPackages = componentScanPackages;
        }

        public String getComponentScanBeans() {
            return componentScanBeans;
        }

        public void setComponentScanBeans(String componentScanBeans) {
            this.componentScanBeans = componentScanBeans;
        }

        public boolean isClassesScan() {
            return classesScan;
        }

        public void setClassesScan(boolean classesScan) {
            this.classesScan = classesScan;
        }

        public String getClassesScanPackages() {
            return classesScanPackages;
        }

        public void setClassesScanPackages(String classesScanPackages) {
            this.classesScanPackages = classesScanPackages;
        }

        
        
    }

}
