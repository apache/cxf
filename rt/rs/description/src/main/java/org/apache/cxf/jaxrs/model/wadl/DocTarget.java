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
package org.apache.cxf.jaxrs.model.wadl;

/**
 * {@link Description} can use one of DocTarget constants to bind
 * itself to a specific WADL element.
 * {@link Description} annotations documenting WADL 'resource', 'method',
 * 'param' and input 'representation' elements do not have use these constants.
 */
public final class DocTarget {
    /**
     * WADL resource element, in most cases it corresponds
     * to the root resource or sub-resource classes
     */
    public static final String RESOURCE = "resource";
    /**
     * WADL method element, corresponds to a class resource method
     */
    public static final String METHOD = "method";
    /**
     * WADL request param or representation elements, correspond to
     * input parameters of the resource method
     */
    public static final String PARAM = "param";
    /**
     * WADL response representation element, corresponds to
     * the return type of the resource method
     */
    public static final String RETURN = "return";

    /**
     * WADL request element
     */
    public static final String REQUEST = "request";
    /**
     * WADL request element
     */
    public static final String RESPONSE = "response";

    private DocTarget() {

    }
}
