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
package org.apache.cxf.binding.xml;

import org.apache.cxf.binding.Binding;
import org.apache.cxf.interceptor.AbstractBasicInterceptorProvider;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.message.XMLMessage;
import org.apache.cxf.service.model.BindingInfo;

public class XMLBinding extends AbstractBasicInterceptorProvider implements Binding {

    private BindingInfo bindingInfo;

    public XMLBinding(BindingInfo bindingInfo) {
        super();
        this.bindingInfo = bindingInfo;
    }

    public BindingInfo getBindingInfo() {
        return bindingInfo;
    }

    public Message createMessage() {
        return createMessage(new MessageImpl());
    }

    public Message createMessage(Message m) {
        if (!m.containsKey(Message.CONTENT_TYPE)) {

            String ct = null;

            // Should this be done in ServiceInvokerInterceptor to support a case where the
            // response content type is detected early on the inbound chain for all the bindings ?
            Exchange exchange = m.getExchange();
            if (exchange != null) {
                ct = (String)exchange.get(Message.CONTENT_TYPE);
            }
            if (ct == null) {
                ct = "text/xml";
            }
            m.put(Message.CONTENT_TYPE, ct);
        }
        return new XMLMessage(m);
    }
}
