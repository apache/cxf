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

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

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

    public static class Servlet {

        /**
         * Servlet init parameters to pass to Apache CXF.
         */
        private Map<String, String> init = new HashMap<>();

        /**
         * Load on startup priority of the Apache CXF servlet.
         */
        private int loadOnStartup = -1;

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

}
