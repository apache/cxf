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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.Fault;
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
        Set<QName> notUnderstandHeaders = new HashSet<QName>();
        Set<Header> ultimateReceiverHeaders = new HashSet<Header>();
        Set<QName> mustUnderstandQNames = new HashSet<QName>();

        initServiceSideInfo(mustUnderstandQNames, soapMessage, serviceRoles);
        buildMustUnderstandHeaders(mustUnderstandHeaders, soapMessage,
                                   serviceRoles, ultimateReceiverHeaders);
        
        checkUnderstand(mustUnderstandHeaders, mustUnderstandQNames, notUnderstandHeaders);
        
        if (!notUnderstandHeaders.isEmpty()) {
            throw new SoapFault(new Message("MUST_UNDERSTAND", BUNDLE, notUnderstandHeaders),
                            soapVersion.getMustUnderstand());
        }
        if (!ultimateReceiverHeaders.isEmpty() && !isRequestor(soapMessage)) {
            checkUltimateReceiverHeaders(ultimateReceiverHeaders, mustUnderstandQNames, soapMessage);
        }
    }

    private void checkUltimateReceiverHeaders(Set<Header> ultimateReceiverHeaders,
                                              Set<QName> mustUnderstandQNames, 
                                              SoapMessage soapMessage) {
        soapMessage.getInterceptorChain()
            .add(new UltimateReceiverMustUnderstandInterceptor(mustUnderstandQNames));
        Object o = soapMessage.getContextualProperty("endpoint.handles.headers");
        if (o == null) {
            //The default here really should be to make o = "" and process
            //so any mustUnderstands are kill immediately. That will break
            //existing apps though.  Thus, it's a migration issue.
            return;
        }
        Collection<Object> o2;
        if (o instanceof Collection) {
            o2 = CastUtils.cast((Collection<?>)o);
        } else {
            o2 = Collections.singleton(o);
        }
        for (Object obj : o2) {
            QName qn;
            if (obj instanceof QName) {
                qn = (QName)obj;
            } else {
                qn = QName.valueOf((String)obj);
            }
            Iterator<Header> hit = ultimateReceiverHeaders.iterator();
            while (hit.hasNext()) {
                if (qn.equals(hit.next().getName())) {
                    hit.remove();
                }
            }
        }
        if (!ultimateReceiverHeaders.isEmpty()) {
            Set<QName> notFound = new HashSet<QName>();
            for (Header h : ultimateReceiverHeaders) {
                notFound.add(h.getName());
            }
            throw new SoapFault(new Message("MUST_UNDERSTAND", BUNDLE, notFound),
                                soapMessage.getVersion().getMustUnderstand());
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
                                            Set<URI> serviceRoles,
                                            Set<Header> ultimateReceiverHeaders) {
        for (Header header : soapMessage.getHeaders()) {
            if (header instanceof SoapHeader && ((SoapHeader)header).isMustUnderstand()) {
                String role = ((SoapHeader)header).getActor();
                if (!StringUtils.isEmpty(role)) {
                    role = role.trim();
                    if (role.equals(soapMessage.getVersion().getNextRole())) {
                        mustUnderstandHeaders.add(header);
                    } else if (role.equals(soapMessage.getVersion().getUltimateReceiverRole())) {
                        ultimateReceiverHeaders.add(header);                        
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
                    ultimateReceiverHeaders.add(header);
                }
            }
        }
    }

    private void checkUnderstand(Set<Header> mustUnderstandHeaders,
                                    Set<QName> mustUnderstandQNames,
                                    Set<QName> notUnderstandHeaders) {

        for (Header header : mustUnderstandHeaders) {
            QName qname = header.getName();
            if (!mustUnderstandQNames.contains(qname)) {
                notUnderstandHeaders.add(header.getName());
            }
        }
    }
    
    
    
    /**
     * 
     */
    private class UltimateReceiverMustUnderstandInterceptor extends AbstractSoapInterceptor {
        Set<QName> knownHeaders;
        public UltimateReceiverMustUnderstandInterceptor(Set<QName> knownHeaders) {
            super(Phase.INVOKE);
            this.knownHeaders = knownHeaders;
        }
        public void handleMessage(SoapMessage soapMessage) throws Fault {
            SoapVersion soapVersion = soapMessage.getVersion();
            Set<QName> notFound = new HashSet<QName>();
            List<Header> heads = soapMessage.getHeaders();
            
            for (Header header : heads) {
                if (header instanceof SoapHeader
                    && ((SoapHeader)header).isMustUnderstand()
                    && header.getDirection() == Header.Direction.DIRECTION_IN
                    && !knownHeaders.contains(header.getName())
                    && (StringUtils.isEmpty(((SoapHeader)header).getActor())
                        || soapVersion.getUltimateReceiverRole()
                            .equals(((SoapHeader)header).getActor()))) {
                    
                    notFound.add(header.getName());
                }
            }
            
            
            if (!notFound.isEmpty()) {
                throw new SoapFault(new Message("MUST_UNDERSTAND", BUNDLE, notFound),
                                soapVersion.getMustUnderstand());
            }            
        }

    }
}
