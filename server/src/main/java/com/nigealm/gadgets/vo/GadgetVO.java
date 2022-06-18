package com.nigealm.gadgets.vo;

import com.nigealm.dao.AbstractPersistentEntity;
import com.nigealm.utils.Debug;

// @Entity(name = "GadgetVO")
public class GadgetVO extends AbstractPersistentEntity
{
	private static final long serialVersionUID = 4407724748187286446L;

	public enum GadgetGroup
	{
		ACTIVITIES(0, "Activities"), RULES_AND_ALERTS(1, "Rules and Alerts"), ANALYTICS(2, "Analytics");

		private int id;
		private String name;

		private GadgetGroup(final int id, final String name)
		{
			this.id = id;
			this.name = name;
		}

		public int getId()
		{
			return this.id;
		}

		public String getName()
		{
			return this.name;
		}

		public static GadgetGroup getGroup(final int id)
		{
			switch (id)
			{
				case 0:
					return ACTIVITIES;
				case 1:
					return RULES_AND_ALERTS;
				case 2:
					return ANALYTICS;

				default:
					return null;
			}
		}

		public static GadgetGroup getGroup(final String name)
		{
			if (name.equals(ACTIVITIES.getName()))
				return ACTIVITIES;
			if (name.equals(RULES_AND_ALERTS.getName()))
				return RULES_AND_ALERTS;
			if (name.equals(ANALYTICS.getName()))
				return ANALYTICS;

			Debug.abort("unknown group name");
			return null;
		}
	}

	private int groupId;
	private String name;
	private String kpis;

	protected GadgetVO()
	{
		// protected - to avoid creating empty bean in the code
	}

	public GadgetVO(final GadgetGroup group, final String name, final String kpis)
	{
		this.groupId = group.getId();
		this.name = name;
		this.kpis = kpis;
	}

	public GadgetGroup getGroup()
	{
		return GadgetGroup.getGroup(getGroupId());
	}

	public GadgetGroup getGroup(final String name)
	{
		return GadgetGroup.getGroup(name);
	}

	public int getGroupId()
	{
		return groupId;
	}

	public void setGroupId(int groupId)
	{
		this.groupId = groupId;
	}

	public String getKpis()
	{
		return kpis;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

}
