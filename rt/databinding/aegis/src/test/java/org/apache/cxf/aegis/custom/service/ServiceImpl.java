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
package org.apache.cxf.aegis.custom.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServiceImpl implements Service {

    public Map<String, NoDefaultConstructorBean> getByIds(Collection<String> beanIds) {
        if (beanIds == null) {
            throw new IllegalArgumentException("beanIds is null");
        }

        Map<String, NoDefaultConstructorBean> map = new HashMap<String, NoDefaultConstructorBean>();

        map.put("1", new NoDefaultConstructorBeanImpl("1", "name"));
        map.put("2", new NoDefaultConstructorBeanImpl("2", "name"));

        return map;
    }

    public Collection<NoDefaultConstructorBean> getByName(NoDefaultConstructorBeanKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        Collection<NoDefaultConstructorBean> collection = new ArrayList<NoDefaultConstructorBean>();

        collection.add(new NoDefaultConstructorBeanImpl("1", "name"));

        return collection;
    }

    public Map<NoDefaultConstructorBeanKey, Collection<NoDefaultConstructorBean>> 
    getByNames(Collection<NoDefaultConstructorBeanKey> keys) {
        if (keys == null) {
            throw new IllegalArgumentException("keys is null");
        }

        Map<NoDefaultConstructorBeanKey, Collection<NoDefaultConstructorBean>> map = 
                new HashMap<NoDefaultConstructorBeanKey, Collection<NoDefaultConstructorBean>>();

        Collection<NoDefaultConstructorBean> collection = new ArrayList<NoDefaultConstructorBean>();

        collection.add(new NoDefaultConstructorBeanImpl("1", "name"));

        map.put(new NoDefaultConstructorBeanKeyImpl("name"), collection);

        return map;
    }

    public String getException(NoDefaultConstructorBeanKey key) throws ServiceException {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }

        throw new ServiceException("Key: " + key.getKey());
    }
}
