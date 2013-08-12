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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Defines the factory used for the service.
 * 
 * Either use the factoryClass attribute to define your own 
 * factory or use one of the "value" convenience enums.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
public @interface FactoryType {

    Type value() default Type.Singleton;
    
    String[] args() default { };
    
    /**
     * The class for the factory.  It MUST have a constructor that takes
     * two arguments:
     *    1) The Class for the service
     *    2) String[] of the args from above 
     */
    Class<?> factoryClass() default DEFAULT.class;
    
    enum Type {
        Singleton,
        Session,
        Pooled, //args[0] is the size of the pool
        PerRequest,
        Spring, //args[0] is the Spring bean name
    };
    
    static final class DEFAULT { }
}

