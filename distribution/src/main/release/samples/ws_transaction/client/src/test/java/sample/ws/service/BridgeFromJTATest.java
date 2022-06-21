/**
 * licensed to the apache software foundation (AS) under one
 * or more contributor license agreements. see the notice file
 * distributed with this work for additional information
 * regarding copyright ownership. the asf licenses this file
 * to you under the apache license, version 2.0 (the
 * "license"); you may not use this file except in compliance
 * with the license. you may obtain a copy of the license at
 *
 * http://www.apache.org/licenses/license-2.0
 *
 * unless required by applicable law or agreed to in writing,
 * software distributed under the license is distributed on an
 * "as is" basis, without warranties or conditions of any
 * kind, either express or implied. see the license for the
 * specific language governing permissions and limitations
 * under the license.
 */

package sample.ws.service;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringRunner;

import jakarta.transaction.UserTransaction;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SampleWsApplication.class, webEnvironment = WebEnvironment.DEFINED_PORT)
public class  BridgeFromJTATest {

    @Autowired
    private UserTransaction ut;

    private FirstServiceAT firstClient;
    private SecondServiceAT secondClient;


    @Before
    public void setUp() throws Exception {
        firstClient = FirstClient.newInstance();
        secondClient = SecondClient.newInstance();
    }

    @After
    public void teardownTest() throws Exception {
        rollbackIfActive(ut);
        try {
            ut.begin();
            firstClient.resetCounter();
            secondClient.resetCounter();
            ut.commit();
        } finally {
            rollbackIfActive(ut);
        }
    }


    @Test
    public void testCommit() throws Exception {
        ut.begin();
        firstClient.incrementCounter(1);
        secondClient.incrementCounter(1);
        ut.commit();

        ut.begin();
        int counter1 = firstClient.getCounter();
        int counter2 = secondClient.getCounter();
        ut.commit();

        Assert.assertEquals(1, counter1);
        Assert.assertEquals(1, counter2);
    }

    @Test
    public void testClientDrivenRollback() throws Exception {
        ut.begin();
        firstClient.incrementCounter(1);
        secondClient.incrementCounter(1);
        ut.rollback();

        ut.begin();
        int counter1 = firstClient.getCounter();
        int counter2 = secondClient.getCounter();
        ut.commit();

        Assert.assertEquals(0, counter1);
        Assert.assertEquals(0, counter2);
    }


    /**
     * Utility method for rolling back a transaction if it is currently active.
     *
     * @param ut The User Business Activity to cancel.
     */
    private void rollbackIfActive(UserTransaction ut) {
        try {
            ut.rollback();
        } catch (Throwable th2) {
            // do nothing, not active
        }
    }
}
