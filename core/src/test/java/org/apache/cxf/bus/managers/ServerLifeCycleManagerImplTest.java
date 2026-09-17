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

package org.apache.cxf.bus.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerLifeCycleListener;
import org.apache.cxf.endpoint.ServerLifeCycleManager;
import org.apache.cxf.transport.Destination;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ServerLifeCycleManagerImplTest {
    private static final class NoopServerLifeCycleListener implements ServerLifeCycleListener {
        @Override
        public void stopServer(Server server) {
        }
        
        @Override
        public void startServer(Server server) {
        }
    }
    

    @Test
    public void testServerLifeCycleManager() throws InterruptedException, ExecutionException {
        final Random random = new Random();
        final ServerLifeCycleManager lifeCycleManager = new ServerLifeCycleManagerImpl();
        final Server server = new Server() {
            
            @Override
            public void stop() {
            }
            
            @Override
            public void start() {
            }
            
            @Override
            public boolean isStarted() {
                return false;
            }
            
            @Override
            public Endpoint getEndpoint() {
                return null;
            }
            
            @Override
            public Destination getDestination() {
                return null;
            }
            
            @Override
            public void destroy() {
            }
        };

        final NoopServerLifeCycleListener[] listeners = new NoopServerLifeCycleListener[10];
        for (int i = 0; i < 10; ++i) {
            listeners[i] = new NoopServerLifeCycleListener();
            lifeCycleManager.registerListener(listeners[i]);
        }

        final Phaser phaser = new Phaser(10);
        phaser.register();

        final Collection<Future<?>> futures = new ArrayList<>(); 
        final ExecutorService executor = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10; ++i) {
            final int index = i;
            if (random.nextBoolean()) {
                futures.add(executor.submit(() -> {
                    phaser.arriveAndAwaitAdvance();
                    lifeCycleManager.stopServer(server);
                }));
            } else {
                futures.add(executor.submit(() -> {
                    phaser.arriveAndAwaitAdvance();
                    lifeCycleManager.unRegisterListener(listeners[index]);
                }));
            }
        }

        phaser.arriveAndDeregister();
        executor.shutdown();
        assertThat(executor.awaitTermination(20, TimeUnit.SECONDS), equalTo(true));

        for (final Future<?> future: futures) {
            assertThat(future.isDone(), equalTo(true));
            assertThat(future.get(), equalTo((Void) null));
        }
    }
}
