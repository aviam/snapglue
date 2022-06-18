package com.nigealm.usermanagement;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.stereotype.Service;
import com.nigealm.common.utils.Tracer;

@Service
@Consumes(
{ "application/json" })
@Produces(
{ "application/json" })
@Path("/usermanagement")
public class UserManagementServiceImpl
{
	private static Tracer tracer = new Tracer(UserManagementServiceImpl.class);

    @Autowired
    private UserManagementDao userManagementDao;
    @Autowired
    @Qualifier("sessionRegistry")
    private SessionRegistry sessionRegistry;

    @GET
    @Path("/users")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    /**
     * http://localhost:8080/rest/usermanagement/users
     */
	public String getAllUsers()
    {
		tracer.entry("getAllUsers");

		JSONArray allusers = userManagementDao.getAllUsers();
		tracer.trace("all users:" + allusers);
		tracer.exit("getAllUsers");
		return allusers.toString();
    }

	@GET
	@Path("/users/{id}")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public String getUserByID(@PathParam("id") String id)
	{
		tracer.entry("getUserByID");
		tracer.trace("user id: " + id);
		JSONObject res = userManagementDao.getUserById(id);
		tracer.exit("getUserByID");
		return res.toString();
	}

	@POST
	@Path("/users")
	@PreAuthorize("hasRole('ROLE_ADMIN')")
	public void addUser(String user)
	{
		userManagementDao.createUser(user);
	}

    @DELETE
	@Path("/users/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
	public void deleteUser(@PathParam("id") String userId)
    {
		tracer.entry("deleteUser");
		tracer.trace("deleteUser " + userId);
		userManagementDao.deleteUser(String.valueOf(userId));
		tracer.exit("deleteUser");
    }

	@PUT
	@Path("/users/{id}")
	@PreAuthorize("hasRole('ROLE_USER') or hasRole('ROLE_ADMIN')")
	public void updateUserById(String user, @PathParam("id") String id)
	{
		tracer.entry("updateUser");
		userManagementDao.updateUserById(user, id);
		tracer.exit("updateUser");
	}

}
