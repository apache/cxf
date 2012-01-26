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

package org.apache.cxf.common.injection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to let our JSR250 Processor know
 * not to bother examining the class for annotations
 * as it's know not to have any 
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface NoJSR250Annotations {
    
    /**
     * If these fields are null, it will go ahead and do JSR250 processing
     * as it assumes the values were not set via a constructor.
     * 
     * Be careful with this.  If the field is injected with a value via @Resource,
     * when the other annotations are processed (@PostConstruct), the field is then
     * not-null so they won't be run.   The best bet is to make sure the @Resource
     * setter methods handle any registration or similar
     */
    String[] unlessNull() default { };
}
