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
package org.apache.cxf.jca.outbound;

import java.io.Serializable;

import javax.resource.Referenceable;
import javax.resource.ResourceException;


/**
 * Provides methods to create a {@link CXFConnection} object that provides access
 * to a Web Service defined from the supplied specifications.  A CXFConnectionFactory 
 * is returned from an environment naming context JNDI lookup by the Application 
 * Server.
 */
public interface CXFConnectionFactory extends Serializable, Referenceable  {
    
    /**
     * Creates a CXFConnection object which allows access to CXF web service based on 
     * the CXFConnectionSpec object.  Required CXFConnectionSpec fields are wsdlURL, 
     * serviceClass, endpointName, and serviceName.  Each connection returned by this 
     * method MUST be closed by calling the {@link CXFConnection#close()} when it is no 
     * longer needed.  
     * 
     * @param spec
     * @return CXFConnection 
     * @throws ResourceException
     */
    CXFConnection getConnection(CXFConnectionSpec spec) throws ResourceException;

}
