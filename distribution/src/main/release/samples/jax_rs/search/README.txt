JAX-RS Search Demo 
=================

The demo shows a basic usage of search extension with REST based Web Services using 
JAX-RS 2.0 (JSR-339). The REST server provides the following services: 

A RESTful catalog service is provided on URL http://localhost:9000/catalog 
A web browser demo is available at: http://localhost:9000/

A HTTP GET request to URL http://localhost:9000/catalog
returns all the documents currently stored and managed (in JSON format):

[
    "jsr339-jaxrs-2.0-final-spec.pdf",
    "JavaWebSocketAPI_1.0_Final.pdf"
]

A HTTP POST request to URL http://localhost:9000/catalog
uploads document and stores it.

A HTTP GET request to URL http://localhost:9000/catalog/search?$filter=<query>
searches the relevant documents which match the query and returns them (in JSON format):

[
    {
        "source":"JavaWebSocketAPI_1.0_Final.pdf",
        "score":0.07321092486381531,
        "url":"http://localhost:9000/catalog/JavaWebSocketAPI_1.0_Final.pdf"
    },
    {
        "source":"jsr339-jaxrs-2.0-final-spec.pdf",
        "score":0.03448590263724327,
        "url":"http://localhost:9000/catalog/jsr339-jaxrs-2.0-final-spec.pdf"
    }
]

A HTTP GET request to URL http://localhost:9000/catalog/<document> returns the
original document content (in  binary form).

A HTTP DELETE request to URL http://localhost:9000/catalog removes all documents
from the catalog.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run mvn clean".



