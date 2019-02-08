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

package org.apache.cxf.management.counters;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.management.InstrumentationManager;
import org.apache.cxf.management.interceptor.ResponseTimeMessageInInterceptor;
import org.apache.cxf.management.interceptor.ResponseTimeMessageInvokerInterceptor;
import org.apache.cxf.management.interceptor.ResponseTimeMessageOutInterceptor;

/* Counters are created and managed by CounterRepository
 * If a counter which is queried form the counterRepository is not exist,
 * the CounterRepository will create a counter and expose it to the JMX
 * Because we can get the instance of the counter object,
 * it is not need to qurey the counter object from JMX
 * */
public class CounterRepository {
    private static final Logger LOG = LogUtils.getL7dLogger(CounterRepository.class);

    private Map<ObjectName, Counter> counters;
    private Bus bus;
    private Lock counterCreationLock = new ReentrantLock();

    public CounterRepository() {
        counters = new ConcurrentHashMap<>();
    }

    public void setBus(Bus b) {
        bus = b;
        if (bus != null) {
            registerInterceptorsToBus();
        }
    }

    public Bus getBus() {
        return bus;
    }

    public Map<ObjectName, Counter> getCounters() {
        return counters;
    }

    void registerInterceptorsToBus() {
        ResponseTimeMessageInInterceptor in = new ResponseTimeMessageInInterceptor();
        ResponseTimeMessageInvokerInterceptor invoker = new ResponseTimeMessageInvokerInterceptor();
        ResponseTimeMessageOutInterceptor out = new ResponseTimeMessageOutInterceptor();

        bus.getInInterceptors().add(in);
        bus.getInInterceptors().add(invoker);
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        bus.setExtension(this, CounterRepository.class);

        //create CounterRepositroyMoniter to writer the counter log

        //if the service is stopped or removed, the counters should remove itself
    }

    public void increaseCounter(ObjectName on, MessageHandlingTimeRecorder mhtr) {
        Counter counter = getCounter(on);
        if (null == counter) {
            counterCreationLock.lock();
            try {
                // check if the counter has been created during the locked time
                counter = getCounter(on);
                if (counter == null) {
                    counter = createCounter(on);
                    counters.put(on, counter);
                }
            } finally {
                counterCreationLock.unlock();
            }
        }
        counter.increase(mhtr);
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine("Increase counter " + on + " with " + mhtr);
        }
    }

    //find a counter
    public Counter getCounter(ObjectName on) {
        return counters.get(on);
    }

    public Counter createCounter(ObjectName on) {
        Counter counter = null;
        counter = new ResponseTimeCounter(on);
        InstrumentationManager im = bus.getExtension(InstrumentationManager.class);
        if (null != im) {
            try {
                im.register(counter);
            } catch (JMException e) {
                LOG.log(Level.WARNING, "INSTRUMENTATION_REGISTER_FAULT_MSG",
                        new Object[]{on, e});
            }
        }
        return counter;
    }



}
