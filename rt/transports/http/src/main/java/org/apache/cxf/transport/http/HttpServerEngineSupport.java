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

package org.apache.cxf.transport.http;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.SystemPropertyAction;

/**
 * Support class for HTTP server engines: holds common properties and commonly
 * used methods, shared across all HTTP server engine implementations (Tomcat, Jetty,
 * Undertow, Netty, ...).
 */
public interface HttpServerEngineSupport {
    String ENABLE_HTTP2 = "org.apache.cxf.transports.http2.enabled";
    
    
    /** 
     * Check if Http2 is enabled on the Bus or system property
     * Default if not configured otherwise is true
     * @param bus
     * @return
     */
    default boolean isHttp2Enabled(Bus bus) {
        Object value = null;
        
        if (bus != null) {
            value = bus.getProperty(ENABLE_HTTP2);
        }
        
        if (value == null) {
            value = SystemPropertyAction.getPropertyOrNull(ENABLE_HTTP2);
        }
        
        return !PropertyUtils.isFalse(value);
    }
    
    /** 
     * Check if Http2 is enabled on the Bus or system property
     * Default if not configured otherwise is false
     * @param bus
     * @return
     */
    default boolean isHttp2Required(Bus bus) {
        Object value = null;
        
        if (bus != null) {
            value = bus.getProperty(ENABLE_HTTP2);
        }
        
        if (value == null) {
            value = SystemPropertyAction.getPropertyOrNull(ENABLE_HTTP2);
        }
        
        return PropertyUtils.isTrue(value);
    }
}
