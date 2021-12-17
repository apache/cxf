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

package org.apache.cxf.systest.jaxb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 *
 */
public class HashMapAdapter extends XmlAdapter<HashMapAdapter.HashMapType, Map<String, byte[]>> {

    @XmlType()
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HashMapEntryType {
        @XmlAttribute
        private String key;
        @XmlValue
        private byte[] value;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public byte[] getValue() {
            return value;
        }

        public void setValue(byte[] value) {
            this.value = value;
        }
    }

    public static class HashMapType {
        private List<HashMapEntryType> entry = new ArrayList<>();

        public List<HashMapEntryType> getEntry() {
            return entry;
        }

        public void setEntry(List<HashMapEntryType> entry) {
            this.entry = entry;
        }
    }

    public HashMapType marshal(Map<String, byte[]> arg0) throws Exception {
        HashMapType myHashMapType = new HashMapType();
        if (arg0 != null && !arg0.isEmpty()) {
            for (Map.Entry<String, byte[]> entry : arg0.entrySet()) {
                if (entry != null) {
                    HashMapEntryType myHashEntryType = new HashMapEntryType();
                    myHashEntryType.key = entry.getKey();
                    myHashEntryType.value = entry.getValue();
                    myHashMapType.entry.add(myHashEntryType);
                }
            }
        }
        return myHashMapType;
    }

    public Map<String, byte[]> unmarshal(HashMapType arg0) throws Exception {
        Map<String, byte[]> hashMap = new HashMap<>();
        if (arg0 != null && arg0.entry != null) {
            for (HashMapEntryType myHashEntryType : arg0.entry) {
                if (myHashEntryType.key != null) {
                    hashMap.put(myHashEntryType.key, myHashEntryType.value);
                }
            }
        }
        return hashMap;
    }

}
