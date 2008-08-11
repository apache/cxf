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

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import javax.xml.namespace.QName;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;

public class SoapFault extends Fault {

    public static final QName ATTACHMENT_IO = new QName(Soap12.SOAP_NAMESPACE, "AttachmentIOError");

    /**
     * "The message was incorrectly formed or did not contain the appropriate
     * information in order to succeed." -- SOAP 1.2 Spec
     */ 

    /**
     * A SOAP 1.2 only fault code. <p/> "The message could not be processed for
     * reasons attributable to the processing of the message rather than to the
     * contents of the message itself." -- SOAP 1.2 Spec <p/> If this message is
     * used in a SOAP 1.1 Fault it will most likely (depending on the
     * FaultHandler) be mapped to "Sender" instead.
     */ 

    private QName subCode;
    private String role;
    private String node;
    private Map<String, String> namespaces = new HashMap<String, String>();

    public SoapFault(Message message, Throwable throwable, QName faultCode) {
        super(message, throwable, faultCode);
    }

    public SoapFault(Message message, QName faultCode) {
        super(message, faultCode);
    }

    public SoapFault(String message, QName faultCode) {
        super(new Message(message, (ResourceBundle)null), faultCode);
    }

    public SoapFault(String message, Throwable t, QName faultCode) {
        super(new Message(message, (ResourceBundle)null), t, faultCode);
    }

    
    public String getCodeString(String prefix, String defaultPrefix) {
        return getFaultCodeString(prefix, defaultPrefix, getFaultCode());
    }
    
    public String getSubCodeString(String prefix, String defaultPrefix) {
        return getFaultCodeString(prefix, defaultPrefix, subCode);
    }
    
    private String getFaultCodeString(String prefix, String defaultPrefix, QName fCode) {
        String codePrefix = null;
        if (StringUtils.isEmpty(prefix)) {
            codePrefix = fCode.getPrefix();
            if (StringUtils.isEmpty(codePrefix)) {
                codePrefix = defaultPrefix;
            }
        } else {
            codePrefix = prefix;
        }
        
        return codePrefix + ":" + fCode.getLocalPart();        
    }

    public String getReason() {
        return getMessage();
    }

    /**
     * Returns the fault actor.
     * 
     * @return the fault actor.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the fault actor.
     * 
     * @param actor the actor.
     */
    public void setRole(String actor) {
        this.role = actor;
    }
    
    public String getNode() {
        return node;
    }

    public void setNode(String n) {
        this.node = n;
    }    

    /**
     * Returns the SubCode for the Fault Code.
     * 
     * @return The SubCode element as detailed by the SOAP 1.2 spec.
     */
    public QName getSubCode() {
        return subCode;
    }

    /**
     * Sets the SubCode for the Fault Code.
     * 
     * @param subCode The SubCode element as detailed by the SOAP 1.2 spec.
     */
    public void setSubCode(QName subCode) {
        this.subCode = subCode;
    }

    public Map<String, String> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, String> namespaces) {
        this.namespaces = namespaces;
    }

    public static SoapFault createFault(Fault f, SoapVersion v) {
        if (f instanceof SoapFault) {
            //make sure the fault code is per spec
            //if it's one of our internal codes, map it to the proper soap code
            if (f.getFaultCode().getNamespaceURI().equals(Fault.FAULT_CODE_CLIENT.getNamespaceURI())) {
                QName fc = f.getFaultCode();
                if (Fault.FAULT_CODE_CLIENT.equals(fc)) {
                    fc = v.getSender();
                } else if (Fault.FAULT_CODE_SERVER.equals(fc)) { 
                    fc = v.getReceiver();
                }
                f.setFaultCode(fc);
            }
            return (SoapFault)f;
        }

        QName fc = f.getFaultCode();
        if (Fault.FAULT_CODE_CLIENT.equals(fc)) {
            fc = v.getSender();
        } else if (Fault.FAULT_CODE_SERVER.equals(fc)) { 
            fc = v.getReceiver();
        }
        SoapFault soapFault = new SoapFault(new Message(f.getMessage(), (ResourceBundle)null),
                                            f.getCause(),
                                            fc);
        
        soapFault.setDetail(f.getDetail());
        return soapFault;
    }
}
