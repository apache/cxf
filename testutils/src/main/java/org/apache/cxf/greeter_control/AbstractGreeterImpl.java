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

package org.apache.cxf.greeter_control;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.Response;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.types.FaultDetail;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.greeter_control.types.PingMeResponse;
import org.apache.cxf.greeter_control.types.SayHiResponse;

/**
 *
 */

public class AbstractGreeterImpl implements Greeter {

    private static final Logger LOG = LogUtils.getLogger(AbstractGreeterImpl.class);
    private long delay;
    private String lastOnewayArg;
    private boolean throwAlways;
    private AtomicBoolean useLastOnewayArg = new AtomicBoolean();
    private int pingMeCount;

    public long getDelay() {
        return delay;
    }

    public void setDelay(long d) {
        delay = d;
    }

    public void resetLastOnewayArg() {
        synchronized (this) {
            lastOnewayArg = null;
        }
    }

    public void useLastOnewayArg(boolean use) {
        useLastOnewayArg.set(use);
    }

    public void setThrowAlways(boolean t) {
        throwAlways = t;
    }

    public String greetMe(String arg0) {
        LOG.fine("Executing operation greetMe with parameter: " + arg0);
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                // ignore
            }
        }
        String result = useLastOnewayArg.get() ? lastOnewayArg : arg0.toUpperCase();
        LOG.fine("returning: " + result);
        return result;
    }

    public Future<?> greetMeAsync(String arg0, AsyncHandler<GreetMeResponse> arg1) {
        return null;
    }

    public Response<GreetMeResponse> greetMeAsync(String arg0) {
        return null;
    }

    public void greetMeOneWay(String arg0) {
        synchronized (this) {
            lastOnewayArg = arg0;
        }
        LOG.fine("Executing operation greetMeOneWay with parameter: " + arg0);
    }

    public void pingMe() throws PingMeFault {
        pingMeCount++;
        if ((pingMeCount % 2) == 0 || throwAlways) {
            LOG.fine("Throwing PingMeFault while executiong operation pingMe");
            FaultDetail fd = new FaultDetail();
            fd.setMajor((short)2);
            fd.setMinor((short)1);
            throw new PingMeFault("Pings succeed only every other time.", fd);
        }
        LOG.fine("Executing operation pingMe");
    }

    public Response<PingMeResponse> pingMeAsync() {
        return null;
    }

    public Future<?> pingMeAsync(AsyncHandler<PingMeResponse> arg0) {
        return null;
    }

    public String sayHi() {
        return null;
    }

    public Response<SayHiResponse> sayHiAsync() {
        return null;
    }

    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> arg0) {
        return null;
    }

}
