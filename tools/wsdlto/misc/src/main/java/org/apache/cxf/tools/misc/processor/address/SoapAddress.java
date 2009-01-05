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

import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.ToolContext;

public class SoapAddress extends XmlAddress {
    public Map<String, String> getNamespaces(final ToolContext context) {
        Map<String, String> ns = new HashMap<String, String>();
        if (context.optionSet(ToolConstants.CFG_SOAP12)) {
            ns.put(ToolConstants.CFG_SOAP12, WSDLConstants.NS_SOAP12);
        } else {
            ns.put("soap", WSDLConstants.NS_SOAP11);
        }
        return ns;
    }

    public Map<String, Object> buildAddressArguments(final ToolContext context) {
        Map<String, Object> args = new HashMap<String, Object>();

        args.putAll(super.buildAddressArguments(context));

        if (context.optionSet(ToolConstants.CFG_SOAP12)) {
            args.put(ToolConstants.CFG_SOAP12,
                     context.get(ToolConstants.CFG_SOAP12));
        }
        return args;
    }
}
