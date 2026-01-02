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
package org.apache.cxf.systest.ws.mtom;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;

import jakarta.activation.DataHandler;
import jakarta.jws.WebService;
import org.apache.cxf.feature.Features;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItMtomPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItMtomPortType")
@Features(features = "org.apache.cxf.ext.logging.LoggingFeature")
public class DoubleIt4Impl implements DoubleItMtomPortType {

    @Override
    public int doubleIt4(int numberToDouble, DataHandler imageData) throws DoubleItFault {
        if (numberToDouble == 0) {
            throw new DoubleItFault("0 can't be doubled!");
        }

        try {
            BufferedImage image = ImageIO.read(imageData.getInputStream());
            if (image == null) {
                throw new DoubleItFault("Error processing image data");
            }
        } catch (IOException e) {
            throw new DoubleItFault("Error processing image data: " + e.getMessage());
        }

        return numberToDouble * 2;
    }

}
