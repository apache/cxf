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
package org.apache.cxf.jca.inbound;

import javax.resource.ResourceException;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ResourceAdapter;

/**
 * MDBActivationSpec is an {@link javax.resource.spi.ActivationSpec} that
 * activates a CXF service endpoint facade.  All resource locations are 
 * relative to the message driven bean jar.
 *
 */
public class MDBActivationSpec implements ActivationSpec {

    private ResourceAdapter resouceAdapter;
    private String wsdlLocation;
    private String schemaLocations;
    private String serviceInterfaceClass;
    private String busConfigLocation;
    private String address;
    private String endpointName;
    private String serviceName;
    private String displayName;
    
    /**
     * Gets the transport address used by 
     * {@link org.apache.cxf.frontend.ServerFactoryBean}.
     * 
     * @return the address
     */
    public String getAddress() {
        return address;
    }

    /**      
     * @return the busConfigLocation
     */
    public String getBusConfigLocation() {
        return busConfigLocation;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     *  
     * @return the endpointName
     */
    public String getEndpointName() {
        return endpointName;
    }

    public ResourceAdapter getResourceAdapter() {
        return resouceAdapter;
    }

    /**
     * Comma separated schema locations
     * 
     * @return the schemaLocations
     */
    public String getSchemaLocations() {
        return schemaLocations;
    }


    /**
     * Gets the service endpoint interface classname.  
     * 
     * The class should be available in the Message Driven Bean jar.
     * 
     * @return the serviceInterfaceClass
     */
    public String getServiceInterfaceClass() {
        return serviceInterfaceClass;
    }

    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }


    /**
     * 
     * @return the wsdlLocation
     */
    public String getWsdlLocation() {
        return wsdlLocation;
    }


    /**
     * Sets the transport address used by 
     * {@link org.apache.cxf.frontend.ServerFactoryBean}.
     * 
     * @param address the address to set
     */
    public void setAddress(String address) {
        this.address = address;
    }


    /**
     *      
     * @param busConfigLocation the busConfigLocation to set
     */
    public void setBusConfigLocation(String busConfigLocation) {
        this.busConfigLocation = busConfigLocation;
    }


    /**
     * A unique name that is readable to human and it is to
     * identify an inbound endpoint within a application server.
     * 
     * @param displayName the displayName to set
     */
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /**
     * @param endpointName the endpointName to set
     */
    public void setEndpointName(String endpointName) {
        this.endpointName = endpointName;
    }

    public void setResourceAdapter(ResourceAdapter ra) throws ResourceException {
        resouceAdapter = ra;
    }

    /**
     * Comma separated schema locations
     * 
     * @param schemaLocations the schemaLocations to set
     */
    public void setSchemaLocations(String schemaLocations) {
        this.schemaLocations = schemaLocations;
    }

    /**
     * @param serviceInterfaceClass the serviceInterfaceClass to set
     */
    public void setServiceInterfaceClass(String serviceInterfaceClass) {
        this.serviceInterfaceClass = serviceInterfaceClass;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }


    /**
     * 
     * @param wsdlLocation the wsdlLocation to set
     */
    public void setWsdlLocation(String wsdlLocation) {
        this.wsdlLocation = wsdlLocation;
    }

    /**
     * TODO implement validation
     */
    public void validate() throws InvalidPropertyException {
    }

}
