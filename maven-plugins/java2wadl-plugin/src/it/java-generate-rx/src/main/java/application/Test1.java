/**
 * Created by Apache CXF WadlToJava code generator
**/
package application;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import java.util.concurrent.CompletableFuture;
import org.apache.cxf.jaxrs.ext.multipart.Multipart;

@Path("/1")
public interface Test1 {

    @PUT
    @Consumes("multipart/mixed")
    @Produces("text/plain")
    CompletableFuture<String> put(@QueryParam("standalone") Boolean standalone, @Multipart("action") String action, @Multipart(value = "sources", required = false) String sources);

}
