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

package corba.server;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.UserException;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;

import corba.common.BankHelper;

public final class Server {
    private Server() {
        //not created
    }


    static int run(ORB orb, String[] args) throws UserException {
        //
        // Resolve Root POA
        //
        POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));

        //
        // Get a reference to the POA manager
        //
        POAManager manager = poa.the_POAManager();

        //
        // Create implementation object
        //
        BankImpl bankImpl = new BankImpl(poa);

        byte[] oid = "Bank".getBytes();
        poa.activate_object_with_id(oid, bankImpl);

        org.omg.CORBA.Object ref = poa.create_reference_with_id(oid, BankHelper.id());

        // Register in NameService
        org.omg.CORBA.Object nsObj = orb.resolve_initial_references("NameService");
        NamingContextExt rootContext = NamingContextExtHelper.narrow(nsObj);
        NameComponent[] nc = rootContext.to_name("Bank");
        rootContext.rebind(nc, ref);

        //
        // Run implementation
        //
        manager.activate();
        System.out.println("Server ready...");
        orb.run();

        return 0;
    }

    public static void main(String[] args) {
        Properties props = System.getProperties();
        props.put("org.omg.CORBA.ORBInitialHost", "localhost");
        props.put("org.omg.CORBA.ORBInitialPort", "1050");

        int status = 0;
        ORB orb = null;
        try         {
            orb = ORB.init(args, props);
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
