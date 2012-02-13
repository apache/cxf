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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.wsn.util.IdGenerator;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_2.UnableToCreatePullPointFaultType;
import org.oasis_open.docs.wsn.bw_2.CreatePullPoint;
import org.oasis_open.docs.wsn.bw_2.UnableToCreatePullPointFault;
import org.oasis_open.docs.wsn.bw_2.UnableToDestroyPullPointFault;

@WebService(endpointInterface = "org.oasis_open.docs.wsn.bw_2.CreatePullPoint")
public abstract class AbstractCreatePullPoint extends AbstractEndpoint 
    implements CreatePullPoint, CreatePullPointMBean {

    private static final Logger LOGGER = LogUtils.getL7dLogger(AbstractCreatePullPoint.class);

    private IdGenerator idGenerator;

    private Map<String, AbstractPullPoint> pullPoints;

    public AbstractCreatePullPoint(String name) {
        super(name);
        idGenerator = new IdGenerator();
        pullPoints = new ConcurrentHashMap<String, AbstractPullPoint>();
    }

    public void init() throws Exception {
        register();
    }

    public void destroy() throws Exception {
        unregister();
    }
    
    public List<String> getPullEndpoints() {
        return new ArrayList<String>(pullPoints.keySet());
    }

    public EndpointMBean getPullEndpoint(String name) {
        return pullPoints.get(name);
    }

    @WebMethod(operationName = "CreatePullPoint")
    @WebResult(name = "CreatePullPointResponse", 
               targetNamespace = "http://docs.oasis-open.org/wsn/b-2", 
               partName = "CreatePullPointResponse")
    public CreatePullPointResponse createPullPoint(
            @WebParam(name = "CreatePullPoint", 
                      targetNamespace = "http://docs.oasis-open.org/wsn/b-2", 
                      partName = "CreatePullPointRequest")
            org.oasis_open.docs.wsn.b_2.CreatePullPoint createPullPointRequest)
        throws UnableToCreatePullPointFault {

        LOGGER.finest("CreatePullEndpoint");
        return handleCreatePullPoint(createPullPointRequest, null);
    }

    public CreatePullPointResponse handleCreatePullPoint(
            org.oasis_open.docs.wsn.b_2.CreatePullPoint createPullPointRequest, 
            EndpointManager manager) throws UnableToCreatePullPointFault {
        AbstractPullPoint pullPoint = null;
        boolean success = false;
        try {
            pullPoint = createPullPoint(createPullPointName(createPullPointRequest));
            pullPoint.setCreatePullPoint(this);
            pullPoints.put(pullPoint.getAddress(), pullPoint);
            pullPoint.create(createPullPointRequest);
            if (manager != null) {
                pullPoint.setManager(manager);
            }
            pullPoint.register();
            CreatePullPointResponse response = new CreatePullPointResponse();
            response.setPullPoint(pullPoint.getEpr());
            success = true;
            return response;
        } catch (EndpointRegistrationException e) {
            LOGGER.log(Level.WARNING, "Unable to register new endpoint", e);
            UnableToCreatePullPointFaultType fault = new UnableToCreatePullPointFaultType();
            throw new UnableToCreatePullPointFault("Unable to register new endpoint", fault, e);
        } finally {
            if (!success && pullPoint != null) {
                pullPoints.remove(pullPoint.getAddress());
                try {
                    pullPoint.destroy();
                } catch (UnableToDestroyPullPointFault e) {
                    LOGGER.log(Level.INFO, "Error destroying pullPoint", e);
                }
            }
        }
    }

    protected String createPullPointName(org.oasis_open.docs.wsn.b_2.CreatePullPoint createPullPointRequest) {
        return idGenerator.generateSanitizedId();
    }

    public void destroyPullPoint(String address) throws UnableToDestroyPullPointFault {
        AbstractPullPoint pullPoint = pullPoints.remove(address);
        if (pullPoint != null) {
            pullPoint.destroy();
        }
    }

    protected abstract AbstractPullPoint createPullPoint(String name);

}
