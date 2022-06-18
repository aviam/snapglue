	package com.nigealm.gadgets.svc;

import javax.jdo.annotations.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import com.nigealm.usermanagement.UserEntity;
import com.nigealm.usermanagement.UserManagementDao;
import com.nigealm.common.utils.Tracer;

@Consumes(
{ "application/json" })
@Produces(
{ "application/json" })
@Path("/usergadgets")
@Service
public class UserGadgetServiceImpl implements UserGadgetService
{
	private static final Tracer tracer = new Tracer(UserGadgetServiceImpl.class);

	@Autowired
	private UserManagementDao userManagementDao;

	@Override
	@Transactional
	@POST
	@Path("/update")
	@PreAuthorize("hasRole('ROLE_USER')")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)

    public Response updateData(JSONArray data){
		tracer.entry("updateGadgetsData");
		tracer.trace("The gadgets json from client is: " + data.toString());
		tracer.trace("the user data was set successfully");
		tracer.exit("updateGadgetsData");
		return Response.status(200).entity("ok").build();
	}

	@Override
	@GET
	@Path("/data")
	@PreAuthorize("hasRole('ROLE_USER')")
	public String getData()
	{
		tracer.entry("getData");
		UserEntity loggedInUser = userManagementDao.getLoggedInUserEntity();
		String data = loggedInUser.getData();
		tracer.exit("getData");
		return data != null ? data : "";
	}
}
