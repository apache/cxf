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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("management.metrics")
public class MetricsProperties {

    private final Soap soap = new Soap();

    public Soap getSoap() {
        return this.soap;
    }

    public static class Soap {

        private final Server server = new Server();

        public Server getServer() {
            return this.server;
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
            private String requestsMetricName = "soap.server.requests";

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
    }
}
