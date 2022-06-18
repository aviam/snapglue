package com.nigealm.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;


/**
 * generic class for a pair.
 * key may be null
 * method equals compares both value1 and value2 for equality
 * 
 * @author danny.simons
 */
public class Pair<V1,V2> extends AbstractPair<V1, V2>
{
	public static <V1,V2> List<V1> getValue1(Collection<Pair<V1, V2>> p)
	{
		ArrayList<V1> l = new ArrayList<V1>();
		for (Pair<V1, V2> pair : p)
		{
			l.add(pair.getValue1());
		}
		return l;
	}

	public static <V1,V2> List<V2> getValue2(Collection<Pair<V1, V2>> p)
	{
		ArrayList<V2> l = new ArrayList<V2>();
		for (Pair<V1, V2> pair : p)
		{
			l.add(pair.getValue2());
		}
		return l;
	}

	public static <V1,V2> V1 v1(Pair<V1, V2> p)
	{
		if (p == null)
		{
			return null;
		}
		return p.getValue1();
	}
	
	public static <V1,V2> V2 v2(Pair<V1, V2> p)
	{
		if (p == null)
		{
			return null;
		}
		return p.getValue2();
	}

	public static <V1, V2> Pair<V1, V2> findPairByValue1(Collection<Pair<V1, V2>> p, V1 _val)
	{
		for (Pair<V1, V2> pair : p)
		{
			if (Objects.deepEquals(pair.getValue1(), _val))
			{
				return pair;
			}
		}
		return null;
	}

    /** not persisted on db */
	private static final long serialVersionUID = 7455020340373541402L;

	public Pair(V1 value1, V2 value2) {
    	super(value1, value2);
    }
	public V1 getValue1()
	{
		return val1;
	}
	public void setValue1(V1 value)
	{
		this.val1 = value;
	}
	public void setValue2(V2 value)
	{
		this.val2 = value;
	}
	public V2 getValue2()
	{
		return val2;
	}

    @Override
	public boolean equals(Object o) {
    	if (o==null)
    		return false;
        if (!(o instanceof Pair))
            return false;
		@SuppressWarnings("unchecked")
		final Pair<V1, V2> p = (Pair<V1, V2>) o;
		if (!GeneralUtils.equalsNullable(val1, p.val1))
        	return false;
		return GeneralUtils.equalsNullable(val2, p.val2);
    }
}

