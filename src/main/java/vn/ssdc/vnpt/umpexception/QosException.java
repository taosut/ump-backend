/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vn.ssdc.vnpt.umpexception;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author kiendt
 */
public class QosException extends WebApplicationException {

    public QosException(String message) {
        super(Response.status(701)
                .entity(message)
                .type(MediaType.TEXT_PLAIN)
                .build());
    }
}
