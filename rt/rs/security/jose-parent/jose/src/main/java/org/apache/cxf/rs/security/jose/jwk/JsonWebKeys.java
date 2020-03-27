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
package org.apache.cxf.rs.security.jose.jwk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxrs.json.basic.JsonMapObject;

public class JsonWebKeys extends JsonMapObject {
    public static final String KEYS_PROPERTY = "keys";
    private static final long serialVersionUID = -8002543601655429723L;

    public JsonWebKeys() {

    }
    public JsonWebKeys(JsonWebKey key) {
        setKey(key);
    }

    public JsonWebKeys(List<JsonWebKey> keys) {
        setKeys(keys);
    }

    public List<JsonWebKey> getKeys() {
        List<?> list = (List<?>)super.getProperty(KEYS_PROPERTY);
        if (list != null && !list.isEmpty()) {
            Object first = list.get(0);
            if (first instanceof JsonWebKey) {
                return CastUtils.cast(list);
            }
            List<Map<String, Object>> listOfMaps = super.getListMapProperty(KEYS_PROPERTY);
            List<JsonWebKey> keys = new ArrayList<>(listOfMaps.size());
            for (Map<String, Object> map : listOfMaps) {
                keys.add(new JsonWebKey(map));
            }
            return keys;
        }
        return null;
    }
    public final void setKey(JsonWebKey key) {
        setKeys(Collections.singletonList(key));
    }
    public final void setKeys(List<JsonWebKey> keys) {
        super.setProperty(KEYS_PROPERTY, keys);
    }

    public Map<String, JsonWebKey> getKeyIdMap() {
        List<JsonWebKey> keys = getKeys();
        if (keys == null) {
            return Collections.emptyMap();
        }
        Map<String, JsonWebKey> map = new LinkedHashMap<>();
        for (JsonWebKey key : keys) {
            String kid = key.getKeyId();
            if (kid != null) {
                map.put(kid, key);
            }
        }
        return map;
    }
    public JsonWebKey getKey(String kid) {
        return getKeyIdMap().get(kid);
    }
    public Map<KeyType, List<JsonWebKey>> getKeyTypeMap() {
        List<JsonWebKey> keys = getKeys();
        if (keys == null) {
            return Collections.emptyMap();
        }
        Map<KeyType, List<JsonWebKey>> map = new LinkedHashMap<>();
        for (JsonWebKey key : keys) {
            KeyType type = key.getKeyType();
            if (type != null) {
                List<JsonWebKey> list = map.get(type);
                if (list == null) {
                    list = new LinkedList<>();
                    map.put(type, list);
                }
                list.add(key);
            }
        }
        return map;
    }

    public Map<KeyOperation, List<JsonWebKey>> getKeyOperationMap() {
        List<JsonWebKey> keys = getKeys();
        if (keys == null) {
            return Collections.emptyMap();
        }
        Map<KeyOperation, List<JsonWebKey>> map = new LinkedHashMap<>();
        for (JsonWebKey key : keys) {
            List<KeyOperation> ops = key.getKeyOperation();
            if (ops != null) {
                for (KeyOperation op : ops) {
                    List<JsonWebKey> list = map.get(op);
                    if (list == null) {
                        list = new LinkedList<>();
                        map.put(op, list);
                    }
                    list.add(key);
                }
            }
        }
        return map;
    }
    public List<JsonWebKey> getKeys(String keyType) {
        KeyType kt = KeyType.getKeyType(keyType);
        if (kt == null) {
            return null;
        }
        return getKeyTypeMap().get(kt);
    }
    public List<JsonWebKey> getRsaKeys() {
        return getKeyTypeMap().get(KeyType.RSA);
    }
    public List<JsonWebKey> getEllipticKeys() {
        return getKeyTypeMap().get(KeyType.EC);
    }
    public List<JsonWebKey> getSecretKeys() {
        return getKeyTypeMap().get(KeyType.OCTET);
    }
}
