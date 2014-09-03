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
package org.apache.cxf.rs.security.oauth2.jwk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.rs.security.oauth2.jwt.AbstractJwtObject;

public class JsonWebKeys extends AbstractJwtObject {
    public static final String KEYS_PROPERTY = "keys";
    public List<JsonWebKey> getKeys() {
        List<?> list = (List<?>)super.getValue(KEYS_PROPERTY);
        if (list != null && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof JsonWebKey) {
                return CastUtils.cast(list);
            } else {
                List<JsonWebKey> keys = new LinkedList<JsonWebKey>();
                List<Map<String, Object>> listOfMaps = 
                    CastUtils.cast((List<?>)super.getValue(KEYS_PROPERTY));
                for (Map<String, Object> map : listOfMaps) {
                    keys.add(new JsonWebKey(map));
                }
                return keys;
            }
        } else {
            return null;
        }
    }

    public void setKeys(List<JsonWebKey> keys) {
        super.setValue(KEYS_PROPERTY, keys);
    }
    
    public Map<String, JsonWebKey> getKeysMap() {
        List<JsonWebKey> keys = getKeys();
        if (keys == null) {
            return Collections.emptyMap();
        }
        Map<String, JsonWebKey> map = new LinkedHashMap<String, JsonWebKey>();
        for (JsonWebKey key : keys) {
            String kid = key.getKid();
            if (kid != null) {
                map.put(kid, key);
            }
        }
        return map;
    }
}
