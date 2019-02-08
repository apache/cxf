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

package demo.jaxrs.tracing.client;

import brave.okhttp3.TracingCallFactory;
import demo.jaxrs.tracing.server.CatalogTracing;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OkHttp3Client {
    private OkHttp3Client() {
    }

    public static void main(final String[] args) throws Exception {
        try (final CatalogTracing tracing = new CatalogTracing("catalog-client")) {
            final OkHttpClient client = new OkHttpClient();
            final Call.Factory factory = TracingCallFactory.create(tracing.getHttpTracing(), client);
            
            final Request request = new Request.Builder()
                .url("http://localhost:9000/catalog")
                .header("Accept", "application/json")
                .build();

            try (final Response response = factory.newCall(request).execute()) {
                System.out.println(response.body().string());
            }
        }
    }
}
