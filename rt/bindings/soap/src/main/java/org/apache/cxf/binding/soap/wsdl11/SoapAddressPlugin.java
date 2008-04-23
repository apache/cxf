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

package org.apache.cxf.binding.soap.wsdl11;

import java.util.Map;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;

import org.apache.cxf.tools.common.ToolConstants;
import org.apache.cxf.tools.common.extensions.soap.SoapAddress;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.wsdl.AbstractWSDLPlugin;

public final class SoapAddressPlugin extends AbstractWSDLPlugin {

    public ExtensibilityElement createExtension(Map<String, Object> args) throws WSDLException {
        return createExtension(optionSet(args, ToolConstants.CFG_SOAP12),
                               getOption(args, ToolConstants.CFG_ADDRESS));
    }

    public ExtensibilityElement createExtension(final boolean isSOAP12,
                                                final String address) throws WSDLException {
        SoapAddress soapAddress = null;

        soapAddress = SOAPBindingUtil.createSoapAddress(registry, isSOAP12);
        
        soapAddress.setLocationURI(address);

        return soapAddress;
    }
}
