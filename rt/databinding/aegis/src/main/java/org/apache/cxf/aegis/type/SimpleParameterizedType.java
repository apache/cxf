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

package org.apache.cxf.aegis.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

/**
 * When an XML file tells is that it wants a List to be a List<x> or a Map to be
 * a Map<x, y>, we create one of these. We do not tolerate nesting. If we really
 * wanted the entire apparatus, asm would be more appropriate. This is good enough
 * to allow us to probe a hash table hashed on Type objects where List<x> might
 * be in there.
 */
class SimpleParameterizedType implements ParameterizedType {
    private Class<?> rawType;
    private Type[] parameters;
    
    SimpleParameterizedType(Class<?> rawType, Type[] parameters) {
        this.rawType = rawType;
        this.parameters = parameters;
    }

    public Type[] getActualTypeArguments() {
        return parameters;
    }

    public Type getOwnerType() {
        // no nested types.
        return null;
    }

    public Type getRawType() {
        return rawType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(parameters);
        result = prime * result + ((rawType == null) ? 0 : rawType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        
        ParameterizedType opt = null;
        try {
            opt = (ParameterizedType) obj;
        } catch (ClassCastException cce) {
            return false;
        }
        
        if (opt.getOwnerType() != null) {
            return false;
        }
        
        if (rawType != opt.getRawType()) {
            return false;
        }
        
        return Arrays.equals(parameters, opt.getActualTypeArguments());
    }
}
