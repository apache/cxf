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

package demo.jaxrs.tracing.server;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

public class CatalogStore {
    private final Map<String, String> books = new ConcurrentHashMap<>();

    public CatalogStore() {
    }

    public boolean remove(final String key) throws IOException {
        return books.remove(key) != null;
    }

    public JsonObject get(final String key) throws IOException {
        final String title = books.get(key);

        if (title != null) {
            return Json.createObjectBuilder()
                .add("id", key)
                .add("title", title)
                .build();
        }

        return null;
    }

    public void put(final String key, final String title) throws IOException {
        books.put(key, title);
    }

    public JsonArray scan() throws IOException {
        final JsonArrayBuilder builder = Json.createArrayBuilder();

        for (final Map.Entry<String, String> entry: books.entrySet()) {
            builder.add(Json.createObjectBuilder()
                .add("id", entry.getKey())
                .add("title", entry.getValue())
            );
        }

        return builder.build();
    }

}
