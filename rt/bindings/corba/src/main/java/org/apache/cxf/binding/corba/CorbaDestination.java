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

package org.apache.cxf.binding.corba;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.binding.corba.runtime.CorbaDSIServant;
import org.apache.cxf.binding.corba.utils.CorbaBindingHelper;
import org.apache.cxf.binding.corba.utils.CorbaUtils;
import org.apache.cxf.binding.corba.utils.OrbConfig;
import org.apache.cxf.binding.corba.wsdl.AddressType;
import org.apache.cxf.binding.corba.wsdl.PolicyType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.transport.MultiplexDestination;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;
import org.omg.CORBA.ORB;
import org.omg.CORBA.Policy;
import org.omg.PortableServer.Current;
import org.omg.PortableServer.IdAssignmentPolicyValue;
import org.omg.PortableServer.IdUniquenessPolicyValue;
import org.omg.PortableServer.LifespanPolicyValue;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManager;
import org.omg.PortableServer.RequestProcessingPolicyValue;
import org.omg.PortableServer.Servant;
import org.omg.PortableServer.ThreadPolicyValue;

public class CorbaDestination implements MultiplexDestination {
    
    private static final String IOR_SHARED_KEY = "ior:shared-key";
    private static final Logger LOG = LogUtils.getL7dLogger(CorbaDestination.class);
    
    private AddressType address;
    private EndpointReferenceType reference;
    private ORB orb;
    private BindingInfo binding;
    private EndpointInfo endpointInfo;
    private OrbConfig orbConfig;
    private MessageObserver incomingObserver;
    private CorbaTypeMap typeMap;
    private byte[] objectId;
    private POA bindingPOA;
    private String poaName;
    private String serviceId;
    private boolean isPersistent;
    private org.omg.CORBA.Object obj;

    public CorbaDestination(EndpointInfo ei, OrbConfig config) {
        this(ei, config, null);    
    }

    public CorbaDestination(EndpointInfo ei, OrbConfig config, CorbaTypeMap tm) {
        address = ei.getExtensor(AddressType.class);
        binding = ei.getBinding();
        reference = new EndpointReferenceType();
        AttributedURIType addr = new AttributedURIType();
        addr.setValue(address.getLocation());
        reference.setAddress(addr);
        endpointInfo = ei;
        orbConfig = config;
        if (tm != null) {
            typeMap = tm;
        } else {
            typeMap = TypeMapCache.get(binding.getService());
        }
        PolicyType policy = ei.getExtensor(PolicyType.class);
        if (policy != null) {
            poaName = policy.getPoaname();
            isPersistent = policy.isPersistent();
            serviceId = policy.getServiceid();            
        }
    }

    public OrbConfig getOrbConfig() {
        return orbConfig;
    }
    
    public EndpointReferenceType getAddress() {
        return reference;
    }    

    public Conduit getBackChannel(Message inMessage,
                                  Message partialResponse,
                                  EndpointReferenceType ref)
        throws IOException {
        return new CorbaServerConduit(endpointInfo, reference, obj,
                                      orb, orbConfig, typeMap);
    }

    public BindingInfo getBindingInfo() {
        return binding;
    }
    
    public EndpointInfo getEndPointInfo() {
        return endpointInfo;
    }

    public CorbaTypeMap getCorbaTypeMap() {
        return typeMap;
    }

    public void shutdown() {
        if (orb != null) {
            try {
                // Ask for the ORB to be destroyed.  If another destination is using it, we'll
                // simply decrement a use count, but not destroy the ORB so that we don't break the
                // other CorbaDestination.
                if (CorbaUtils.isIOR(getDestinationAddress())) {
                    CorbaBindingHelper.destroyORB(IOR_SHARED_KEY, orb);
                } else {
                    CorbaBindingHelper.destroyORB(getDestinationAddress(), orb);
                }
            } catch (Exception ex) {
                throw new CorbaBindingException(ex);
            }
            orb = null;
        }
    }
    
    public ORB getORB(List<String> orbArgs, 
                      String location, 
                      java.util.Properties props) {
        // See if an ORB has already been created for the given address. If so,
        // we'll simply use it
        // so that we don't try re-create another ORB on the same host and port.
        if (CorbaUtils.isIOR(location)) {
            location = IOR_SHARED_KEY;
        }
        orb = CorbaBindingHelper.getAddressSpecificORB(location, props, orbArgs);

        // Get the binding helper to remember that we need this ORB kept alive, even if another
        // destination tries to destroy it.
        CorbaBindingHelper.keepORBAlive(location);
        
        return orb;

    }
    
    protected ORB getOrb() {
        return orb;
    }
    
    protected AddressType getAddressType() {
        return address;
    }    

