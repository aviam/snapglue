package com.nigealm.gadgets.dto;

public class GadgetSettingsDTO
{

	private Long id;
	private int itemsLimitNumber;

	public GadgetSettingsDTO(final Long id, final int itemsLimitNumber)
	{
		this.id = id;
		this.itemsLimitNumber = itemsLimitNumber;
	}

	public Long getId()
	{
		return id;
	}

	public int getItemsLimitNumber()
	{
		return itemsLimitNumber;
	}

	public void setItemsLimitNumber(int itemsLimitNumber)
	{
		this.itemsLimitNumber = itemsLimitNumber;
	}
}
