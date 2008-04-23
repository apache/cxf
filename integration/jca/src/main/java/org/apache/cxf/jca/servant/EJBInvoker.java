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

package org.apache.cxf.jca.servant;

import java.lang.reflect.Method;

import javax.ejb.EJBHome;

import org.apache.cxf.message.Exchange;
import org.apache.cxf.service.invoker.AbstractInvoker;


public class EJBInvoker extends AbstractInvoker {
    
    private static final Object[] EMPTY_OBJECT = new Object[0];
    
    private EJBHome home;
    
    private Method createMethod;
     
    public EJBInvoker(EJBHome home) {
        this.home = home;
        try {
            if (!home.getEJBMetaData().isSession() || !home.getEJBMetaData().isStatelessSession())
            {
                throw new IllegalArgumentException("home must be for a stateless session bean");
            }
            createMethod = home.getClass().getMethod("create", new Class[0]);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to initialize invoker: " + ex);
        }
    }
    
    
    @Override
    public Object getServiceObject(Exchange context) {
        Object ejbObject = null;
        try {
            ejbObject = createMethod.invoke(home, EMPTY_OBJECT);
        } catch (Exception e) {
            throw new RuntimeException("Error in creating EJB Object");
        }
        return ejbObject;
    }

}