    public synchronized void setMessageObserver(MessageObserver observer) {
        if (observer != incomingObserver) {
            MessageObserver old = incomingObserver;
            incomingObserver = observer;
            if (observer != null) { 
                if (old == null) {
                    activate();
                }
            } else {
                if (old != null) {
                    deactivate();
                }
            }
        }
    }

    public void activate() {
        java.util.Properties props = new java.util.Properties();
        if (orbConfig.getOrbClass() != null) {
            props.put("org.omg.CORBA.ORBClass", orbConfig.getOrbClass());
        }
        if (orbConfig.getOrbSingletonClass() != null) {
            props.put("org.omg.CORBA.ORBSingletonClass", orbConfig
                .getOrbSingletonClass());
        }
        
        String location = getDestinationAddress();
        
        if (!CorbaUtils.isValidURL(location)) {
            throw new CorbaBindingException(
                    "Invalid addressing specified for CORBA port location");
        }
        
        LOG.info("Service address retrieved: " + location);
        
        
        URI addressURI = null;
        try {
            addressURI = new URI(location);
        } catch (java.net.URISyntaxException ex) {
            throw new CorbaBindingException(
                    "Unable to create ORB with address " + address);
        }
        
        
        List<String> orbArgs = new ArrayList<String>(orbConfig.getOrbArgs());

        String scheme = addressURI.getScheme();
        // A corbaloc address gives us host and port information to use when
        // setting up the
        // endpoint for the ORB. Other types of references will just create ORBs
        // on the
        // host and port used when no preference has been specified.
        if (poaName != null) {
            poaName = poaName.replace('.', '_');
        }
        if ("corbaloc".equals(scheme)) {
            if (poaName == null) {
                poaName = getEndPointInfo().getName().getLocalPart().replace('.', '_');                
            }
            setCorbaLocArgs(addressURI, orbArgs);
        } else if ("corbaname".equals(scheme)) {
            int idx = location.indexOf("#");
            if (idx != -1) {
                serviceId = location.substring(idx + 1);
            }
        }

        if (isPersistent) {
            if (poaName == null) {
                throw new CorbaBindingException(
                        "POA name missing for corba port "
                                + "with a persistent policy");
            }
        } else {
            poaName = CorbaUtils.getUniquePOAName(getEndPointInfo()
                    .getService().getName(), getEndPointInfo().getName()
                    .getLocalPart(), poaName).replace('.', '_');
        }

        orb = getORB(orbArgs, location, props);        
        

        try {
            POA rootPOA = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
            POAManager poaManager = rootPOA.the_POAManager();
            try {
                bindingPOA = rootPOA.find_POA(poaName, false);
            } catch (org.omg.PortableServer.POAPackage.AdapterNonExistent ex) {
                // do nothing
            }

            // When using object references, we can run into a situation where
            // we are implementing
            // multiple instances of the same port type such that we would end
            // up using the same
            // poaname for each when persistance is used. Handle this case by
            // not throwing an
            // exception at this point during the activation, we should see an
            // exception if we try
            // an activate two objects with the same servant ID instead.
            if (bindingPOA != null && !isPersistent && serviceId == null) {
                throw new CorbaBindingException(
                        "Corba Port activation failed because the poa "
                                + poaName + " already exists");
            } else if (bindingPOA == null) {
                bindingPOA = createPOA(poaName, rootPOA, poaManager);
            }
                        
            if (bindingPOA == null) {
                throw new CorbaBindingException("Unable to create CXF CORBA Binding POA");
            }

            CorbaDSIServant servant = new CorbaDSIServant();
            servant.init(orb, bindingPOA, this, incomingObserver, typeMap);
            if (serviceId != null) {
                objectId = serviceId.getBytes();
                try {
                    bindingPOA.activate_object_with_id(objectId, servant);
                } catch (org.omg.PortableServer.POAPackage.ObjectAlreadyActive ex) {
                    if (!isPersistent) {
                        throw new CorbaBindingException("Object "
                                                        + serviceId
                                                        + " already active for non-persistent poa");
                    }
                }
            } else {                
                objectId = bindingPOA.activate_object(servant);
            }
            bindingPOA.set_servant(servant);
            obj = bindingPOA.id_to_reference(objectId);
            orbConfig.exportObjectReference(orb, obj, location, address);
            
            populateEpr(orb.object_to_string(obj));
            LOG.info("Object Reference: " + orb.object_to_string(obj));
            // TODO: Provide other export mechanisms? 
            poaManager.activate();
        } catch (Exception ex) {
            throw new CorbaBindingException("Unable to activate CORBA servant", ex);
        }
    }

    private void populateEpr(String ior) {
        AttributedURIType addr = new AttributedURIType();
        addr.setValue(ior);
        reference.setAddress(addr);
    }

    public String getDestinationAddress() {
        // We should check the endpoint first for an address.  This allows object references
        // to use the address that is associated with their endpoint instead of the single 
        // address for a particular port type that is listed in the wsdl.  Otherwise, for all 
        // object references we want to create, we would need to add the address to the wsdl 
        // file before running the application.
        String location = null;
        if (endpointInfo != null) {
            location = endpointInfo.getAddress();
        }

        if (location == null) {
            location = address.getLocation();
        }

        return location;
    }
    
