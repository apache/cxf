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

package org.apache.cxf.alarm_retrieval.client;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import v1.tmf854.fault.GetActiveAlarmsResponseT;
import v1.tmf854.fault.GetActiveAlarmsResponseT.ActiveAlarmList;

public class AsyncAlarmHandler implements AsyncHandler<GetActiveAlarmsResponseT> {

    private GetActiveAlarmsResponseT reply;

    public void handleResponse(Response<GetActiveAlarmsResponseT> response) {
        try {
            System.out.println("handling asynchronous response...");

            reply = response.get();
        } catch (InterruptedException ex) {
            System.err.println("InterruptedException while awaiting response."
                + ex);
        } catch (java.util.concurrent.ExecutionException ex) {
            System.err.println("Operation received ExecutionException." + ex);
        }
    }

    public ActiveAlarmList getResponse() {
        if (reply != null) {
            return reply.getActiveAlarmList();
        }
        return null;
    }

}
