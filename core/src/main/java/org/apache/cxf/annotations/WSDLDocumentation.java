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
 * Adds documentation nodes to the generated WSDL
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
@Inherited
public @interface WSDLDocumentation {
    /**
     * The documentation to add
     * @return documentation string
     */
    String value();

    /**
     * The place to put the documentation.  The Default depends on the
     * location of the annotation.   On the method in the SEI, it would be
     * the portType/operation, on the SEI, it would be the portType, on the
     * service impl, the service element.
     * @return location
     */
    Placement placement() default Placement.DEFAULT;

    /**
     * If Placement is FAULT_MESSAGE, PORT_FAULT, or BINDING_FAULT,
     * return the fault class associated with this documentation
     * @return the fault class
     */
    Class<?> faultClass() default DEFAULT.class;

    enum Placement {
        DEFAULT,
        TOP,

        INPUT_MESSAGE,
        OUTPUT_MESSAGE,
        FAULT_MESSAGE,

        PORT_TYPE,
        PORT_TYPE_OPERATION,
        PORT_TYPE_OPERATION_INPUT,
        PORT_TYPE_OPERATION_OUTPUT,
        PORT_TYPE_OPERATION_FAULT,

        BINDING,
        BINDING_OPERATION,
        BINDING_OPERATION_INPUT,
        BINDING_OPERATION_OUTPUT,
        BINDING_OPERATION_FAULT,

        SERVICE,
        SERVICE_PORT,

    };


    final class DEFAULT { }
}
