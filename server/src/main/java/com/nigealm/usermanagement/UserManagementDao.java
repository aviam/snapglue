package com.nigealm.usermanagement;

import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
public interface UserManagementDao {

	UserEntity getUserByName(String userName);
	
	UserEntity getLoggedInUserEntity();

	String getLoggedInUserName();

	JSONArray getAllUsers();

	void createUser(String user);

	void deleteUser(String userId);

	JSONObject getUserById(String id);

	void updateUserById(String user, String id);

	JSONObject getUserByName(String key, String value);

	Document getUserDocByName(String key, String value);

	String getUserTenancyByName(String username);
}
