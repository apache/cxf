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
 * Enables FastInfoset negotiation
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE })
@Inherited
public @interface FastInfoset {
    /**
     * Set to true in order for FastInfoset to be always used without negotiation
     */
    boolean force() default false;

    /**
     * Sets the property <code>attributeValueMapMemoryLimit</code> on FastInfoset StAX Serializers. The property
     * controls attribute value map size and can be used to control the memory and (indirectly) CPU footprint of
     * processing.
     */
    int serializerAttributeValueMapMemoryLimit() default -1;

    /**
     * Sets the property <code>minAttributeValueSize</code> on FastInfoset StAX Serializers. The property controls the
     * <b>minimum</b> size of attribute values to be indexed.
     */
    int serializerMinAttributeValueSize() default -1;

    /**
     * Sets the property <code>maxAttributeValueSize</code> on FastInfoset StAX Serializers. The property controls the
     * <b>maximum</b> size of attribute values to be indexed. Tests have shown that setting this property to lower
     * values reduces CPU burden of processing, at the expense of larger sizes of resultant encoded Fast Infoset data.
     */
    int serializerMaxAttributeValueSize() default -1;

    /**
     * Sets the property <code>characterContentChunkMapMemoryLimit</code> on FastInfoset StAX Serializers. The property
     * controls character content chunk map size and can be used to control the memory and (indirectly) CPU footprint of
     * processing.
     */
    int serializerCharacterContentChunkMapMemoryLimit() default -1;

    /**
     * Sets the property <code>minCharacterContentChunkSize</code> on FastInfoset StAX Serializers. The property
     * controls the <b>minimum</b> size of character content chunks to be indexed.
     */
    int serializerMinCharacterContentChunkSize() default -1;

    /**
     * Sets the property <code>maxCharacterContentChunkSize</code> on FastInfoset StAX Serializers. The property
     * controls the <b>maximum</b> size of character content chunks to be indexed. Tests have shown that setting this
     * property to lower values reduces CPU burden of processing, at the expense of larger sizes of resultant encoded
     * Fast Infoset data.
     */
    int serializerMaxCharacterContentChunkSize() default -1;

}

