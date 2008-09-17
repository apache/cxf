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

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

public class GreeterBean implements SessionBean {

    //------------- Business Methods
    public String sayHi() {
        System.out.println("sayHi invoked");
        return "Hi from an EJB"; 
    }

    public String greetMe(String user) {
        System.out.println("greetMe invoked user:" + user);
        return "Hi " + user + " from an EJB"; 
    }

    //------------- EJB Methods
    public void ejbActivate() {
    }
    
    public void ejbRemove() {
    }
    
    public void ejbPassivate() {
    }
    
    public void ejbCreate() throws CreateException {
    }
    
    public void setSessionContext(SessionContext con) {
    }

}
