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
package org.apache.cxf.systest.jms.continuations;

import java.net.URL;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.cxf.systest.jms.AbstractVmJMSTest;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class HelloWorldContinuationsThrottleTest extends AbstractVmJMSTest {
    private static final String WSDL_PATH = "org/apache/cxf/systest/jms/continuations/test.wsdl";

    @BeforeClass
    public static void startServers() throws Exception {
        startBusAndJMS(HelloWorldContinuationsThrottleTest.class);
        publish("jms:queue:test.jmstransport.text?replyToQueueName=test.jmstransport.text.reply",
                new HelloWorldWithContinuationsJMS2());
    }

    @Test
    public void testThrottleContinuations() throws Exception {
        QName serviceName = new QName("http://cxf.apache.org/systest/jaxws", "HelloContinuationService");

        URL wsdlURL = getClass().getClassLoader().getResource(WSDL_PATH);
        HelloContinuationService service = new HelloContinuationService(wsdlURL, serviceName);
        final HelloContinuation helloPort = markForClose(service.getPort(HelloContinuation.class, cff));

        ThreadPoolExecutor executor = new ThreadPoolExecutor(5, 5, 0, TimeUnit.SECONDS,
                                                             new ArrayBlockingQueue<Runnable>(10));
        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch helloDoneSignal = new CountDownLatch(5);

        executor.execute(new HelloWorker(helloPort, "Fred", "", startSignal, helloDoneSignal));
        startSignal.countDown();

        Thread.sleep(10000);

        executor.execute(new HelloWorker(helloPort, "Barry", "Jameson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Harry", "", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "Rob", "Davidson", startSignal, helloDoneSignal));
        executor.execute(new HelloWorker(helloPort, "James", "ServiceMix", startSignal, helloDoneSignal));

        helloDoneSignal.await(60, TimeUnit.SECONDS);
        executor.shutdownNow();

        Assert.assertEquals("Some invocations are still running", 0, helloDoneSignal.getCount());
    }

}
