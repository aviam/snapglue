package com.nigealm.usermanagement;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.nigealm.common.utils.Tracer;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.utils.JSONUtils;
import org.apache.commons.validator.EmailValidator;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class UserManagementDaoImpl implements UserManagementDao {
    private static final Tracer tracer = new Tracer(UserManagementDaoImpl.class);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SaltSource saltSource;

    @Override
    public UserEntity getUserByName(String name) {
        tracer.entry("getUserByName");

        FindIterable<Document> users = MongoConnectionManager
                .findAllDocsInCollectionByValue(MongoDBCollection.USERS.name(), "username", name);
        MongoCursor<Document> cursor = users.iterator();
        while (cursor.hasNext()) {
            Document doc = cursor.next();
            tracer.trace("user found: " + doc.toString());
            String username = doc.getString("username");
            String role = doc.getString("role");
            String email = doc.getString("email");
            String password = doc.getString("password");
            String pictureLink = doc.getString("pictureLink");
            String tenant = doc.getString("tenant");
            String data = doc.getString("data");

            boolean enabled = doc.getBoolean("enabled");
            boolean accountNonExpired = doc.getBoolean("accountNonExpired");
            boolean credentialsNonExpired = doc.getBoolean("credentialsNonExpired");
            boolean accountNonLocked = doc.getBoolean("accountNonLocked");
            tracer.exit("getUserByName");
            return new UserEntity(username, role, email, password, pictureLink, enabled, accountNonExpired,
                    credentialsNonExpired, accountNonLocked, tenant, data);
        }

        tracer.trace("user not found: " + name);
        return null;
    }

    @Override
    public JSONArray getAllUsers() {
        tracer.entry("getAllUsers");

        FindIterable<Document> allUsers = MongoConnectionManager
                .findAllDocsInCollection(MongoDBCollection.USERS.name());

        JSONArray res = JSONUtils.convertDocumentListToJSONArray(allUsers);
        tracer.exit("getAllUsers");
        return res;
    }

    @Override
    public void createUser(String user) {
        tracer.entry("createUser");

        // first check if this user already exists in the system
        Document userDoc = Document.parse(user);
        String username = (String) userDoc.get("username");

        FindIterable<Document> adminUsers = MongoConnectionManager
                .findAllDocsInCollectionByValue(MongoDBCollection.USERS.name(),
                        "username", username);
        MongoCursor<Document> cursor = adminUsers.iterator();
        if (cursor.hasNext()) {
            tracer.trace("A new user was not created as this user already exists in the system:" + user);
            return;
        }


        String role = (String) userDoc.get("role");
        String email = (String) userDoc.get("email");
        String password = (String) userDoc.get("password");
        String tenant = (String) userDoc.get("tenant");

        validateMail(username);
        validateMail(email);
        validateTenent(tenant);

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(role));
        UserDetails userDetails = new User(username, password, authorities);
        String hashedPassword = passwordEncoder.encodePassword(password, saltSource.getSalt(userDetails));

        userDoc.put("password", hashedPassword);

        MongoConnectionManager.addDocument(MongoDBCollection.USERS.name(), userDoc);

        tracer.trace("user created: " + userDoc.toString());
        tracer.exit("createUser");
    }

    private void validateTenent(String tenant) {
        boolean tenantIsValid = tenant != null;
        if (!tenantIsValid) {
            throw new IllegalArgumentException("Tenant is not valid");
        }
    }

    @Override
    public void deleteUser(String id) {
        tracer.entry("deleteUser");
        MongoConnectionManager.deleteDocumentByID(MongoDBCollection.USERS.name(), id);
        tracer.trace("user deleted:" + id);
        tracer.exit("deleteUser");
    }

    @Override
    public JSONObject getUserById(String id) {
        tracer.entry("getUserById");
        tracer.trace("looking for user:" + id);
        Document doc = MongoConnectionManager.findDocumentInCollectionById(MongoDBCollection.USERS.name(), id);
        JSONObject jsonObject = JSONUtils.convertDocumentToJSONObject(doc);
        tracer.trace("user found:" + jsonObject.toString());
        tracer.exit("getUserById");
        return jsonObject;
    }

    @Override
    public JSONObject getUserByName(String key, String value) {
        tracer.entry("getUserByByName");
        tracer.trace("looking for key:" + key);
        Document doc = MongoConnectionManager.findDocumentInCollectionByKeyAndValue(MongoDBCollection.USERS.name(),
                key, value);
        if (doc == null)
            return null;
        JSONObject jsonObject = JSONUtils.convertDocumentToJSONObject(doc);
        tracer.trace("user found:" + jsonObject);
        tracer.exit("getUserByByName");
        return jsonObject;
    }

    @Override
    public Document getUserDocByName(String key, String value) {
        tracer.entry("getUserDocByName");
        return MongoConnectionManager.findDocumentInCollectionByKeyAndValue(MongoDBCollection.USERS.name(), key,
                value);
    }

    @Override
    public String getUserTenancyByName(String username) {
        tracer.entry("getUserTenancyByName");
        Document doc = MongoConnectionManager.findDocumentInCollectionByKeyAndValue(MongoDBCollection.USERS.name(),
                "username", username);
        if (doc == null)
            return null;

        return (String) doc.get("tenant");
    }

    @Override
    public void updateUserById(String user, String id) {
        tracer.entry("updateUserDetails");

        Document userDoc = Document.parse(user);

        String role = (String) userDoc.get("role");
        String username = (String) userDoc.get("username");
        String email = (String) userDoc.get("email");
        String password = (String) userDoc.get("password");

        validateMail(username);
        validateMail(email);

        if (password.length() < 30)//update password only if not encoded
        {
            Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
            authorities.add(new SimpleGrantedAuthority(role));
            UserDetails userDetails = new User(username, password, authorities);
            String hashedPassword = passwordEncoder.encodePassword(password, saltSource.getSalt(userDetails));

            userDoc.put("password", hashedPassword);
        }

        MongoConnectionManager.updateDocument(MongoDBCollection.USERS.name(), userDoc, id);

        tracer.trace("finished updating user:" + id);
        tracer.exit("updateUserDetails");
    }

    public UserEntity getLoggedInUserEntity() {
        String loggedInUserName = getLoggedInUserName();
        return getUserByName(loggedInUserName);
    }

    public String getLoggedInUserName() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private static void validateMail(String email) {
        boolean mailIsValid = EmailValidator.getInstance().isValid(email);
        if (!mailIsValid) {
            throw new IllegalArgumentException("Email is not valid");
        }
    }

}
