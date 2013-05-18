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

package demo.wseventing;

import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.cxf.ws.eventing.backend.manager.SubscriptionManagerInterfaceForNotificators;
import org.apache.cxf.ws.eventing.backend.notification.EventSinkInterfaceNotificatorService;
import org.apache.cxf.ws.eventing.backend.notification.NotificatorService;

import demo.wseventing.eventapi.CatastrophicEventSink;

@WebListener
public class NotificatorServiceHolder implements ServletContextListener {

    private static NotificatorService instance;

    private Logger logger = Logger.getLogger(NotificatorServiceHolder.class.getName());

    public static NotificatorService getInstance() {
        return instance;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing and starting NotificatorService");
        instance = new EventSinkInterfaceNotificatorService() {
            @Override
            protected SubscriptionManagerInterfaceForNotificators obtainManager() {
                return SingletonSubscriptionManagerContainer.getInstance();
            }

            @Override
            protected Class getEventSinkInterface() {
                return CatastrophicEventSink.class;
            }
        };
        instance.start();
        ApplicationSingleton.getInstance().createEventSink("/default");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Stopping NotificatorService");
        instance.stop();     // very important!
        SingletonSubscriptionManagerContainer.destroy();
    }
}
