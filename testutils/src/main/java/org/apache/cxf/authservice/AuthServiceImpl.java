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

package org.apache.cxf.authservice;

import java.util.ArrayList;
import java.util.List;

import javax.jws.WebService;

@WebService(targetNamespace = "http://cxf.apache.org/AuthService", name = "AuthService",
            endpointInterface = "org.apache.cxf.authservice.AuthService")
public class AuthServiceImpl implements AuthService {

    public boolean authenticate(String sid, String uid, String pwd) {
        if (uid == null) {
            //test to make sure a "middle" param can be null
            return pwd != null;
        }
        return sid.equals(uid);
    }

    public boolean authenticate(Authenticate au) {
        return au.getUid().equals(au.getSid());
    }
    
    public String getAuthentication(String sid) {
        return "get " + sid;
    }

    public List<String> getRoles(String sid) {
        List<String> list = new ArrayList<String>();
        list.add(sid);
        list.add(sid + "-1");
        list.add(sid + "-2");
        return list;
    }
    public String[] getRolesAsArray(String sid) {
        if ("null".equals(sid)) {
            return null;
        }
        if ("0".equals(sid)) {
            return new String[0];
        }
        return new String[] {sid, sid + "-1"};
    }

}
