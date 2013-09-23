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
package org.apache.cxf.jaxrs.client.spec;

import java.io.InputStream;

import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.AbstractResponseContextImpl;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.apache.cxf.message.Message;

public class ClientResponseContextImpl extends AbstractResponseContextImpl 
    implements ClientResponseContext {

    public ClientResponseContextImpl(ResponseImpl r, 
                                     Message m) {
        super(r, m);
    }
    
    public InputStream getEntityStream() {
        InputStream is = m.getContent(InputStream.class);
        if (is == null) {
            is = ((ResponseImpl)r).convertEntityToStreamIfPossible();
        }
        return is;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return HttpUtils.getModifiableStringHeaders(m);
    }

    
    @Override
    public void setEntityStream(InputStream is) {
        m.setContent(InputStream.class, is);
        r.setEntity(is, r.getEntityAnnotations());

    }
    
    @Override
    public boolean hasEntity() { 
        return getEntityStream() != null;
    }
}
