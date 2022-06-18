package com.nigealm.common.utils;

import java.io.Serializable;

public abstract class AbstractPair<V1, V2> implements Serializable
{
	private static final long serialVersionUID = 9139222066418485448L;

	protected V1 val1;
	protected V2 val2;

	/**
	 * Creates a pair.
	 * 
	 * @param v1 first object in the pair
	 * @param v2 second object in the pair
	 */
	protected AbstractPair(V1 v1, V2 v2)
	{
		val1 = v1;
		val2 = v2;
	}

	@Override
	public String toString()
	{
		return "[" + val1 + " " + val2 + "]";
	}

	@Override
	public abstract boolean equals(Object o);

	@Override
	public int hashCode()
	{
		return (val1 != null ? val1.hashCode() : 0) + (val2 != null ? val2.hashCode() : 17);
	}
}
