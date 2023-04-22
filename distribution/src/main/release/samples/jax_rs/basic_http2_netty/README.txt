JAX-RS Basic Demo With HTTPS communications
===========================================

This demo takes the JAX-RS basic demo a step further 
by doing the communication using HTTP/2 over TLS and
HTTP/2 over cleartext using Netty container.

Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn -Ph2-server  (HTTP/2 over TLS)
  mvn -Ph2c-server  (HTTP/2 over cleartext)
    
To remove the target dir, run "mvn clean".

Certificates
------------
See the src/main/config folder for the sample keys used (don't use
these keys in production!) as well as scripts used for their creation.

HTTP/2 over cleartext
------------

- Upgrade from HTTP/1.1 to HTTP/2 (cleartext)

    $ curl http://localhost:9001/customerservice/customers/123 --http2 -i
  
    HTTP/1.1 101 Switching Protocols
    connection: upgrade
    upgrade: h2c

    HTTP/2 200
    content-type: application/xml
    date: Thu, 14 Oct 2021 01:48:35 GMT
    content-length: 105

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>John</name></Customer>

- Use HTTP/2 prior knowledge (cleartext)

    $ curl http://localhost:9001/customerservice/customers/123 --http2-prior-knowledge -i
  
    HTTP/2 200
    content-type: application/xml
    date: Thu, 14 Oct 2021 01:49:08 GMT
    content-length: 105

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>John</name></Customer>
  
- Force HTTP/1.1 usage (cleartext)

    $ curl http://localhost:9001/customerservice/customers/123 --http1.1 -i
  
    HTTP/1.1 200 OK
    Content-Type: application/xml
    Date: Thu, 14 Oct 2021 01:49:35 GMT
    content-length: 105
    connection: keep-alive
  
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>John</name></Customer>
    
HTTP/2 over TLS
------------

- Use HTTP/2

    $ curl https://localhost:9000/customerservice/customers/123 --http2 -ik
  
    HTTP/2 200
    content-type: application/xml
    date: Thu, 14 Oct 2021 01:46:59 GMT
    content-length: 105

    <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>John</name></Customer>
  
- Force HTTP/1.1 usage

    $ curl https://localhost:9000/customerservice/customers/123 --http1.1 -ik
  
    HTTP/1.1 200 OK
    Content-Type: application/xml
    Date: Thu, 14 Oct 2021 01:47:51 GMT
    content-length: 105
    connection: keep-alive
  
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?><Customer><id>123</id><name>John</name></Customer>

