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
package org.apache.cxf.systest.jaxrs.reactive;

public class HelloWorldBean {
    private String greeting;
    private String audience = "World";
    public HelloWorldBean() {
        this("Hello");
    }
    public HelloWorldBean(String greeting) {
        this(greeting, "World");
    }
    
    public HelloWorldBean(String greeting, String audience) {
        this.greeting = greeting;
        this.audience = audience;
    }

    public String getGreeting() {
        return greeting;
    }
    public void setGreeting(String greeting) {
        this.greeting = greeting;
    }
    public String getAudience() {
        return audience;
    }
    public void setAudience(String audience) {
        this.audience = audience;
    }
    @Override
    public int hashCode() {
        int result = 31 + ((audience == null) ? 0 : audience.hashCode());
        result = 31 * result + ((greeting == null) ? 0 : greeting.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (getClass() != obj.getClass()) {
            return false;
        }
        
        final HelloWorldBean other = (HelloWorldBean) obj;
        if (audience == null && other.audience != null) {
            return false;
        } else if (!audience.equals(other.audience)) {
            return false;
        }
        
        if (greeting == null && other.greeting != null) {
            return false;
        } else if (!greeting.equals(other.greeting)) {
            return false;
        }
        
        return true;
    }
}
