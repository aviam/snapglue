package com.nigealm.agent.svc;

import com.nigealm.agent.scheduler.PluginsCollectorTrigger;
import com.nigealm.common.utils.Tracer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.ws.rs.*;

/**
 * Created by Gil on 19/03/2016.
 */

@Consumes(
        {"application/json"})
@Produces(
        {"application/json"})
@Path("/api")
@Service
public class AgentServiceImpl {
    private final static Tracer tracer = new Tracer(AgentServiceImpl.class);
    private static PluginsCollectorTrigger pt = PluginsCollectorTrigger.getInstance();

    @GET
    @Path("/stopAgentJob")
    public String stopAgentJob(@QueryParam("confId") String confId) {
        tracer.info("confId is: " + confId);
        pt.stopJob(confId);
        tracer.exit("StopAgentJob From Service");
        return "Agent Job Stop Successfully";
    }

    @GET
    @Path("/startAgentJob")
    public String startAgentJob(@QueryParam("confId") String confId,
                                @QueryParam("notify_url") String notify) {
        try {
            tracer.info("confId is: " + confId);
            boolean installAll = !StringUtils.isEmpty(notify);
            pt.start(installAll, confId, notify);
        } catch (Exception e) {
            tracer.exception("startAgentJob", e);
            System.out.println("Failed Start Agent..............");
            return "Agent Job failed to start with this message: " + e.getMessage();
        }
        tracer.exit("StartAgentJob From Service");
        return "Agent Job Start Successfully";
    }
}
