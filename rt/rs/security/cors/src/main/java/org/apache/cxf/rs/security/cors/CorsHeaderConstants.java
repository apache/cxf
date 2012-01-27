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

package org.apache.cxf.rs.security.cors;

/**
 * Headers used to implement http://www.w3.org/TR/cors/.
 */
public final class CorsHeaderConstants {

    public static final String HEADER_ORIGIN = "Origin";
    public static final String HEADER_AC_REQUEST_METHOD = "Access-Control-Request-Method";
    public static final String HEADER_AC_REQUEST_HEADERS = "Access-Control-Request-Headers";
    public static final String HEADER_AC_ALLOW_METHODS = "Access-Control-Allow-Methods";
    public static final String HEADER_AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    public static final String HEADER_AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    public static final String HEADER_AC_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    public static final String HEADER_AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    public static final String HEADER_AC_MAX_AGE = "Access-Control-Max-Age";

    private CorsHeaderConstants() {
        //
    }

}
