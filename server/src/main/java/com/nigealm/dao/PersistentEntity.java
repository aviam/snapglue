/*
 * 
 */
package com.nigealm.dao;

import java.io.Serializable;


public interface PersistentEntity<PK extends Serializable> extends Serializable {

	public PK getPrimaryKey();
}
