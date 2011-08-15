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
package org.apache.cxf.rs.security.xml;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;

public class XmlEncInInterceptor extends AbstractXmlEncInHandler implements PhaseInterceptor<Message> {

    private static final Logger LOG = 
        LogUtils.getL7dLogger(XmlEncInInterceptor.class);
    
    @Override
    public void handleFault(Message message) {
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        decryptContent(message);
    }

    protected void throwFault(String error, Exception ex) {
        LOG.warning(error);
        Response response = Response.status(400).entity(error).build();
        throw ex != null ? new WebApplicationException(ex, response) : new WebApplicationException(response);
    }
    
    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        return null;
    }

    @Override
    public Set<String> getAfter() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getBefore() {
        return Collections.emptySet();
    }

    @Override
    public String getId() {
        return getClass().getName();
    }

    @Override
    public String getPhase() {
        return Phase.UNMARSHAL;
    }

    
}
