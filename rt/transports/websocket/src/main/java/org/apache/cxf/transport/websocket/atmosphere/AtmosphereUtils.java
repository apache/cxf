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

import org.apache.cxf.Bus;
import org.atmosphere.cpr.AtmosphereFramework;
import org.atmosphere.cpr.AtmosphereInterceptor;

/**
 * 
 */
public final class AtmosphereUtils {

    private AtmosphereUtils() {
    }

    public static void addInterceptors(AtmosphereFramework framework, Bus bus) {
        List<AtmosphereInterceptor> ais = (List<AtmosphereInterceptor>)bus.getProperty("atmosphere.interceptors");
        if (ais == null) {
            framework.interceptor(new DefaultProtocolInterceptor());
            return;
        } 
        for (AtmosphereInterceptor i : ais) {
            framework.interceptor(i);
        }
    }
}
