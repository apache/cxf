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
package org.apache.cxf.tracing.brave;

import com.github.kristofa.brave.http.HttpRequest;
import com.github.kristofa.brave.http.SpanNameProvider;

import org.apache.cxf.common.util.StringUtils;

public class ServerSpanNameProvider implements SpanNameProvider {
    @Override
    public String spanName(HttpRequest request) {
        return buildSpanDescription(request.getUri().getPath(), request.getHttpMethod());
    }
    
    private String buildSpanDescription(final String path, final String method) {
        if (StringUtils.isEmpty(method)) {
            return path;
        } else {
            return method + " " + path;
        }
    }
}
