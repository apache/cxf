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
package org.apache.cxf.cdi;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;
import org.apache.cxf.jaxrs.ext.ContextProvider;

/**
 * ContextResolved is an internal qualifier used by CXF to differentiate the beans it will manage from
 * beans a user may have provided.  A user should not use this qualifier, but all beans that CXF provides
 * that are from {@link jakarta.ws.rs.core.Context} objects.
 *
 * Likewise, for any field level injections, as well as constructor injections, the CDI instance of the
 * Context object will be used.  Methods annotated {@link jakarta.inject.Inject} will also delegate to CDI.
 * Any method parameter that takes a Context object will still be resolved from non-CDI semantics.
 *
 * For all built in context objects (as defined by the JAX-RS specification), the thread local aware instance
 * is used.  For any custom context objects (implemented via {@link ContextProvider}) you must ensure that
 * they are implemented in a thread safe manner.  All context objects are backed by a
 * {@link jakarta.enterprise.context.RequestScoped} bean.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface ContextResolved {

    ContextResolved LITERAL = new ContextResolvedLiteral();

    final class ContextResolvedLiteral extends AnnotationLiteral<ContextResolved> implements ContextResolved {
        private static final long serialVersionUID = 1L;
    }
}
