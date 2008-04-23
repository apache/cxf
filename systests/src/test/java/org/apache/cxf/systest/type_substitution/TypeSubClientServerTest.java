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

package org.apache.cxf.systest.type_substitution;

import java.net.URL;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.type_substitution.Car;
import org.apache.type_substitution.CarDealer;
import org.apache.type_substitution.CarDealerService;
import org.apache.type_substitution.Porsche;
import org.junit.BeforeClass;
import org.junit.Test;

public class TypeSubClientServerTest extends AbstractBusClientServerTestBase {    

    private final QName serviceName = new QName("http://apache.org/type_substitution/",
                                                "CarDealerService");
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class));
    }

    @Test
    public void testBasicConnection() throws Exception {
        CarDealer dealer = getCardealer();

        /**
         * CarDealer.getSedans() returns List<Car>
         * Car is abstract class. The code below shows
         * that the client is expecting a Porsche which extends
         * Car.
         *
         * It shows a doc wrapper style operation.
        */

        List<Car> cars = dealer.getSedans("porsche");
        assertEquals(2, cars.size());
        Porsche car = (Porsche) cars.get(0);
        assertNotNull(car);
        if (car != null && "Porsche".equals(car.getMake()) 
            && "Boxster".equals(car.getModel()) 
            && "1998".equals(car.getYear()) 
            && "white".equals(car.getColor())) {
            // get the right car
        } else {
            fail("Get the wrong car!");
        }
        
        /**
         * CarDealer.tradeIn(Car) takes an abstract class Car and returns the same.
         * We will send a sub-class instead and expect to get the same.
         *
         */
        Porsche oldCar = new Porsche();
        oldCar.setMake("Porsche");
        oldCar.setColor("white");
        oldCar.setModel("GT2000");
        oldCar.setYear("2000");
        Porsche newCar = (Porsche)dealer.tradeIn(oldCar);
        assertNotNull(newCar);

        if (newCar != null && "Porsche".equals(newCar.getMake()) 
            && "911GT3".equals(newCar.getModel()) 
            && "2007".equals(newCar.getYear()) 
            && "black".equals(newCar.getColor())) {
            // get the right car
        } else {
            fail("Get the wrong car!");
        }
    }

    private CarDealer getCardealer() {
        URL wsdl = getClass().getResource("/wsdl/cardealer.wsdl");
        assertNotNull("WSDL is null", wsdl);

        CarDealerService service = new CarDealerService(wsdl, serviceName);
        assertNotNull("Service is null ", service);
        
        return service.getCarDealerPort();
    }
}
