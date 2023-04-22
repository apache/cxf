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

package org.apache.cxf.metrics.micrometer;

public class MicrometerMetricsProperties {

    /**
     * Whether requests handled by Cxf should be automatically timed. If the number of time series
     * emitted grows too large on account of request mapping timings, disable this and use 'Timed'
     * on a per request mapping basis as needed.
     */
    private boolean autoTimeRequests = true;

    /**
     * Name of the metric for received requests.
     */
    private String serverRequestsMetricName = "cxf.server.requests";
    
    /**
     * Name of the metric for sent requests.
     */
    private String clientRequestsMetricName = "cxf.client.requests";

    public boolean isAutoTimeRequests() {
        return autoTimeRequests;
    }

    public void setAutoTimeRequests(boolean autoTimeRequests) {
        this.autoTimeRequests = autoTimeRequests;
    }

    public String getServerRequestsMetricName() {
        return serverRequestsMetricName;
    }

    public void setServerRequestsMetricName(String requestsMetricName) {
        this.serverRequestsMetricName = requestsMetricName;
    }

    public String getClientRequestsMetricName() {
        return clientRequestsMetricName;
    }

    public void setClientRequestsMetricName(String clientRequestsMetricName) {
        this.clientRequestsMetricName = clientRequestsMetricName;
    }
}
