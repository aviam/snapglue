package com.nigealm.gadgets.dto;





public class UserGadgetDTO
{

	private Long id;

	private String gadgetsData;

	private GadgetDTO gadget;
	private GadgetSettingsDTO gadgetSettings;

	// private int topLeftX;
	// private int topLeftY;
	// private int width;
	// private int height;

	public UserGadgetDTO(final Long id, final String gadgetsData)
	{
		this.id = id;
		this.gadgetsData = gadgetsData;
	}

	// public UserGadgetDTO(final Long id, final GadgetDTO gadget, final int
	// topLeftX, final int topLeftY,
	// final int width,
	// final int height, final GadgetSettingsDTO gadgetSettings)
	// {
	// this.id = id;
	// this.gadget = gadget;
	// this.topLeftX = topLeftX;
	// this.topLeftY = topLeftY;
	// this.width = width;
	// this.height = height;
	// this.gadgetSettings = gadgetSettings;
	// }

	public String getGadgetsData()
	{
		return gadgetsData;
	}

	public void setGadgetsData(String gadgetsData)
	{
		this.gadgetsData = gadgetsData;
	}

	public GadgetDTO getGadget()
	{
		return gadget;
	}

	public void setGadget(GadgetDTO gadget)
	{
		this.gadget = gadget;
	}

	public GadgetSettingsDTO getGadgetSettings()
	{
		return gadgetSettings;
	}

	public void setGadgetSettings(GadgetSettingsDTO gadgetSettings)
	{
		this.gadgetSettings = gadgetSettings;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}
}