/**
 * Created by Apache CXF WadlToJava code generator
**/
package application;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/2")
public interface Test2 {

    @PUT
    @Consumes("application/json")
    void put(@QueryParam("snapshot") boolean snapshot, String flow);

}
