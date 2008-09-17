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
package demo.ejb;

import javax.ejb.MessageDrivenBean;
import javax.ejb.MessageDrivenContext;

import org.apache.hello_world_soap_http.Greeter;

public class GreeterBean implements MessageDrivenBean, Greeter {

    public String sayHi() {
        System.out.println("sayHi called ");
        return "Hi there!";
    }

    public String greetMe(String user) {
        System.out.println("greetMe called user = " + user);
        return "Hello " + user;
    }

    //---------------- EJB Methods
    public void ejbCreate() {
    }

    public void ejbRemove() {
    }

    public void setMessageDrivenContext(MessageDrivenContext mdc) {
    }

}
