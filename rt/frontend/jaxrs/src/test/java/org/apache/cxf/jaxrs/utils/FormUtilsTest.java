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

package org.apache.cxf.jaxrs.utils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FormUtilsTest {
    private static final String HTTP_PARAM = "httpParam";
    private static final String HTTP_PARAM1 = HTTP_PARAM + "1";
    private static final String HTTP_PARAM2 = HTTP_PARAM + "2";
    private static final String HTTP_PARAM_VALUE = "httpValue";
    private static final String HTTP_PARAM_VALUE1 = HTTP_PARAM_VALUE + "1";
    private static final String HTTP_PARAM_VALUE2 = HTTP_PARAM_VALUE + "2";

    private static final String FORM_PARAM = "formParam";
    private static final String FORM_PARAM1 = FORM_PARAM + "1";
    private static final String FORM_PARAM2 = FORM_PARAM + "2";
    private static final String FORM_PARAM_VALUE = "formValue";
    private static final String FORM_PARAM_VALUE1 = FORM_PARAM_VALUE + "1";
    private static final String FORM_PARAM_VALUE2 = FORM_PARAM_VALUE + "2";

    private Message mockMessage;
    private HttpServletRequest mockRequest;

    @Test
    public void populateMapFromStringFromHTTP() {
        mockObjects(null, 2);

        MultivaluedMap<String, String> params = new MetadataMap<>();
        FormUtils.populateMapFromString(params, mockMessage, null, StandardCharsets.UTF_8.name(),
                                        false, mockRequest);

        assertEquals(2, params.size());
        assertEquals(HTTP_PARAM_VALUE1, params.get(HTTP_PARAM1).iterator().next());
        assertEquals(HTTP_PARAM_VALUE2, params.get(HTTP_PARAM2).iterator().next());
    }

    @Test
    public void populateMapFromStringFromHTTPWithProp() {
        mockObjects("false", 2);

        MultivaluedMap<String, String> params = new MetadataMap<>();
        FormUtils.populateMapFromString(params, mockMessage, null, StandardCharsets.UTF_8.name(),
                                        false, mockRequest);

        assertEquals(0, params.size());
    }

    @Test
    public void populateMapFromStringFromBody() {
        mockObjects(null, 2);

        MultivaluedMap<String, String> params = new MetadataMap<>();
        String postBody = FORM_PARAM1 + "=" + FORM_PARAM_VALUE1 + "&" + FORM_PARAM2 + "=" + FORM_PARAM_VALUE2;
        FormUtils.populateMapFromString(params, mockMessage, postBody, StandardCharsets.UTF_8.name(),
                                        false, mockRequest);

        assertEquals(2, params.size());
        assertEquals(FORM_PARAM_VALUE1, params.get(FORM_PARAM1).iterator().next());
        assertEquals(FORM_PARAM_VALUE2, params.get(FORM_PARAM2).iterator().next());
    }


    @Test
    public void populateMapFromBodyExceedsDefaultMaxFormParams() {
        mockObjects(null, FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT);
        
        final MultivaluedMap<String, String> params = new MetadataMap<>();
        final String postBody = IntStream
                .range(1, FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT + 1)
                .mapToObj(i -> FORM_PARAM + i + "=" + FORM_PARAM_VALUE + i)
                .collect(Collectors.joining("&"));
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> FormUtils.populateMapFromString(params, mockMessage, postBody, 
                StandardCharsets.UTF_8.name(), false));
        assertThat(ex.getResponse().getStatus(), equalTo(413)); /* Request Entity Too Large */

        // Increase the limit and try again
        when(mockMessage.getContextualProperty("maxFormParameterCount"))
            .thenReturn(Integer.toString(FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT + 1));
        FormUtils.populateMapFromString(params, mockMessage, null, StandardCharsets.UTF_8.name(), false, mockRequest);
        assertEquals(500, params.size());
    }
    
    @Test
    public void populateMapFromMultiPartExceedsDefaultMaxFormParams() {
        mockObjects(null, FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT);

        final MultivaluedMap<String, String> params = new MetadataMap<>();
        final MultipartBody body = new MultipartBody(IntStream
                .range(1, FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT + 1)
                .mapToObj(i -> {
                    final MultivaluedMap<String, String> headers = new MetadataMap<>();
                    headers.putSingle("Content-ID", UUID.randomUUID().toString());
                    return new Attachment(InputStream.nullInputStream(), headers);
                })
                .collect(Collectors.toList()));
        final WebApplicationException ex = assertThrows(WebApplicationException.class,
            () -> FormUtils.populateMapFromMultipart(params, body, mockMessage, false));
        assertThat(ex.getResponse().getStatus(), equalTo(413)); /* Request Entity Too Large */

        // Increase the limit and try again
        when(mockMessage.getContextualProperty("maxFormParameterCount"))
            .thenReturn(Integer.toString(FormUtils.DEFAULT_MAX_FORM_PARAM_COUNT + 1));
        FormUtils.populateMapFromMultipart(params, body, mockMessage, false);
        assertEquals(500, params.size());
    }

    private void mockObjects(String formPropertyValue, int params) {
        final ExchangeImpl exchange = new ExchangeImpl();

        mockMessage = mock(Message.class);
        when(mockMessage.getContextualProperty(FormUtils.FORM_PARAMS_FROM_HTTP_PARAMS))
            .thenReturn(formPropertyValue);
        when(mockMessage.getExchange()).thenReturn(exchange);
        when(mockMessage.put(FormUtils.FORM_PARAM_MAP_DECODED, true))
            .thenReturn(null);
        exchange.setInMessage(mockMessage);

        mockRequest = mock(HttpServletRequest.class);
        String[] httpParamNames =  IntStream.range(1, params + 1)
            .mapToObj(i -> "httpParam" + i)
            .toArray(String[]::new);
        Enumeration<String> httpParamsEnum = Collections.enumeration(Arrays.asList(httpParamNames));
        when(mockRequest.getParameterNames()).thenReturn(httpParamsEnum);
        for (int i = 1; i <= httpParamNames.length; ++i) {
            when(mockRequest.getParameterValues(HTTP_PARAM + i)).thenReturn(new String[] {HTTP_PARAM_VALUE + i});
        }
    }
}