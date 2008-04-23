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

package org.apache.cxf.binding.soap.interceptor;

import java.net.URI;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.HeaderUtil;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.phase.Phase;

public class MustUnderstandInterceptor extends AbstractSoapInterceptor {
    private static final Logger LOG = LogUtils.getL7dLogger(MustUnderstandInterceptor.class);

    private static final ResourceBundle BUNDLE = LOG.getResourceBundle();

    public MustUnderstandInterceptor() {
        super(Phase.PRE_PROTOCOL);
    }
    public MustUnderstandInterceptor(String phase) {
        super(phase);
    }

    public void handleMessage(SoapMessage soapMessage) {
        SoapVersion soapVersion = soapMessage.getVersion();              
        Set<Header> mustUnderstandHeaders = new HashSet<Header>();
        Set<URI> serviceRoles = new HashSet<URI>();
        Set<QName> notUnderstandQNames = new HashSet<QName>();
        Set<QName> mustUnderstandQNames = new HashSet<QName>();

        buildMustUnderstandHeaders(mustUnderstandHeaders, soapMessage, serviceRoles);
        initServiceSideInfo(mustUnderstandQNames, soapMessage, serviceRoles);
        
        if (!checkUnderstand(mustUnderstandHeaders, mustUnderstandQNames, notUnderstandQNames)) {
            StringBuffer sb = new StringBuffer(300);
            int pos = 0;
            for (QName qname : notUnderstandQNames) {
                pos = pos + qname.toString().length() + 2;
                sb.append(qname.toString() + ", ");
            }
            sb.delete(pos - 2, pos);
            throw new SoapFault(new Message("MUST_UNDERSTAND", BUNDLE, sb.toString()),
                                soapVersion.getMustUnderstand());
        }
    }

    private void initServiceSideInfo(Set<QName> mustUnderstandQNames, SoapMessage soapMessage,
                    Set<URI> serviceRoles) {

        Set<QName> paramHeaders = HeaderUtil.getHeaderQNameInOperationParam(soapMessage);

        if (paramHeaders != null) {
            mustUnderstandQNames.addAll(paramHeaders);
        }
        for (Interceptor interceptorInstance : soapMessage.getInterceptorChain()) {
            if (interceptorInstance instanceof SoapInterceptor) {
                SoapInterceptor si = (SoapInterceptor) interceptorInstance;
                Set<URI> roles = si.getRoles();
                if (roles != null) {
                    serviceRoles.addAll(roles);
                }
                Set<QName> understoodHeaders = si.getUnderstoodHeaders();
                if (understoodHeaders != null) {
                    mustUnderstandQNames.addAll(understoodHeaders);
                }
            }
        }
    }

    private void buildMustUnderstandHeaders(Set<Header> mustUnderstandHeaders,
                                            SoapMessage soapMessage,
                                            Set<URI> serviceRoles) {
        for (Header header : soapMessage.getHeaders()) {
            if (header instanceof SoapHeader && ((SoapHeader)header).isMustUnderstand()) {
                String role = ((SoapHeader)header).getActor();
                if (role != null) {
                    role = role.trim();
                    if (role.equals(soapMessage.getVersion().getNextRole())
                                    || role.equals(soapMessage.getVersion().getUltimateReceiverRole())) {
                        mustUnderstandHeaders.add(header);
                    } else {
                        for (URI roleFromBinding : serviceRoles) {
                            if (role.equals(roleFromBinding.toString())) {
                                mustUnderstandHeaders.add(header);
                            }
                        }
                    }
                } else {
                    // if role omitted, the soap node is ultimate receiver,
                    // needs to understand
                    mustUnderstandHeaders.add(header);
                }
            }
        }
    }

    private boolean checkUnderstand(Set<Header> mustUnderstandHeaders,
                                    Set<QName> mustUnderstandQNames,
                                    Set<QName> notUnderstandQNames) {

        for (Header header : mustUnderstandHeaders) {
            QName qname = header.getName();
            if (!mustUnderstandQNames.contains(qname)) {
                notUnderstandQNames.add(qname);
            }
        }
        if (notUnderstandQNames.size() > 0) {
            return false;
        }
        return true;
    }
}
