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


/**
 * The DispatchMDBMessageListener is intended to be used as the 
 * <messagelistener-type> of an <inbound-resourcesadapter>  in the resource 
 * adapter's deployment descriptor (ra.xml).  The default implementation 
 * class is {@link DispatchMDBMessageListenerImpl}.
 * The benefit of using DispatchMDBMessageListener is that users
 * are not required to put the Service Endpoint Interface (SEI) in the resource
 * adaptor's deployment descriptor.  Thus, users can leave the <messagelistener-type>
 * as org.apache.cxf.jca.inbound.DispatchMDBMessageListener for any their endpoints.
 */
public interface DispatchMDBMessageListener {

    /**
     * Looks up the target object by JNDI name.
     * 
     * @param targetJndiName
     * @return
     */
    Object lookupTargetObject(String targetJndiName) throws Exception;

}
