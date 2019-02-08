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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Can be used to document resource classes and methods
 *
 * See <a href="http://www.w3.org/Submission/wadl/#x3-80002.3">WADL Documentation</a>.
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface Description {
    /**
     * This value, if set, will be set as WADL doc content
     */
    String value() default "";
    /**
     * Maps to WADL doc/@xml:lang attribute
     **/
    String lang() default "";
    /**
     * Maps to WADL doc/@title attribute
     **/
    String title() default "";
    /**
     * This uri, if set, will be used to retrieve
     * the content which will be set as WADL doc content
     */
    String docuri() default "";

    /**
     * Target of this description, see {@link DocTarget}
     */
    String target() default "";
}
