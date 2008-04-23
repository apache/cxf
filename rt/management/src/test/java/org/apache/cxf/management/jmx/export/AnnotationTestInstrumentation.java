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

package org.apache.cxf.management.jmx.export;

import javax.management.JMException;
import javax.management.ObjectName;

import org.apache.cxf.management.ManagedComponent;
import org.apache.cxf.management.annotation.ManagedAttribute;
import org.apache.cxf.management.annotation.ManagedNotification;
import org.apache.cxf.management.annotation.ManagedNotifications;
import org.apache.cxf.management.annotation.ManagedOperation;
import org.apache.cxf.management.annotation.ManagedOperationParameter;
import org.apache.cxf.management.annotation.ManagedOperationParameters;
import org.apache.cxf.management.annotation.ManagedResource;

@ManagedResource(componentName = "AnnotationTest", description = "My Managed Bean",
                 persistPolicy = "OnUpdate", currencyTimeLimit = 15 , 
                 log = false ,
                 logFile = "jmx.log", persistPeriod = 200,
                 persistLocation = "/local/work", persistName = "bar.jmx")
@ManagedNotifications({@ManagedNotification(name = "My Notification",
                                            notificationTypes = {"type.foo", "type.bar" }) })
public class AnnotationTestInstrumentation implements ManagedComponent {

    private String name; 

    private String nickName;

    private int age;

    private boolean isSuperman;


    @ManagedAttribute(description = "The Age Attribute", currencyTimeLimit = 15)
    public int getAge() {
        return age;
    }
        
    public void setAge(int a) {
        this.age = a;
    }

    @ManagedOperation(currencyTimeLimit = 30)
    public long myOperation() {
        return 1L;
    }

    @ManagedAttribute(description = "The Name Attribute",
                      currencyTimeLimit = 20,
                      defaultValue = "bar",
                      persistPolicy = "OnUpdate")
    public void setName(String n) {
        this.name = n;
    }

    @ManagedAttribute(defaultValue = "bar", persistPeriod = 300)
    public String getName() {
        return name;
    }

    @ManagedAttribute(defaultValue = "barasd", description = "The Nick Name Attribute")
    public String getNickName() {
        return this.nickName;
    }

    public void setNickName(String n) {
        this.nickName = n;
    }

    @ManagedAttribute(description = "The Is Superman Attribute")
    public void setSuperman(boolean superman) {
        this.isSuperman = superman;
    }

    public boolean isSuperman() {
        return isSuperman;
    }

    @ManagedOperation(description = "Add Two Numbers Together")
    @ManagedOperationParameters({@ManagedOperationParameter(
                                 name = "x", description = "Left operand"),
                                 @ManagedOperationParameter(
                                 name = "y", description = "Right operand") })
    public int add(int x, int y) {
        return x + y;
    }

    public ObjectName getObjectName() throws JMException {
        return new ObjectName("org.apache.cxf:type=AnnotationTestInstrumentation");
    }
    
}
