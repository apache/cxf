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
package org.apache.cxf.systest.jaxws;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.ws.WebFault;

@WebFault()
public class ServiceTestFault extends Exception {
    private ServiceTestDetails details;
    
    public ServiceTestFault(String msg) {
        super(msg);
    }
    public ServiceTestFault(String msg, ServiceTestDetails details) {
        super(msg);
        this.details = details;
    }
    public ServiceTestFault(ServiceTestDetails details) {
        super();
        this.details = details;
    }
    public ServiceTestDetails getFaultInfo() {
        return details;
    }
    
    @XmlRootElement(name = "ServiceTestDetails")
    public static class ServiceTestDetails {
        private long id;
        
        public ServiceTestDetails() {
        }
        public ServiceTestDetails(int i) {
            id = i;
        }
        
        public long getId() {
            return id;
        }
        public void setId(long id) {
            this.id = id;
        }
    }
}
