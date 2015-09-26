package nl.pwiddershoven.scriptor.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.pwiddershoven.scriptor.repository.EndpointConfigurationRepository;
import nl.pwiddershoven.scriptor.service.EndpointConfiguration;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Consumes(MediaType.APPLICATION_JSON)
@Path("/configs")
public class EndpointConfigurationController {
    private final Logger logger = Logger.getLogger(EndpointConfigurationController.class);

    @Autowired
    private EndpointConfigurationRepository endpointConfigurationRepository;

    @POST
    public Response createConfiguration(@Valid EndpointConfigurationDTO endpointConfigurationDTO) {
        String id = endpointConfigurationRepository.save(endpointConfigurationDTO.toEndpointConfiguration());
        return Response.created(getLocation(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void updateConfiguration(@PathParam("id") String id, EndpointConfigurationDTO endpointConfigurationDTO) {
        endpointConfigurationRepository.update(id, endpointConfigurationDTO.toEndpointConfiguration());
    }

    @GET
    public List<EndpointConfigurationDTO> getEndpointConfigurations(
            @PathParam("id") String id,
            @DefaultValue("1") @QueryParam("page") int page,
            @DefaultValue("10") @QueryParam("perPage") int perPage) {
        int offset = (page - 1) * perPage;
        List<EndpointConfiguration> endpointConfigurations = endpointConfigurationRepository.findAll(offset, perPage);

        return endpointConfigurations.stream()
                .map(this::buildDTO).collect(Collectors.toList());
    }

    @GET
    @Path("/{id}")
    public EndpointConfigurationDTO getEndpointConfiguration(@PathParam("id") String id) {
        EndpointConfiguration endpointConfiguration = findConfigurationOr404(id);
        return buildDTO(endpointConfiguration);
    }

    @DELETE
    @Path("/{id}")
    public void removeEndpointConfiguration(@PathParam("id") String id) {
        endpointConfigurationRepository.remove(id);
    }

    private URI getLocation(String id) {
        try {
            return new URI("/config/" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private EndpointConfigurationDTO buildDTO(EndpointConfiguration endpointConfiguration) {
        EndpointConfigurationDTO endpointConfigurationDTO = new EndpointConfigurationDTO();

        endpointConfigurationDTO.id = endpointConfiguration.id;
        endpointConfigurationDTO.script = endpointConfiguration.processingScript;
        endpointConfigurationDTO.contentType = endpointConfiguration.contentType;
        endpointConfigurationDTO.accessToken = endpointConfiguration.accessToken;

        return endpointConfigurationDTO;
    }

    private EndpointConfiguration findConfigurationOr404(String id) {
        EndpointConfiguration endpointConfiguration = endpointConfigurationRepository.find(id);

        if (endpointConfiguration == null)
            throw new WebApplicationException(404);

        return endpointConfiguration;
    }
}