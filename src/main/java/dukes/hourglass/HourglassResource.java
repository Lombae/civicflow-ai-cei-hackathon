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
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/hourglass")

public class HourglassResource {

    @Inject
    private HourglassService hourglassService;

    @POST
    @Path("analyze")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response generateMapping(HourglassInput input) {
        // Call service and return 200 OK with the HourglassOutput
        return Response.ok().build();
    }

}
