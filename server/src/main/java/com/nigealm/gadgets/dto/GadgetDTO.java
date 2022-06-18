package com.nigealm.gadgets.dto;

import com.nigealm.gadgets.vo.GadgetVO.GadgetGroup;

public class GadgetDTO
{

	private Long id;

	private GadgetGroup group;
	private String name;// such as "Daily Report"
	private String kpis;

	public GadgetDTO(final Long id, final GadgetGroup group, final String name, final String kpis)
	{
		this.id = id;
		this.group = group;
		this.name = name;
		this.kpis = kpis;
	}

	public Long getId()
	{
		return id;
	}

	public GadgetGroup getGroup()
	{
		return group;
	}

	public String getName()
	{
		return name;
	}

	public String getKpis()
	{
		return kpis;
	}
}
