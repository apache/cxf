/**
 * Created by Apache CXF WadlToJava code generator
**/
package application;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

@Path("/2")
public interface Test2 {

    @PUT
    @Consumes("application/json")
    void put(@QueryParam("snapshot") boolean snapshot, String flow);

}
