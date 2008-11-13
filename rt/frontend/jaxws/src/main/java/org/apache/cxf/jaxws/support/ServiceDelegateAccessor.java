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

package org.apache.cxf.jaxws.support;

import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.Service;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxws.ServiceImpl;

/**
 * A utility that allows access to the 'private' implementation specific delegate
 * of a Service. Usefull when extensions to the JAXWS Service supported methods
 * are required.
 */
public final class ServiceDelegateAccessor {

    private static final Logger LOG = LogUtils.getL7dLogger(ServiceDelegateAccessor.class);

    private static final String DELEGATE_FIELD_NAME = "delegate";
    private static final String DELEGATE_FIELD_NAME2 = "_delegate";

    private ServiceDelegateAccessor() {        
    }
    
    /**
     * Get the delegate reference from the Service private field. This method
     * uses Field.setAccessible() which, in the presence of a SecurityManager,
     * requires the suppressAccessChecks permission
     * 
     * @param service the taraget service
     * @return the implementation delegate
     * @throws WebServiceException if access to the field fails for any reason
     */
    public static ServiceImpl get(Service service) {
        ServiceImpl delegate = null;
        try {
            Field delegateField = Service.class.getDeclaredField(DELEGATE_FIELD_NAME);
            delegateField.setAccessible(true);
            delegate = (ServiceImpl)delegateField.get(service);
        } catch (Exception e) {
            try {
                Field delegateField = Service.class.getDeclaredField(DELEGATE_FIELD_NAME2);
                delegateField.setAccessible(true);
                delegate = (ServiceImpl)delegateField.get(service);
            } catch (Exception e2) {
                WebServiceException wse = new WebServiceException("Failed to access Field named "
                                                                  + DELEGATE_FIELD_NAME 
                                                                  + " of Service instance "
                                                                  + service, e);
                LOG.log(Level.SEVERE, e.getMessage(), e);
                throw wse;                
            }
        }
        return delegate;
    }
}
