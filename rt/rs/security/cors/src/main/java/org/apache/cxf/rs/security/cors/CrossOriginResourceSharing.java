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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Attach <a href="http://www.w3.org/TR/cors/">CORS</a> information
 * to a resource. This annotation is read by {@link CrossOriginResourceSharingFilter}.
 * If this annotation is present on a method, or 
 * on the method's class (or its superclasses), then it completely
 * overrides any parameters set in {@link CrossOriginResourceSharingFilter}. 
 * If a particular parameter of this annotation is not specified, then the
 * default value is used, <em>not</em> the parameters of the filter. 
 * 
 * Note that the CORS specification censors the headers on a 
 * preflight OPTIONS request. As a result, the filter cannot determine
 * exactly which method corresponds to the request, and so uses only 
 * class-level annotations to set policies.
 */
@Target({ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface CrossOriginResourceSharing {
    /**
     * If true, this resource will return 
     * <pre>Access-Control-Allow-Origin: *</pre>
     * for a valid request 
     */
    boolean allowAllOrigins() default false;
    /**
     * A list of permitted origins. It is ignored if
     * {@link #allowAllOrigins()} returns true
     */
    String[] allowOrigins() default { };
    /**
     * A list of headers that the client may include
     * in an actual request. All the headers listed in 
     * the Access-Control-Request-Headers will be allowed if
     * the list is empty
     */
    String[] allowHeaders() default { };
    
    /**
     * If true, this resource will return 
     * <pre>Access-Control-Allow-Credentials: true</pre>
     */
    boolean allowCredentials() default false;
    /**
     * A list of headers to return in <tt>
     * Access-Control-Expose-Headers</tt>. 
     */
    String[] exposeHeaders() default { };
    /**
     * The value to return in <tt>Access-Control-Max-Age</tt>.
     * If this is negative, then no header is returned. The default
     * value is -1.
     */
    int maxAge() default -1;
}
