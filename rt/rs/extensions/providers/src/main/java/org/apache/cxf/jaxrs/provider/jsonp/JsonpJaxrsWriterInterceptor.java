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

package org.apache.cxf.jaxrs.provider.jsonp;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.WriterInterceptor;
import jakarta.ws.rs.ext.WriterInterceptorContext;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;

/**
 * Appends the jsonp callback to json responses when the '_jsonp' parameter has been set in the querystring.
 */
public class JsonpJaxrsWriterInterceptor implements WriterInterceptor {

    private String mediaType = JsonpInInterceptor.JSONP_TYPE;
    private String paddingEnd = "(";

    public JsonpJaxrsWriterInterceptor() {
    }

    @Override
    public void aroundWriteTo(WriterInterceptorContext context) throws IOException, WebApplicationException {
        String callback = getCallbackValue(JAXRSUtils.getCurrentMessage());
        if (!StringUtils.isEmpty(callback)) {
            context.getHeaders().putSingle(Message.CONTENT_TYPE,
                                           JAXRSUtils.toMediaType(getMediaType()));
            context.getOutputStream().write((callback + getPaddingEnd()).getBytes(StandardCharsets.UTF_8));
        }
        context.proceed();
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setPaddingEnd(String paddingEnd) {
        this.paddingEnd = paddingEnd;
    }

    public String getPaddingEnd() {
        return paddingEnd;
    }

    protected String getCallbackValue(Message message) {
        Exchange exchange = message.getExchange();
        return (String) exchange.get(JsonpInInterceptor.CALLBACK_KEY);
    }



}
