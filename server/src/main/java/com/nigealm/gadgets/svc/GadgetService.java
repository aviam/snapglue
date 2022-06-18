package com.nigealm.gadgets.svc;

import java.util.List;

import com.nigealm.gadgets.dto.GadgetDTO;

public interface GadgetService
{

	// read actions
	/**
	 * get all gadgets defined in the db
	 * 
	 * URL: "/all"
	 */
	public List<GadgetDTO> getAllGadgets();

}
