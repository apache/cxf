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
package org.apache.cxf.jaxrs.nio;

import java.io.IOException;
import java.util.logging.Logger;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.ws.rs.container.AsyncResponse;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public final class NioReadListenerImpl implements ReadListener {
    private static final Logger LOG = LogUtils.getL7dLogger(NioReadListenerImpl.class);
    private final NioReadEntity entity;
    private final NioInputStream in;

    public NioReadListenerImpl(NioReadEntity entity, ServletInputStream in) {
        this.entity = entity;
        this.in = new NioInputStream(in);
    }

    @Override
    public void onDataAvailable() throws IOException {
        while (in.isReady() && !in.isFinished()) {
            entity.getReader().read(in);
        }
    }

    @Override
    public void onAllDataRead() throws IOException {
        entity.getCompletion().complete();
    }
    
    @Override
    public void onError(Throwable t) {
        if (entity.getError() == null) {
            getAsyncResponse().resume(t);
            return;
        }
        try {
            entity.getError().error(t);
        } catch (final Throwable ex) {
            LOG.warning("NIO NioReadListener error: " + ExceptionUtils.getStackTrace(ex));
        }
    }

    private AsyncResponse getAsyncResponse() {
        return JAXRSUtils.getCurrentMessage().getExchange().get(AsyncResponse.class);
    }

}