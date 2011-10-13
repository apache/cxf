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
import java.util.Collections;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.staxutils.StaxUtils;

public abstract class AbstractSoapInterceptor extends AbstractPhaseInterceptor<SoapMessage> 
    implements SoapInterceptor {

    /**
     * @deprecated
     */
    public AbstractSoapInterceptor() {
        super(null);
    }
    
    public AbstractSoapInterceptor(String p) {
        super(p);
    }
    public AbstractSoapInterceptor(String i, String p) {
        super(i, p);
    }

    
    public Set<URI> getRoles() {
        return Collections.emptySet();
    }

    public Set<QName> getUnderstoodHeaders() {
        return Collections.emptySet();
    }
    
    protected String getFaultCodePrefix(XMLStreamWriter writer, QName faultCode) throws XMLStreamException {
        String codeNs = faultCode.getNamespaceURI();
        String prefix = null;
        if (codeNs.length() > 0) {
            prefix = StaxUtils.getUniquePrefix(writer, codeNs, true);
        }        
        return prefix;
    }
    
    protected void prepareStackTrace(SoapMessage message, SoapFault fault) throws Exception {
        String config = (String)message
            .getContextualProperty(org.apache.cxf.message.Message.FAULT_STACKTRACE_ENABLED);
        if (config != null && Boolean.valueOf(config).booleanValue() && fault.getCause() != null) {
            StringBuilder sb = new StringBuilder();
            Throwable throwable = fault.getCause();
            while (throwable != null) {
                for (StackTraceElement ste : throwable.getStackTrace()) {
                    sb.append(ste.getClassName() + "!" + ste.getMethodName() + "!" + ste.getFileName() + "!"
                          + ste.getLineNumber() + "\n");
                }
                throwable = throwable.getCause();
                if (throwable != null) {
                    sb.append("Caused by: " +  throwable.getClass().getCanonicalName() 
                              + " : " + throwable.getMessage() + "\n");
                }
            }
            Element detail = fault.getDetail();
            String soapNamespace = message.getVersion().getNamespace();
            if (detail == null) {
                Document doc = XMLUtils.newDocument();
                Element stackTrace = doc.createElementNS(
                    Fault.STACKTRACE_NAMESPACE, Fault.STACKTRACE);
                stackTrace.setTextContent(sb.toString());
                detail = doc.createElementNS(
                    soapNamespace, "detail");
                fault.setDetail(detail);
                detail.appendChild(stackTrace);
            } else {
                Element stackTrace = 
                    detail.getOwnerDocument().createElementNS(Fault.STACKTRACE_NAMESPACE,
                                                              Fault.STACKTRACE);
                stackTrace.setTextContent(sb.toString());
                detail.appendChild(stackTrace);
            }
        }
    }
}
