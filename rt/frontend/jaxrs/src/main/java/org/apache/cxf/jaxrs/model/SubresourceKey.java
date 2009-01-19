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

package org.apache.cxf.jaxrs.model;

public class SubresourceKey {
    private Class<?> typedClass;
    private Class<?> instanceClass;
    
    public SubresourceKey(Class<?> tClass, Class<?> iClass) {
        typedClass = tClass;
        instanceClass = iClass;
    }
    
    public Class<?> getTypedClass() {
        return typedClass;
    }
    
    public Class<?> getInstanceClass() {
        return instanceClass;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof SubresourceKey)) {
            return false;
        }
        SubresourceKey other = (SubresourceKey)o;
        return typedClass == other.typedClass && instanceClass == other.instanceClass;
    }
    
    @Override
    public int hashCode() {
        return typedClass.hashCode() + 37 * instanceClass.hashCode();
    }
}