    public MessageObserver getMessageObserver() {
        return incomingObserver;
    }

    public void deactivate() {
        if (orb != null) {
            if (bindingPOA == null) {
                throw new CorbaBindingException("Corba Port deactivation failed because the poa is null");
            }

            try {
                bindingPOA.deactivate_object(objectId);
            } catch (Exception ex) {
                throw new CorbaBindingException("Unable to deactivate CORBA servant", ex);
            }
        }
    }
    
    private void setCorbaLocArgs(URI addressURI, List<String> orbArgs) {
        String schemeSpecificPart = addressURI.getSchemeSpecificPart();
        int keyIndex = schemeSpecificPart.indexOf('/');
        String corbaAddr = schemeSpecificPart.substring(0, keyIndex);
        String key = schemeSpecificPart.substring(keyIndex + 1);

        int index = corbaAddr.indexOf(':');
        String protocol = "iiop";
        if (index != 0) {
            protocol = corbaAddr.substring(0, index);
        }
        int oldIndex = index;
        index = corbaAddr.indexOf(':', oldIndex + 1);
        String host = corbaAddr.substring(oldIndex + 1, index);
        String port = corbaAddr.substring(index + 1);

        orbArgs.add("-ORB" + key + ":" + protocol + ":host");
        orbArgs.add(host);
        orbArgs.add("-ORB" + key + ":" + protocol + ":port");
        orbArgs.add(port);
        orbArgs.add("-ORBpoa:" + poaName + ":direct_persistent");
        orbArgs.add("true");
        orbArgs.add("-ORBpoa:" + poaName + ":well_known_address");
        orbArgs.add(key);
        isPersistent = true;
        serviceId = key;
    }

    protected POA createPOA(String name, POA parentPOA, POAManager poaManager) {
        List<Policy> policies = new ArrayList<Policy>();
        policies.add(parentPOA
                .create_thread_policy(ThreadPolicyValue.ORB_CTRL_MODEL));

        if (isPersistent) {
            policies.add(parentPOA
                    .create_lifespan_policy(LifespanPolicyValue.PERSISTENT));
        } else {
            policies.add(parentPOA
                    .create_lifespan_policy(LifespanPolicyValue.TRANSIENT));
        }

        if (serviceId != null) {
            policies.add(parentPOA
                         .create_id_assignment_policy(IdAssignmentPolicyValue.USER_ID));
            
        }

        policies.add(parentPOA.create_id_uniqueness_policy(IdUniquenessPolicyValue.MULTIPLE_ID));
        RequestProcessingPolicyValue value = RequestProcessingPolicyValue.USE_DEFAULT_SERVANT;
        policies.add(parentPOA.create_request_processing_policy(value));        
        
        orbConfig.addPOAPolicies(orb, name, parentPOA, poaManager, policies);
        
        Policy[] policyList = (Policy[])policies.toArray(new Policy[policies.size()]);

        try {
            return parentPOA.create_POA(name, poaManager, policyList);
        } catch (Exception ex) {
            throw new CorbaBindingException(
                    "Could not create POA during activation", ex);
        }
    }
    public EndpointReferenceType getAddressWithId(String id) {
        EndpointReferenceType ref = null;
        if (bindingPOA == null) {
            throw new CorbaBindingException(
                 "getAddressWithId failed because the poa is null");
        }
        try {
            Servant servant = bindingPOA.id_to_servant(objectId);
            org.omg.CORBA.Object objRef 
                = bindingPOA.create_reference_with_id(id.getBytes(),
                                               servant._all_interfaces(bindingPOA, objectId)[0]);
            AddressType addr = new AddressType();
            orbConfig.exportObjectReference(orb, objRef,
                                            address.getLocation(),
                                            addr);
            ref = EndpointReferenceUtils.getEndpointReference(addr.getLocation());
            EndpointInfo ei = getEndPointInfo();
            if (ei.getService() != null) {
                EndpointReferenceUtils.setServiceAndPortName(ref, ei.getService().getName(), 
                                                             ei.getName().getLocalPart());
            }
        } catch (Exception e) {
            throw new CorbaBindingException("Failed to getAddressWithId, reason:" + e.toString(), e);
        }
        return ref;
    }

    public String getId(Map contextMap) {
        String id = null;
        try {
            Current currentPoa = (Current) orb
                .resolve_initial_references("POACurrent");
            byte[] idBytes = currentPoa.get_object_id();
            id = new String(idBytes); //NOPMD
        } catch (Exception e) {
            throw new CorbaBindingException("Unable to getId, current is unavailable, reason: "
                                             + e, e);
        }
        return id;
    }

}
