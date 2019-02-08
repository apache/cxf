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

package org.apache.cxf.transport.http;

import java.net.Authenticator;
import java.net.InetAddress;
import java.net.PasswordAuthentication;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 */
public class CXFAuthenticatorCleanupTest {

    /**
     *
     */
    public CXFAuthenticatorCleanupTest() {
    }


    @Test
    public void runCleanupTestStrongRef() throws Exception {
        final List<Integer> traceLengths = new ArrayList<>();

        //create a chain of CXFAuthenticators, strongly referenced to prevent cleanups
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                traceLengths.add(Thread.currentThread().getStackTrace().length);
                return super.getPasswordAuthentication();
            }
        });


        InetAddress add = InetAddress.getLocalHost();
        Authenticator.requestPasswordAuthentication("localhost", add,
                                                    8080, "http", "hello", "http");

        List<CXFAuthenticator> list = new ArrayList<>();
        for (int x = 0; x < 20; x++) {
            CXFAuthenticator.addAuthenticator();
            list.add(CXFAuthenticator.instance);
            CXFAuthenticator.instance = null;
        }

        Authenticator.requestPasswordAuthentication("localhost", add,
                                                    8080, "http", "hello", "http");
        for (int x = 9; x > 0; x -= 2) {
            list.remove(x);
        }
        for (int x = 0; x < 10; x++) {
            System.gc();
            Authenticator.requestPasswordAuthentication("localhost", add,
                                                        8080, "http", "hello", "http");
        }
        list.clear();
        for (int x = 0; x < 10; x++) {
            System.gc();
            Authenticator.requestPasswordAuthentication("localhost", add,
                                                        8080, "http", "hello", "http");
        }
        Assert.assertEquals(22, traceLengths.size());
        //first trace would be just the raw authenticator above
        int raw = traceLengths.get(0);
        //second would be the trace with ALL the auths
        int all = traceLengths.get(1);
        //after remove of 5 and some gc's
        int some = traceLengths.get(11);
        //after clear and gc's
        int none = traceLengths.get(traceLengths.size() - 1);

        //System.out.println(traceLengths);
        Assert.assertTrue(all > (raw + 20 * 3)); //all should be A LOT above raw
        Assert.assertTrue(all > raw);
        Assert.assertTrue(all > some);
        Assert.assertTrue(some > none);
        Assert.assertEquals(raw, none);
    }
    @Test
    public void runCleanupTestWeakRef() throws Exception {
        InetAddress add = InetAddress.getLocalHost();
        final List<Integer> traceLengths = new ArrayList<>();
        //create a chain of CXFAuthenticators, strongly referenced to prevent cleanups
        Authenticator.setDefault(new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                traceLengths.add(Thread.currentThread().getStackTrace().length);
                return super.getPasswordAuthentication();
            }
        });
        Authenticator.requestPasswordAuthentication("localhost", add,
                                                    8080, "http", "hello", "http");


        for (int x = 0; x < 20; x++) {
            CXFAuthenticator.addAuthenticator();
            CXFAuthenticator.instance = null;
            System.gc();
        }
        CXFAuthenticator.addAuthenticator();
        System.gc();

        Authenticator.requestPasswordAuthentication("localhost", add,
                                                    8080, "http", "hello", "http");
        CXFAuthenticator.instance = null;
        for (int x = 0; x < 10; x++) {
            System.gc();
            Authenticator.requestPasswordAuthentication("localhost", add,
                                                        8080, "http", "hello", "http");
        }
        Assert.assertEquals(12, traceLengths.size());

        //first trace would be just the raw authenticator above
        int raw = traceLengths.get(0);
        //second trace should still have an Authenticator added
        int one = traceLengths.get(1);
        //after clear and gc's
        int none = traceLengths.get(traceLengths.size() - 1);

        /*stacktrace for one should be different with raw
         * but the stracktrace length in java 8 and java 9-plus
         * isn't identical
         * so previous assertion one < (raw + (20 * 2)
         * isn't applicable for java 9-plus
         */
        Assert.assertTrue(one != raw); 
        
        
        Assert.assertTrue(one > raw);
        Assert.assertTrue(one > none);
        Assert.assertEquals(raw, none);
    }
}
