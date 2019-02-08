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
package org.apache.cxf.jaxrs.client.spring;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;

/**
 * Allows autowiring of proxy clients
 *
 * It creates a proxy from the auto-discovered service class interface.
 * JAX-RS providers (annotated with @Provider) and marked as Spring Components are added to proxy clients.
 * The providers which are not marked as Spring Components can also be optionally auto-discovered.
 * Proxy can also be configured with optional headers such as Accept and Content-Type
 * (if JAX-RS @Produces and/or @Consumes are missing or need to be overridden) and made thread-safe.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import(JaxRsProxyClientConfiguration.class)
public @interface EnableJaxRsProxyClient {

}
