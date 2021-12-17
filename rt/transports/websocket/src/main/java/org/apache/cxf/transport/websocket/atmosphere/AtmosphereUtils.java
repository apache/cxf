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

package org.apache.cxf.transport.websocket.atmosphere;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.cxf.Bus;
import org.apache.cxf.helpers.CastUtils;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptor;
import org.atmosphere.cpr.HeaderConfig;
import org.atmosphere.interceptor.CacheHeadersInterceptor;
import org.atmosphere.interceptor.HeartbeatInterceptor;
import org.atmosphere.interceptor.JavaScriptProtocol;
import org.atmosphere.interceptor.SSEAtmosphereInterceptor;
import org.atmosphere.util.Utils;

/**
 *
 */
public final class AtmosphereUtils {

    private AtmosphereUtils() {
    }

    public static void addInterceptors(AtmosphereFramework framework, Bus bus) {
        Object ais = bus.getProperty("atmosphere.interceptors");
        // pre-install those atmosphere default interceptors before the custom interceptors.
        framework.interceptor(new CacheHeadersInterceptor()).interceptor(new HeartbeatInterceptor())
        .interceptor(new SSEAtmosphereInterceptor()).interceptor(new JavaScriptProtocol());

        if (ais == null || ais instanceof AtmosphereInterceptor) {
            framework.interceptor(ais == null
                ? new DefaultProtocolInterceptor() : (AtmosphereInterceptor)ais);
            return;
        }
        if (ais instanceof List<?>) {
            List<AtmosphereInterceptor> icps = CastUtils.cast((List<?>)ais);
            // add the custom interceptors
            for (AtmosphereInterceptor icp : icps) {
                framework.interceptor(icp);
            }
        }
    }

    public static boolean useAtmosphere(HttpServletRequest req) {
        return Utils.webSocketEnabled(req)
            || req.getParameter(HeaderConfig.X_ATMOSPHERE_TRANSPORT) != null;
    }
}
