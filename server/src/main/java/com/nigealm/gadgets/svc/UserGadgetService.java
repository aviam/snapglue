package com.nigealm.gadgets.svc;

import javax.ws.rs.core.Response;

import org.json.JSONException;
import org.codehaus.jettison.json.JSONArray;

public interface UserGadgetService
{
	/**
	 * create a new gadget for a user
	 */
	// public void addGadget(UserGadgetDTO userGadgetDTO);

	/**
	 * updates the given user gadget and its settings
	 * @throws JSONException 
	 */
	// public void updateGadget(UserGadgetDTO userGadgetDTO);
	public Response updateData(JSONArray data);

	/**
	 * delete user gadget
	 */
	// public void deleteGadget(Long gadgetId);

	/**
	 * get all gadgets for the user
	 */
	public String getData();

}
