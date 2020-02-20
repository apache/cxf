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
package org.apache.cxf.transport.http_jaxws_spi;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.continuations.SuspendedInvocationException;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;
import org.apache.cxf.transport.http.HTTPSession;

public class JAXWSHttpSpiDestination extends AbstractHTTPDestination {

    static final Logger LOG = LogUtils.getL7dLogger(JAXWSHttpSpiDestination.class);

    public JAXWSHttpSpiDestination(Bus b,
                                   DestinationRegistry registry,
                                   EndpointInfo ei) throws IOException {
        super(b, registry, ei, ei.getAddress(), false);
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    /**
     * This is called by handlers for servicing requests
     *
     * @param req
     * @param resp
     * @throws IOException
     */
    protected void doService(HttpServletRequest req, HttpServletResponse resp)
        throws IOException {

        Bus origBus = BusFactory.getAndSetThreadDefaultBus(bus);
        try {
            serviceRequest(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (origBus != bus) {
                BusFactory.setThreadDefaultBus(origBus);
            }
        }
    }

    protected void serviceRequest(final HttpServletRequest req, final HttpServletResponse resp)
        throws IOException {

        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Service http request on thread: " + Thread.currentThread());
        }
        Message inMessage = new MessageImpl();
        ExchangeImpl exchange = new ExchangeImpl();
        exchange.setInMessage(inMessage);

        setupMessage(inMessage, null, req.getServletContext(), req, resp);

        ((MessageImpl)inMessage).setDestination(this);

        exchange.setSession(new HTTPSession(req));

        try {
            incomingObserver.onMessage(inMessage);
            resp.flushBuffer();
        } catch (SuspendedInvocationException ex) {
            if (ex.getRuntimeException() != null) {
                throw ex.getRuntimeException();
            }
            // else nothing to do
        } catch (Fault ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } finally {
            if (LOG.isLoggable(Level.FINE)) {
                LOG.fine("Finished servicing http request on thread: " + Thread.currentThread());
            }
        }
    }

    protected String getBasePath(String contextPath) throws IOException {
        return contextPath + getAddress().getAddress().getValue();
    }

}
