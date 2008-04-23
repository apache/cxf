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

package org.apache.cxf.tools.misc.processor.address;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;

public class JmsAddress implements Address {
    public Map<String, String> getNamespaces(final ToolContext context) {
        Map<String, String> ns = new HashMap<String, String>();
        ns.put("jms", ToolConstants.NS_JMS_ADDRESS);
        return ns;
    }

    public Map<String, Object> buildAddressArguments(final ToolContext context) {
        Map<String, Object> args = new HashMap<String, Object>();

        if (context.optionSet(ToolConstants.JMS_ADDR_DEST_STYLE)) {
            args.put(ToolConstants.JMS_ADDR_DEST_STYLE,
                     context.get(ToolConstants.JMS_ADDR_DEST_STYLE));
        }
        if (context.optionSet(ToolConstants.JMS_ADDR_INIT_CTX)) {
            args.put(ToolConstants.JMS_ADDR_INIT_CTX,
                     context.get(ToolConstants.JMS_ADDR_INIT_CTX));
        }
        if (context.optionSet(ToolConstants.JMS_ADDR_JNDI_DEST)) {
            args.put(ToolConstants.JMS_ADDR_JNDI_DEST,
                context.get(ToolConstants.JMS_ADDR_JNDI_DEST));
        }
        if (context.optionSet(ToolConstants.JMS_ADDR_JNDI_FAC)) {
            args.put(ToolConstants.JMS_ADDR_JNDI_FAC,
                     context.get(ToolConstants.JMS_ADDR_JNDI_FAC));
        }
        if (context.optionSet(ToolConstants.JMS_ADDR_JNDI_URL)) {
            args.put(ToolConstants.JMS_ADDR_JNDI_URL,
                     context.get(ToolConstants.JMS_ADDR_JNDI_URL));
        }
        if (context.optionSet(ToolConstants.JMS_ADDR_SUBSCRIBER_NAME)) {
            args.put(ToolConstants.JMS_ADDR_SUBSCRIBER_NAME,
                     context.get(ToolConstants.JMS_ADDR_SUBSCRIBER_NAME));
        }
        return args;
    }
}
