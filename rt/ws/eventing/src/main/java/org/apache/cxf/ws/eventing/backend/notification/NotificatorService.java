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

package org.apache.cxf.ws.eventing.backend.notification;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.logging.Logger;


import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.ws.eventing.backend.database.SubscriptionTicket;
import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForNotificators;


/**
 * The service which takes care of notifying subscribers about events. Has access to the subscription database.
 * Receives events from compliant Emitters, eg. EmitterServlet / EmitterMBean,..
 * Don't forget to use the 'stop' method, especially if running inside a servlet container!!
 * Suggested approach for a web container is to instantiate this class in a ServletContextListener
 * and then have it stopped using the same listener. If you don't call 'stop' upon undeployment,
 * the underlying ExecutorService will not be shut down, leaking resources.
 */
public abstract class NotificatorService {

    public static final int CORE_POOL_SIZE = 15;
    protected static final Logger LOG = LogUtils.getLogger(NotificatorService.class);
    protected ExecutorService service;

    public NotificatorService() {
    }

    protected abstract SubscriptionManagerInterfaceForNotificators obtainManager();

    public void dispatchEvent(Object event) {
        if (service == null) {
            throw new IllegalStateException("NotificatorService is not started. "
                    + "Please call the start() method before passing any events to it.");
        }
        for (SubscriptionTicket ticket : obtainManager().getTickets()) {
            if (!ticket.isExpired()) {
                submitNotificationTask(ticket, event);
            } else {
                LOG.info("Ticket expired at " + ticket.getExpires().toXMLFormat());
            }
        }
    }

    protected abstract void submitNotificationTask(SubscriptionTicket ticket, Object event);

    public void subscriptionEnd(SubscriptionTicket ticket, String reason, SubscriptionEndStatus status) {
        LOG.info("NotificatorService will notify about subscription end for ticket=" + ticket.getUuid()
            + "; reason=" + reason);
        service.submit(new SubscriptionEndNotificationTask(ticket, reason, status));
    }


    /**
     * Starts this NotificatorService. You MUST run this method on every instance
     * before starting to pass any events to it. Run it only once.
     */
    public void start() {
        obtainManager().registerNotificator(this);
        service = new ScheduledThreadPoolExecutor(CORE_POOL_SIZE);
    }

    /**
     * Shuts down the NotificatorService. This method is a MUST if you are running it inside a servlet container,
     * because it will shutdown the underlying ExecutorService.
     */
    public void stop() {
        service.shutdown();
    }

}
