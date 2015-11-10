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

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.message.Message;
import org.easymock.EasyMock;

import org.junit.Assert;
import org.junit.Test;

public class FormUtilsTest extends Assert {

    private static final String HTTP_PARAM1 = "httpParam1";
    private static final String HTTP_PARAM2 = "httpParam2";
    private static final String HTTP_PARAM_VALUE1 = "httpValue1";
    private static final String HTTP_PARAM_VALUE2 = "httpValue2";
    
    private static final String FORM_PARAM1 = "formParam1";
    private static final String FORM_PARAM2 = "formParam2";
    private static final String FORM_PARAM_VALUE1 = "formValue1";
    private static final String FORM_PARAM_VALUE2 = "formValue2";

    private Message mockMessage;
    private HttpServletRequest mockRequest;

    @Test
    public void populateMapFromStringFromHTTP() {
        mockObjects(null);
        EasyMock.replay(mockMessage, mockRequest);

        MultivaluedMap<String, String> params = new MetadataMap<String, String>();
        FormUtils.populateMapFromString(params, mockMessage, null, StandardCharsets.UTF_8.name(), 
                                        false, mockRequest);
        
        assertEquals(2, params.size());
        assertEquals(HTTP_PARAM_VALUE1, params.get(HTTP_PARAM1).iterator().next());
        assertEquals(HTTP_PARAM_VALUE2, params.get(HTTP_PARAM2).iterator().next());        
    }

    public void populateMapFromStringFromHTTPWithProp() {
        mockObjects("false");
        EasyMock.replay(mockMessage, mockRequest);

        MultivaluedMap<String, String> params = new MetadataMap<String, String>();
        FormUtils.populateMapFromString(params, mockMessage, null, StandardCharsets.UTF_8.name(), 
                                        false, mockRequest);
        
        assertEquals(0, params.size());
    }

    @Test
    public void populateMapFromStringFromBody() {
        mockObjects(null);
        EasyMock.replay(mockMessage, mockRequest);

        MultivaluedMap<String, String> params = new MetadataMap<String, String>();
        String postBody = FORM_PARAM1 + "=" + FORM_PARAM_VALUE1 + "&" + FORM_PARAM2 + "=" + FORM_PARAM_VALUE2;
        FormUtils.populateMapFromString(params, mockMessage, postBody, StandardCharsets.UTF_8.name(), 
                                        false, mockRequest);
        
        assertEquals(2, params.size());
        assertEquals(FORM_PARAM_VALUE1, params.get(FORM_PARAM1).iterator().next());
        assertEquals(FORM_PARAM_VALUE2, params.get(FORM_PARAM2).iterator().next());        
    }

    
    private void mockObjects(String formPropertyValue) {
        mockMessage = EasyMock.createMock(Message.class);
        EasyMock.expect(mockMessage.getContextualProperty(FormUtils.FORM_PARAMS_FROM_HTTP_PARAMS))
            .andReturn(formPropertyValue).anyTimes();
        EasyMock.expect(mockMessage.getExchange()).andReturn(null).anyTimes();
        
        mockRequest = EasyMock.createMock(HttpServletRequest.class);
        String[] httpParamNames = {HTTP_PARAM1, HTTP_PARAM2};
        Enumeration<String> httpParamsEnum = Collections.enumeration(Arrays.asList(httpParamNames));
        EasyMock.expect(mockRequest.getParameterNames()).andReturn(httpParamsEnum).anyTimes();
        EasyMock.expect(mockRequest.getParameterValues(HTTP_PARAM1)).andReturn(new String[] {HTTP_PARAM_VALUE1})
            .anyTimes();
        EasyMock.expect(mockRequest.getParameterValues(HTTP_PARAM2)).andReturn(new String[] {HTTP_PARAM_VALUE2})
            .anyTimes();        
    }
}
