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
package org.apache.cxf.wsn;

import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.br_2.DestroyRegistration;
import org.oasis_open.docs.wsn.br_2.DestroyRegistrationResponse;
import org.oasis_open.docs.wsn.br_2.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.ResourceNotDestroyedFaultType;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.brw_2.ResourceNotDestroyedFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.brw_2.PublisherRegistrationManager")
public abstract class AbstractPublisher extends AbstractEndpoint implements PublisherRegistrationManager {

    protected W3CEndpointReference publisherReference;

    protected boolean demand;

    protected List<TopicExpressionType> topic;

    public AbstractPublisher(String name) {
        super(name);
    }

    public W3CEndpointReference getPublisherReference() {
        return publisherReference;
    }

    /**
     *
     * @param destroyRegistrationRequest
     * @return returns org.oasis_open.docs.wsn.br_1.DestroyResponse
     * @throws ResourceNotDestroyedFault
     * @throws ResourceUnknownFault
     */
    @WebMethod(operationName = "DestroyRegistration")
    @WebResult(name = "DestroyRegistrationResponse",
               targetNamespace = "http://docs.oasis-open.org/wsn/br-2",
               partName = "DestroyRegistrationResponse")
    public DestroyRegistrationResponse destroyRegistration(
            @WebParam(name = "DestroyRegistration",
                      targetNamespace = "http://docs.oasis-open.org/wsn/br-2",
                      partName = "DestroyRegistrationRequest")
            DestroyRegistration destroyRegistrationRequest)
        throws ResourceNotDestroyedFault, ResourceUnknownFault {

        destroy();
        return new DestroyRegistrationResponse();
    }

    public abstract void notify(NotificationMessageHolderType messageHolder);

    protected void destroy() throws ResourceNotDestroyedFault {
        try {
            unregister();
        } catch (EndpointRegistrationException e) {
            ResourceNotDestroyedFaultType fault = new ResourceNotDestroyedFaultType();
            throw new ResourceNotDestroyedFault("Error unregistering endpoint", fault, e);
        }
    }

    public void create(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault,
            PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault,
            TopicNotSupportedFault {
        validatePublisher(registerPublisherRequest);
        start();
    }

    protected void validatePublisher(RegisterPublisher registerPublisherRequest)
        throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault,
        PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {

        // Check consumer reference
        publisherReference = registerPublisherRequest.getPublisherReference();
        // Check topic
        topic = registerPublisherRequest.getTopic();
        // Check demand based
        demand = registerPublisherRequest.isDemand() != null
            && registerPublisherRequest.isDemand().booleanValue();
        
        if (demand) {
            PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
            throw new PublisherRegistrationFailedFault("On-demand publishing is not supported", fault);
        }
        
        // Check all parameters
        if (publisherReference == null && demand) {
            PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
            throw new PublisherRegistrationFailedFault("Invalid PublisherReference: null", fault);
        }
        if (demand && (topic == null || topic.isEmpty())) {
            InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
            throw new InvalidTopicExpressionFault(
                    "Must specify at least one topic for demand-based publishing", fault);
        }
    }

    protected abstract void start() throws PublisherRegistrationFailedFault;
}
