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

import javax.servlet.http.HttpServletRequest;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * Sets a callback key in the message exchange for HTTP requests containing the '_jsonp' parameter in the
 * querystring.
 */
public class JsonpInInterceptor extends AbstractPhaseInterceptor<Message> {
    public static final String JSONP_TYPE = "application/x-javascript";
    public static final String CALLBACK_PARAM = "_jsonp";
    public static final String CALLBACK_KEY = "JSONP.CALLBACK";
    public static final String DEFAULT_CALLBACK_VALUE = "callback";

    private String callbackParam = CALLBACK_PARAM;
    private String defaultCallback = DEFAULT_CALLBACK_VALUE;
    private String acceptType;
    private String mediaType = JSONP_TYPE;
    
    public JsonpInInterceptor() {
        this(Phase.UNMARSHAL);
    }
    
    public JsonpInInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(Message message) throws Fault {
        String callbackValue = getCallbackValue(message);
        if (!StringUtils.isEmpty(callbackValue)) {
            if (getAcceptType() != null) {
                // may be needed to enforce the selection of 
                // JSON-awarenprovider
                message.put(Message.ACCEPT_CONTENT_TYPE, getAcceptType());
            }
            message.getExchange().put(CALLBACK_KEY, callbackValue);
        }
    }

    protected String getCallbackValue(Message message) {
        HttpServletRequest request = (HttpServletRequest) message.get("HTTP.REQUEST");
        String callback = request.getParameter(callbackParam);
        if (StringUtils.isEmpty(callback)) {
            String httpAcceptType = (String)message.get(Message.ACCEPT_CONTENT_TYPE);
            if (httpAcceptType != null && mediaType.equals(httpAcceptType)) {
                return defaultCallback;
            }
        }
        return callback;
    }
    
    public void setCallbackParam(String callbackParam) {
        this.callbackParam = callbackParam;
    }

    public String getCallbackParam() {
        return callbackParam;
    }

    public void setAcceptType(String acceptType) {
        this.acceptType = acceptType;
    }

    public String getAcceptType() {
        return acceptType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setDefaultCallback(String defaultCallback) {
        this.defaultCallback = defaultCallback;
    }

    public String getDefaultCallback() {
        return defaultCallback;
    }
}
