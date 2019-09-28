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
package odata.server;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

/**
 * This class is invoked by the Olingo framework when the the OData service 
 * is invoked order to display a list/collection of data (entities).
 * This is the case if an EntitySet is requested by the user.
 * Such an example URL would be:
 * http://localhost:8080/ExampleService1/ExampleService1.svc/Products
 */
public class DemoEntityCollectionProcessor implements EntityCollectionProcessor {

    private OData odata;
    private ServiceMetadata serviceMetadata;

    // CHECKSTYLE:OFF
    // our processor is initialized with the OData context object
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }
    // CHECKSTYLE:ON

    // the only method that is declared in the EntityCollectionProcessor interface
    // this method is called, when the user fires a request to an EntitySet
    // in our example, the URL would be:
    // http://localhost:8080/ExampleService1/ExampleServlet1.svc/Products
    public void readEntityCollection(ODataRequest request, ODataResponse response, 
            UriInfo uriInfo, ContentType responseFormat) throws ODataApplicationException, SerializerException {

        // 1st we have retrieve the requested EntitySet from the uriInfo object 
        // (representation of the parsed service URI)
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // In our example, the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); 
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2nd: fetch the data from backend for this requested EntitySetName 
        // it has to be delivered as EntitySet object
        EntityCollection entitySet = getData(edmEntitySet);

        // 3rd: create a serializer based on the requested format (json)
        ODataSerializer serializer = odata.createSerializer(responseFormat);
        
        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts =
                EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
        SerializerResult serializedContent = serializer.entityCollection(serviceMetadata, 
            edmEntityType, entitySet, opts);

        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedContent.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }


    /**
     * Helper method for providing some sample data
     * @param edmEntitySet for which the data is requested
     * @return data of requested entity set
     */
    private EntityCollection getData(EdmEntitySet edmEntitySet) {

        EntityCollection productsCollection = new EntityCollection();
        // check for which EdmEntitySet the data is requested
        if (DemoEdmProvider.ES_PRODUCTS_NAME.equals(edmEntitySet.getName())) {
            List<Entity> productList = productsCollection.getEntities();

            // add some sample product entities
            final Entity e1 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 1))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Notebook Basic 15"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "Notebook Basic, 1.7GHz - 15 XGA - 1024MB DDR2 SDRAM - 40GB"));
            e1.setId(createId("Products", 1));
            productList.add(e1);

            final Entity e2 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 2))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "1UMTS PDA"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "Ultrafast 3G UMTS/HSDPA Pocket PC, supports GSM network"));
            e2.setId(createId("Products", 1));
            productList.add(e2);

            final Entity e3 = new Entity()
                    .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, 3))
                    .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, "Ergo Screen"))
                    .addProperty(new Property(null, "Description", ValueType.PRIMITIVE,
                            "19 Optimum Resolution 1024 x 768 @ 85Hz, resolution 1280 x 960"));
            e3.setId(createId("Products", 1));
            productList.add(e3);
        }

        return productsCollection;
    }
    
    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}