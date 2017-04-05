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

package org.apache.cxf.jaxrs.utils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

public final class ParameterizedCollectionType implements ParameterizedType {
    private final Class<?> collectionMemberClass;
    private final Type[] typeArgs;

    public ParameterizedCollectionType(Class<?> collectionMemberClass) {
        this.collectionMemberClass = collectionMemberClass;
        this.typeArgs = new Type[] {collectionMemberClass};
    }
    
    public ParameterizedCollectionType(Type pt) {
        this.collectionMemberClass = InjectionUtils.getRawType(pt);
        this.typeArgs = new Type[] {pt};
    }

    public Type[] getActualTypeArguments() {
        return typeArgs;
    }

    public Type getOwnerType() {
        return null;
    }

    public Type getRawType() {
        return Collection.class;
    }

    public String toString() {
        return "java.util.Collection<" + collectionMemberClass.getName() + ">";
    }
}
