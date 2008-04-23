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

package org.apache.cxf.binding.soap;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.xml.namespace.QName;

import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.tools.common.extensions.soap.SoapHeader;
import org.apache.cxf.tools.util.SOAPBindingUtil;

public final class HeaderUtil {
    private static final String HEADERS_PROPERTY = HeaderUtil.class.getName() + ".HEADERS";

    private HeaderUtil() {
    }

    private static Set<QName> getHeaderParts(BindingMessageInfo bmi) {
        Object obj = bmi.getProperty(HEADERS_PROPERTY);
        if (obj == null) {
            Set<QName> set = getHeaderQNames(bmi);
            bmi.setProperty(HEADERS_PROPERTY, set);
            return set;
        }
        return CastUtils.cast((Set<?>)obj);
    }

    private static Set<QName> getHeaderQNames(BindingMessageInfo bmi) {
        Set<QName> set = new HashSet<QName>();
        List<MessagePartInfo> mps = bmi.getMessageInfo().getMessageParts();
        List<ExtensibilityElement> extList = bmi.getExtensors(ExtensibilityElement.class);
        if (extList != null) {
            for (ExtensibilityElement ext : extList) {
                if (SOAPBindingUtil.isSOAPHeader(ext)) {
                    SoapHeader header = SOAPBindingUtil.getSoapHeader(ext);
                    String pn = header.getPart();
                    for (MessagePartInfo mpi : mps) {
                        if (pn.equals(mpi.getName().getLocalPart())) {
                            if (mpi.isElement()) {
                                set.add(mpi.getElementQName());
                            } else {
                                set.add(mpi.getTypeQName());
                            }
                            break;
                        }
                    }
                }
            }
        }
        return set;
    }

    public static Set<QName> getHeaderQNameInOperationParam(SoapMessage soapMessage) {
        Set<QName> headers = new HashSet<QName>();
        BindingOperationInfo bop = soapMessage.getExchange()
            .get(BindingOperationInfo.class);
        if (bop != null) {
            if (bop.getInput() != null) {
                headers.addAll(getHeaderParts(bop.getInput()));
            }
            if (bop.getOutput() != null) {
                headers.addAll(getHeaderParts(bop.getOutput()));
            }
        }
        return headers;
    }
}
