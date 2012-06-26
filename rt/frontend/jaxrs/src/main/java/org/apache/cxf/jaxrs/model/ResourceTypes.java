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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

public class ResourceTypes {
    private Map<Class<?>, Type> allTypes = new HashMap<Class<?>, Type>();
    private Map<Class<?>, QName> collectionMap = new HashMap<Class<?>, QName>();
    public Map<Class<?>, Type> getAllTypes() {
        return allTypes;
    }
    public void setAllTypes(Map<Class<?>, Type> allTypes) {
        this.allTypes = allTypes;
    }
    public Map<Class<?>, QName> getCollectionMap() {
        return collectionMap;
    }
    public void setCollectionMap(Map<Class<?>, QName> collectionMap) {
        this.collectionMap = collectionMap;
    }
}
