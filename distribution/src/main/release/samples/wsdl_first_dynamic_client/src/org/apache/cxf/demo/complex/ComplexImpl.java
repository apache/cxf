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

package org.apache.cxf.demo.complex;


import com.company.application.CompanyESBApplicationBiztalkAgentDetails4405AgentDetailsPrtSoap;
import com.company.application.GetAgentDetails;
import com.company.application.GetAgentDetailsResponse;

import agentwsresponse.agent.legacysystemservices.schemas.hitum.esb.company.AgentWSResponse;

/**
 * This is a trivial implementation of a service contributed in a bug report.  It's useful
 * as an example of a hard case of using dynamic client.
 */
public class ComplexImpl implements CompanyESBApplicationBiztalkAgentDetails4405AgentDetailsPrtSoap {

    /** {@inheritDoc}*/
    public GetAgentDetailsResponse getAgentDetails(GetAgentDetails parameters) {
        GetAgentDetailsResponse r = new GetAgentDetailsResponse();
        AgentWSResponse awr = new AgentWSResponse();
        
        int number = parameters.getPart().getAgentNumber();
        awr.setAgenceNumber(number);
        awr.setAgentName("Orange");
        
        r.setAgentWSResponse(awr);
        return r;
    }

}
