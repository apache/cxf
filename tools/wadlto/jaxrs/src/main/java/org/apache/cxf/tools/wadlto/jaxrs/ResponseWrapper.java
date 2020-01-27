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

package org.apache.cxf.tools.wadlto.jaxrs;

import java.util.Set;

import org.apache.cxf.common.util.StringUtils;

/**
 * Wraps response into the container if necessary (for example, when code is generated 
 * using the JAX-RS 2.1 Reactive Extensions).
 */
interface ResponseWrapper {
    /**
     * Wraps response into the container if necessary
     * @param type the response type
     * @param imports current set of import statements to enrich
     * @return response wrapped into the container if necessary
     */
    String wrap(String type, Set<String> imports);
    
    /**
     * Wraps response into the container if necessary
     * @param type the response type
     * s@param imports current set of import statements to enrich
     * @return response wrapped into the container if necessary
     */
    default String wrap(Class<?> type, Set<String> imports) {
        return wrap(type.getSimpleName(), imports);
    }
    
    /**
     * Creates a new instance of the response wrapper
     * @param library the reactive library to use (or null if none) 
     * @return the instance of the response wrapper
     */
    static ResponseWrapper create(String library) {
        if (StringUtils.isEmpty(library)) {
            return new NoopResponseWrapper(); 
        } else if ("java8".equalsIgnoreCase(library.trim())) {
            return new Java8ResponseWrapper();
        } else {
            throw new IllegalArgumentException("The Reactive Extensions library is not supported: " + library);
        }
    }
}

/**
 * Noop response wrapper, returns the response as-is.
 */
class NoopResponseWrapper implements ResponseWrapper {
    @Override
    public String wrap(String type, Set<String> imports) {
        return type;
    }
}

/**
 * Response wrapper for java.util.concurrent.CompletableFuture, returns 
 * the response wrapped into CompletableFuture<?> container.
 */
class Java8ResponseWrapper implements ResponseWrapper {
    @Override
    public String wrap(String type, Set<String> imports) {
        if (imports != null) {
            imports.add("java.util.concurrent.CompletableFuture");
        }
        
        return "CompletableFuture<" + type + ">";
    }
}