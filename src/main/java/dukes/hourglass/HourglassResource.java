/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0.
 *
 * * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package dukes.hourglass;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/hourglass")
public class HourglassResource {

    private static final Logger LOG = Logger.getLogger(HourglassResource.class.getName());

    @Inject
    private HourglassService hourglassService;

    @POST
    @Path("analyze")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateMapping(@Valid HourglassInput input) {
        try {
            HourglassOutput output = hourglassService.process(input);
            return Response.ok(output).build();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Hourglass generation failed", e);
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity("{\"error\":\"The model is temporarily unavailable. Please try again.\"}")
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }

}
