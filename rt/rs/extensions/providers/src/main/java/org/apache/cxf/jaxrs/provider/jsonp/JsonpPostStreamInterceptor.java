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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;

/**
 * Appends the jsonp callback to json responses when the '_jsonp' parameter has been set in the querystring.
 */
public class JsonpPostStreamInterceptor extends AbstractJsonpOutInterceptor {

    private String paddingEnd = ");";

    public JsonpPostStreamInterceptor() {
        super(Phase.POST_STREAM);
    }

    public void handleMessage(Message message) throws Fault {
        if (!StringUtils.isEmpty(getCallbackValue(message))) {
            writeValue(message, getPaddingEnd());
        }
    }

    public void setPaddingEnd(String paddingEnd) {
        this.paddingEnd = paddingEnd;
    }

    public String getPaddingEnd() {
        return paddingEnd;
    }


}
