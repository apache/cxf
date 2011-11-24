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

package org.apache.cxf.jaxrs.ext.multipart;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotate a JAX-RS function parameter to receive data from a multipart 'part'.
 **/
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Multipart {
    /**
     * The name of the MIME part to map to this parameter. The default is
     * the unnamed default part.
     **/
    String value() default "";
    /**
     * Select the part by MIME type. The default is to match any MIME type.
     */
    String type() default "*/*";
    /**
     * How to handle a missing part. By default, if no part matches,
     * the {@link org.apache.cxf.jaxrs.provider.MultipartProvider} 
     * throws a {@link javax.ws.rs.WebApplicationException}
     * with status 404. If this option is set to <strong>false</strong>,
     * the parameter is set to <strong>null</strong>.
     */
    boolean errorIfMissing() default true; 
}
