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

package org.apache.cxf.service.invoker;

import org.apache.cxf.message.Exchange;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Factory that will query the Spring ApplicationContext for the 
 * appropriate bean for each request.
 * 
 * This can be expensive.  If the bean is "prototype" or similar such that a 
 * new instance is created each time, this could slow things down.  In that 
 * case, it's recommended to use this in conjunction with the PooledFactory
 * to pool the beans or the SessionFactory or similar.
 */
public class SpringBeanFactory implements Factory, ApplicationContextAware {
    ApplicationContext ctx;
    String beanName;
    
    public SpringBeanFactory(String name) {
        beanName = name;
    }
    
    /** {@inheritDoc}*/
    public Object create(Exchange e) throws Throwable {
        return ctx.getBean(beanName);
    }

    /** {@inheritDoc}*/
    public void release(Exchange e, Object o) {
        //nothing
    }

    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        ctx = arg0;
    }

}
