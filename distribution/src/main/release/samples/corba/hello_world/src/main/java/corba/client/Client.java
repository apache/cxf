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

package corba.client;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;

import corba.common.HelloWorld;
import corba.common.HelloWorldHelper;

public final class Client {
    private Client() {
        //not consructed
    }
    
    static int run(ORB orb, String[] args) throws UserException {

        // Get "hello" object
        // Resolve the HelloWorldImpl by using INS's corbaname url.
        // The URL locates the NameService running on localhost
        // and listening on 1050 and resolve 'HelloWorld'
        // from that NameService
        org.omg.CORBA.Object obj = orb.string_to_object("corbaname::localhost:1050#HelloWorld");

        if (obj == null) {
            System.err.println("Client: Could not resolve target object");
            return 1;
        }

        HelloWorld hello = HelloWorldHelper.narrow(obj);

        // Test our narrowed "hello" object
        System.out.println("Invoking greetMe...");
        String result = null;
        if (args.length > 0) {
            result = hello.greetMe(args[args.length - 1]);
        } else {
            result = hello.greetMe("World");
        }
        System.out.println("greetMe.result = " + result);

        return 0;
    }

    // Standalone program initialization
    public static void main(String args[]) {
        int status = 0;
        ORB orb = null;

        try {
            orb = ORB.init(args, new Properties());
            status = run(orb, args);
        } catch (Exception ex) {
            ex.printStackTrace();
            status = 1;
        }

        if (orb != null) {
            try {
                orb.destroy();
            } catch (Exception ex) {
                ex.printStackTrace();
                status = 1;
            }
        }

        System.exit(status);
    }
}
