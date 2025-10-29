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
package org.apache.cxf.microprofile.client.mock;

import java.io.IOException;
import java.lang.reflect.Method;

import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Configuration;
import jakarta.ws.rs.core.Response;

public class InvokedMethodClientRequestFilter implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext ctx) throws IOException {
        try {
            final Configuration configuration = ctx.getConfiguration();
            if (configuration == null) {
                throw new NullPointerException("Configuration is null");
            }

            Method m = (Method) ctx.getProperty("org.eclipse.microprofile.rest.client.invokedMethod");

            Path path = m.getAnnotation(Path.class);
            ctx.abortWith(Response.ok("OK")
                                  .header("ReturnType", m.getReturnType().getName())
                                  .header("PUT", m.getAnnotation(PUT.class) == null ? "null" : "PUT")
                                  .header("Path", path == null ? "null" : path.value())
                                  .header("Parm1", m.getParameters()[0].getType().getName())
                                  .header("Parm1Annotation", 
                                          m.getParameters()[0].getAnnotations()[0].annotationType().getName())
                                  .header("Parm2", m.getParameters()[1].getType().getName())
                                  .build());
        } catch (Throwable t) {
            t.printStackTrace();
            ctx.abortWith(Response.serverError().build());
        }
    }

}
