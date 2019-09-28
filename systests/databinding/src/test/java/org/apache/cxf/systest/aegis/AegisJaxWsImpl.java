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
package org.apache.cxf.systest.aegis;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jws.WebService;

import org.apache.cxf.systest.aegis.bean.Item;

import static java.nio.charset.StandardCharsets.UTF_8;

@WebService(endpointInterface = "org.apache.cxf.systest.aegis.AegisJaxWs")
public class AegisJaxWsImpl implements AegisJaxWs {

    Map<Integer, Item> items = new HashMap<>();

    public void addItem(Item item) {
        items.put(item.getKey(), item);
    }

    public Map<?, ?> getItemsMap() {
        return items;
    }

    public Map<Integer, Item> getItemsMapSpecified() {
        return items;
    }

    public Item getItemByKey(String key1, String key2) {
        Item fake = new Item();
        fake.setKey(Integer.valueOf(33));
        fake.setData(key1 + ':' + key2);
        return fake;
    }


    public Integer getSimpleValue(Integer a, String b) {
        return a;
    }

    public List<String> getStringList() {
        return Arrays.asList("a", "b", "c");
    }

    public java.util.List<String> echoBigList(java.util.List<String> l) {
        return l;
    }

    public byte[] export(List<Integer> integers) {
        StringBuilder b = new StringBuilder(integers.size() * 3);
        for (Integer i : integers) {
            b.append(i);
        }
        return b.toString().getBytes(UTF_8);
    }

}
