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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.Predicate;

import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.impl.AbstractResponseContextImpl;
import org.apache.cxf.jaxrs.impl.ResponseImpl;
import org.apache.cxf.jaxrs.utils.ExceptionUtils;
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
            is = r.convertEntityToStreamIfPossible();
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
        // Is Content-Length is explicitly set to 0 ?
        if (HttpUtils.isPayloadEmpty(getHeaders())) {
            return false;
        }
        try {
            return !IOUtils.isEmpty(getEntityStream());
        } catch (IOException ex) {
            throw ExceptionUtils.toInternalServerErrorException(ex, null);
        }
    }

    @Override
    public boolean containsHeaderString(String name, String valueSeparatorRegex, Predicate<String> valuePredicate) {
        final String headerString = HttpUtils.getHeaderString(getHeaders().get(name));
        if (headerString == null) {
            return false;
        }
        return Arrays.stream(headerString.split(valueSeparatorRegex))
            .filter(valuePredicate)
            .findAny()
            .isPresent();
    }
}
