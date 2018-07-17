package vn.ssdc.vnpt.umpexception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by THANHLX on 4/10/2018.
 */
public class UmpNbiException extends WebApplicationException {

    public UmpNbiException(String message) {
        super(Response.status(601)
                .entity(message)
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}
