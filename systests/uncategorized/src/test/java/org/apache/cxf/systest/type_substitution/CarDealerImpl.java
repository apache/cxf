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

import java.util.ArrayList;
import java.util.List;

import jakarta.jws.WebService;
import jakarta.jws.soap.SOAPBinding;
import jakarta.xml.ws.WebServiceException;
import org.apache.type_substitution.Car;
import org.apache.type_substitution.CarDealer;
import org.apache.type_substitution.Porsche;

@WebService(name = "CarDealer")

public class CarDealerImpl implements CarDealer {
    public List<Car> getSedans(String carType) {
        if ("Porsche".equalsIgnoreCase(carType)) {
            List<Car> cars = new ArrayList<>();
            cars.add(newPorsche("Boxster", "1998", "white"));
            cars.add(newPorsche("BoxsterS", "1999", "red"));
            return cars;
        }
        throw new WebServiceException("Not a dealer of: " + carType);
    }

    @SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
    public Car tradeIn(Car oldCar) {
        if (!(oldCar instanceof Porsche)) {
            throw new WebServiceException("Expected Porsche, received, " + oldCar.getClass().getName());
        }

        Porsche porsche = (Porsche)oldCar;
        if ("Porsche".equals(porsche.getMake()) && "GT2000".equals(porsche.getModel())
            && "2000".equals(porsche.getYear()) && "white".equals(porsche.getColor())) {
            return newPorsche("911GT3", "2007", "black");
        }
        throw new WebServiceException("Invalid Porsche Car");
    }

    private Porsche newPorsche(String model, String year, String color) {
        Porsche porsche = new Porsche();
        porsche.setMake("Porsche");
        porsche.setModel(model);
        porsche.setYear(year);
        porsche.setColor(color);
        return porsche;
    }
}
