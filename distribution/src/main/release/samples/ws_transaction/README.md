CXF Web Service Transaction Demo
================================
This demo shows how to use the JTA->WSAT->JTA bridge in the two differnet web services by using the apache-cxf, spring-boot and narayana.
**NOTE**:  the following dependencies are licensed under LGPL and thus may have additional restrictions beyond the Apache License.

 - [Narayana](https://github.com/jbosstm/narayana/blob/master/LICENSE)
 - [Hibernate](https://github.com/hibernate/hibernate-orm/blob/master/lgpl.txt)

Buiding and running the demo
----------------------------

From the base directory of this sample you should use maven to build it
```
mvn clean install
```

And open the console to launch the first web service FirstServiceAT
```
cd ws_first
mvn spring-boot:run
```
open another console to launch the second web service SecondServiceAT
```
cd ws_second
mvn spring-boot:run
```

Now you need to run the demo
```
cd client
mvn test -Ptest
```

JTA->WSAT bridge in the client side
================
It can wrap the local transaction and create a bridge between the JTA and WSAT transaction. 
From the client side, we use the *JaxWSTxOutboundBridgeHandler* to create a mapping of the JTA and Subordinate WSAT
also the *EnabledWSTXHandler* to propagate the WSAT transaction in the SOAP message headers. see `FirstClient` and `SecondClient`.

WSAT->JTA bridge in the server side
================
From the server side, we use the *JaxWSSubordinateHeaderContextProcessor* to import the Subordinate WSAT transaction from the outside
and the *JaxWSHeaderContextProcessor* to resume the WSAT transaction and the *OptionalJaxWSTxInboundBridgeHandler* to create the bridge
the WSAT and JTA. see `wstx_handlers.xml`
