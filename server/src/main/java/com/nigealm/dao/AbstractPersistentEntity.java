package com.nigealm.dao;

import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@MappedSuperclass
public abstract class AbstractPersistentEntity implements PersistentEntity<Long> {

	private static final long serialVersionUID = -7115974429845712703L;
	@Id
	@GeneratedValue
	protected Long id;

	private String internalProjectId;
	private String internalVersion;

	protected AbstractPersistentEntity()
	{
		// protected - to avoid creating empty bean in the code
	}

	public AbstractPersistentEntity(final String internalProjectId, final String internalVersion)
	{
		this.internalProjectId = internalProjectId;
		this.internalVersion = internalVersion;
	}
	
	@Override
	public Long getPrimaryKey() {
		return id;
	}
	
	public void setPrimaryKey(final Long id) {
		this.id = id;
	}
	
	public String getInternalProjectId() {
		return internalProjectId;
	}

	public void setInternalProjectId(String internalProjectId) {
		this.internalProjectId = internalProjectId;
	}

	public String getIntenralVersion() {
		return internalVersion;
	}

	public void setIntenralVersion(String internalVersion) {
		this.internalVersion = internalVersion;
	}

	@Override
	public String toString() {
		return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).append("id", id)
																		  .toString();
	}
}
