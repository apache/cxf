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

package org.apache.cxf.binding.soap.model;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.common.WSDLConstants;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;


public class SoapBindingInfo extends BindingInfo {
    private SoapVersion soapVersion;

    private String style;

    private String transportURI;
    
    public SoapBindingInfo(ServiceInfo serv, String n) {
        this(serv, n, null);
        resolveSoapVersion(n);
    }

    public SoapBindingInfo(ServiceInfo serv, String n, SoapVersion soapVersion) {
        super(serv, n);

        this.soapVersion = soapVersion;
    }

    private void resolveSoapVersion(String n) {
        if (WSDLConstants.NS_SOAP11.equalsIgnoreCase(n)) {
            this.soapVersion = Soap11.getInstance();
        } else if (WSDLConstants.NS_SOAP12.equalsIgnoreCase(n)) {
            this.soapVersion = Soap12.getInstance();
        } else {
            throw new RuntimeException("Unknow bindingId: " + n + ". Can not resolve the SOAP version");
        }
    }


    public SoapVersion getSoapVersion() {
        if (soapVersion == null) {
            resolveSoapVersion(getBindingId());
        }
        return soapVersion;
    }

    public void setSoapVersion(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
    }

    public String getStyle() {
        return style;
    }

    public String getStyle(OperationInfo operation) {
        SoapOperationInfo opInfo = getOperation(operation.getName()).getExtensor(SoapOperationInfo.class);
        if (opInfo != null) {
            return opInfo.getStyle();
        } else {
            return style;
        }
    }

    public OperationInfo getOperationByAction(String action) {
        for (BindingOperationInfo b : getOperations()) {
            SoapOperationInfo opInfo = b.getExtensor(SoapOperationInfo.class);

            if (opInfo.getAction().equals(action)) {
                return b.getOperationInfo();
            }
        }

        return null;
    }

    /**
     * Get the soap action for an operation. Will never return null.
     * 
     * @param operation
     * @return
     */
    public String getSoapAction(OperationInfo operation) {
        BindingOperationInfo b = (BindingOperationInfo) getOperation(operation.getName());
        SoapOperationInfo opInfo = b.getExtensor(SoapOperationInfo.class);

        if (opInfo != null && opInfo.getAction() != null) {
            return opInfo.getAction();
        }

        return "";
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getTransportURI() {
        return transportURI;
    }

    public void setTransportURI(String transportURI) {
        this.transportURI = transportURI;
    }

}
