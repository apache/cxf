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
package org.apache.cxf.systest.jaxrs.form;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.ext.ReaderInterceptor;
import jakarta.ws.rs.ext.ReaderInterceptorContext;
import org.apache.cxf.common.logging.LogUtils;

@Provider
public class FormReaderInterceptor implements ReaderInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(FormReaderInterceptor.class);

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext ctx) throws IOException, WebApplicationException {
        BufferedReader br = new BufferedReader(new InputStreamReader(ctx.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            LOG.info("readLine: " + line);
        }

        ByteArrayInputStream bais = new ByteArrayInputStream("value=MODIFIED".getBytes());
        LOG.info("set value=MODIFIED");
        ctx.setInputStream(bais);
        return ctx.proceed();
    }

}
