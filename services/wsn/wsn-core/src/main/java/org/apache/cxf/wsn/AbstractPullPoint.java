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

import java.math.BigInteger;
import java.util.List;
import java.util.logging.Logger;

import jakarta.jws.Oneway;
import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebResult;
import jakarta.jws.WebService;
import org.apache.cxf.common.logging.LogUtils;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.DestroyPullPoint;
import org.oasis_open.docs.wsn.b_2.DestroyPullPointResponse;
import org.oasis_open.docs.wsn.b_2.GetMessages;
import org.oasis_open.docs.wsn.b_2.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType;
import org.oasis_open.docs.wsn.bw_2.NotificationConsumer;
import org.oasis_open.docs.wsn.bw_2.PullPoint;
import org.oasis_open.docs.wsn.bw_2.UnableToCreatePullPointFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroyPullPointFault;
import org.oasis_open.docs.wsn.bw_2.UnableToGetMessagesFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.PullPoint")
public abstract class AbstractPullPoint extends AbstractEndpoint implements PullPoint, NotificationConsumer {

    private static final Logger LOGGER = LogUtils.getL7dLogger(AbstractPullPoint.class);

    protected AbstractCreatePullPoint createPullPoint;

    public AbstractPullPoint(String name) {
        super(name);
    }

    /**
     *
     * @param notify
     */
    @WebMethod(operationName = "Notify")
    @Oneway
    public void notify(
            @WebParam(name = "Notify",
                      targetNamespace = "http://docs.oasis-open.org/wsn/b-1",
                      partName = "Notify")
            Notify notify) {

        LOGGER.finest("Notify");
        for (NotificationMessageHolderType messageHolder : notify.getNotificationMessage()) {
            store(messageHolder);
        }
    }

    /**
     *
     * @param getMessagesRequest
     * @return returns org.oasis_open.docs.wsn.b_1.GetMessagesResponse
     * @throws ResourceUnknownFault
     * @throws UnableToGetMessagesFault
     */
    @WebMethod(operationName = "GetMessages")
    @WebResult(name = "GetMessagesResponse",
               targetNamespace = "http://docs.oasis-open.org/wsn/b-1",
               partName = "GetMessagesResponse")
    public GetMessagesResponse getMessages(
            @WebParam(name = "GetMessages",
                      targetNamespace = "http://docs.oasis-open.org/wsn/b-1",
                      partName = "GetMessagesRequest")
            GetMessages getMessagesRequest) throws ResourceUnknownFault, UnableToGetMessagesFault {

        LOGGER.finest("GetMessages");
        BigInteger max = getMessagesRequest.getMaximumNumber();
        List<NotificationMessageHolderType> messages = getMessages(max != null ? max.intValue() : 0);
        GetMessagesResponse response = new GetMessagesResponse();
        response.getNotificationMessage().addAll(messages);
        return response;
    }

    /**
     *
     * @param destroyPullPointRequest
     * @return returns org.oasis_open.docs.wsn.b_1.DestroyResponse
     * @throws ResourceUnknownFault
     * @throws UnableToDestroyPullPointFault
     */
    @WebMethod(operationName = "DestroyPullPoint")
    @WebResult(name = "DestroyPullPointResponse",
               targetNamespace = "http://docs.oasis-open.org/wsn/b-2",
               partName = "DestroyPullPointResponse")
    public DestroyPullPointResponse destroyPullPoint(
            @WebParam(name = "DestroyPullPoint",
                      targetNamespace = "http://docs.oasis-open.org/wsn/b-2",
                      partName = "DestroyPullPointRequest")
            DestroyPullPoint destroyPullPointRequest)
        throws ResourceUnknownFault, UnableToDestroyPullPointFault {

        LOGGER.finest("Destroy");
        createPullPoint.destroyPullPoint(getAddress());
        return new DestroyPullPointResponse();
    }

    public void create(CreatePullPoint createPullPointRequest) throws UnableToCreatePullPointFault {
    }

    protected abstract void store(NotificationMessageHolderType messageHolder);

    protected abstract List<NotificationMessageHolderType> getMessages(int max) throws ResourceUnknownFault,
            UnableToGetMessagesFault;

    protected void destroy() throws UnableToDestroyPullPointFault {
        try {
            unregister();
        } catch (EndpointRegistrationException e) {
            UnableToDestroyPullPointFaultType fault = new UnableToDestroyPullPointFaultType();
            throw new UnableToDestroyPullPointFault("Error unregistering endpoint", fault, e);
        }
    }

    public AbstractCreatePullPoint getCreatePullPoint() {
        return createPullPoint;
    }

    public void setCreatePullPoint(AbstractCreatePullPoint createPullPoint) {
        this.createPullPoint = createPullPoint;
    }
}
