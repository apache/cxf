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

package org.apache.cxf.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a property to record for the endpoint
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface EndpointProperty {

    /**
     * The value(s) of the property
     * @return the value of the property
     */
    String[] value() default { };

    /**
     * The key to record the property
     * @return the key for the property
     */
    String key();

    /**
     * Reference to a named bean that is looked up from the
     * configuration associated with the application.
     */
    String ref() default "";

    /**
     * The class for the property. If "ref" is specified,
     * this class is used to cast the looked up reference
     * to make sure the Object is of the correct type.
     *
     * If ref is not set and value is not set, this class
     * is used to create a bean. The class must have either
     * a default constructor, a constructor that takes an
     * org.apache.cxf.endpoint.Endpoint, or a constructor
     * that takes a org.apache.cxf.endpoint.Endpoint and
     * an org.apache.cxf.Bus.
     */
    Class<?> beanClass() default Object.class;

}

