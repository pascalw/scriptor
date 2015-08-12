package nl.pwiddershoven.script.controller;

import java.net.URI;
import java.net.URISyntaxException;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import nl.pwiddershoven.script.config.AuthenticationNotRequired;
import nl.pwiddershoven.script.repository.ScriptConfigurationRepository;
import nl.pwiddershoven.script.service.ScriptConfiguration;
import nl.pwiddershoven.script.service.ScriptExecutor;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Consumes(MediaType.APPLICATION_JSON)
@Path("/")
public class ScriptController {
    private final Logger logger = Logger.getLogger(ScriptController.class);

    static class ScriptConfigurationDTO {
        @NotNull
        public String script;
        @NotNull
        public String contentType;
    }

    @Autowired
    private ScriptExecutor scriptExecutor;

    @Autowired
    private ScriptConfigurationRepository scriptConfigurationRepository;

    @POST
    @Path("/execute")
    public Response execute(ScriptConfigurationDTO scriptConfigurationDTO) {
        ScriptConfiguration scriptConfiguration = buildScriptConfiguration(scriptConfigurationDTO);
        return doExecute(scriptConfiguration);
    }

    @POST
    @Path("/config")
    public Response createConfiguration(@Valid ScriptConfigurationDTO scriptConfigurationDTO) {
        String id = scriptConfigurationRepository.save(buildScriptConfiguration(scriptConfigurationDTO));
        return Response.created(getLocation(id)).build();
    }

    @PUT
    @Path("/config/{id}")
    public void updateConfiguration(@PathParam("id") String id, ScriptConfigurationDTO scriptConfigurationDTO) {
        scriptConfigurationRepository.update(id, buildScriptConfiguration(scriptConfigurationDTO));
    }

    private ScriptConfiguration buildScriptConfiguration(ScriptConfigurationDTO scriptConfigurationDTO) {
        return new ScriptConfiguration(scriptConfigurationDTO.script, scriptConfigurationDTO.contentType);
    }

    @GET
    @Path("/config/{id}")
    public ScriptConfigurationDTO getScriptConfiguration(@PathParam("id") String id) {
        ScriptConfiguration scriptConfiguration = scriptConfigurationRepository.find(id);

        ScriptConfigurationDTO scriptConfigurationDTO = new ScriptConfigurationDTO();
        scriptConfigurationDTO.script = scriptConfiguration.processingScript;
        scriptConfigurationDTO.contentType = scriptConfiguration.contentType;

        return scriptConfigurationDTO;
    }

    @GET
    @AuthenticationNotRequired
    @Path("/executions/{id}")
    public Object getConfiguration(@PathParam("id") String id) {
        ScriptConfiguration scriptConfiguration = scriptConfigurationRepository.find(id);
        return doExecute(scriptConfiguration);
    }

    private Response doExecute(ScriptConfiguration scriptConfiguration) {
        long start = System.currentTimeMillis();
        Object result = scriptExecutor.execute(scriptConfiguration);
        long end = System.currentTimeMillis();

        logger.info("Processing took " + (end - start));

        return Response.ok()
                .type(scriptConfiguration.contentType)
                .entity(result)
                .build();
    }

    private URI getLocation(String id) {
        try {
            return new URI("/config/" + id);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}