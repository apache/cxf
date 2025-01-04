/**
 * Created by Apache CXF WadlToJava code generator
**/
package application;

import java.util.concurrent.CompletableFuture;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;

@Path("/1")
public interface Test1 {

    @PUT
    @Consumes("multipart/mixed")
    @Produces("text/plain")
    CompletableFuture<String> put(@QueryParam("standalone") Boolean standalone, @Multipart("action") String action, @Multipart(value = "sources", required = false) String sources);

}
