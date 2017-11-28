JAX-RS OData Demo 
=================

This demo shows a CXF JAX-RS service interposing over Apache Olingo based OData 4 service.

Credits. 

This demo was built by copying some of the files and the code described at

https://olingo.apache.org/doc/odata4/tutorials/read/tutorial_read.html 

After bulding the demo and starting it with "mvn -Pserver", do

1. curl -H "application/json" http://localhost:9000/DemoService/DemoService.svc/Products

It will return the OData model describing the products

2. curl -H "application/json" http://localhost:9000/DemoService/DemoService.svc/$metadata

It will return the OData model describing the products metadata

3. curl -H "application/json" http://localhost:9000/DemoService/DemoService.svc

It will return the OData service metadata 

