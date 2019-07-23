/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package sample.ws.service;

import org.jboss.jbossts.XTSService;
import org.jboss.jbossts.txbridge.outbound.OutboundBridgeRecoveryManager;
import org.jboss.jbossts.xts.environment.XTSEnvironmentBean;
import org.jboss.jbossts.xts.environment.XTSPropertyManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @author <a href="mailto:zfeng@redhat.com">Zheng Feng</a>
 */
@Configuration
public class XTSConfig {
    @Bean(name = "xtsService", initMethod = "start", destroyMethod = "stop")
    public XTSService xtsService() {

        XTSEnvironmentBean xtsEnvironmentBean = XTSPropertyManager.getXTSEnvironmentBean();
        //xtsEnvironmentBean.setXtsInitialisations();

        XTSService service = new XTSService();
        return service;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @DependsOn({"xtsService"})
    public OutboundBridgeRecoveryManager outboundBridgeRecoveryManager() {
        return new OutboundBridgeRecoveryManager();
    }
}
