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
package org.apache.cxf.jca.inbound;

import java.lang.reflect.Method;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;
import javax.naming.InitialContext;

/**
 * DispatchMDBMessageListenerImpl supports dispatching of calls to a 
 * Stateless Session Bean.
 *  
 * DispatchMDBMessageListenerImpl is intended to be used as the <ejb-class> of
 * the <message-driven> bean in the resource adaptor's deployment descriptor
 * (ra.xml).  When it is used, the <messaging-type> should be set to 
 * org.apache.cxf.jca.inbound.DispatchMDBMessageListener.  Also, the resource
 * adaptor's deployment descriptor should specify the same interface
 * (org.apache.cxf.jca.inbound.DispatchMDBMessageListener) in the 
 * <messagelistener-type> in order to activate the inbound facade endpoint.  
 * Since the Message Driven Bean is used to activate the inbound 
 * endpoint facade by CXF JCA connector, all the required resources (such as, 
 * service endpoint interface class, WSDL, or bus configuration) should be put 
 * in the same jar with the Message Driven Bean. 
 */
public class DispatchMDBMessageListenerImpl 
    implements MessageDrivenBean, DispatchMDBMessageListener {
    
    private static final long serialVersionUID = -8428728265893081763L;

    /**
     * Looks up the target object by EJB local reference.
     */
    public Object lookupTargetObject(String targetJndiName) throws Exception {
        Object home = new InitialContext().lookup(targetJndiName);
        Method method = home.getClass().getMethod("create", new Class[0]);
        return method.invoke(home, new Object[0]);
    }

    //---------------- EJB Methods

    public void ejbCreate() {
    }

    public void ejbRemove() {
    }

    public void setMessageDrivenContext(MessageDrivenContext mdc) {
    }

   

}
