package com.nigealm.Listeners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.dao.SaltSource;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.support.WebApplicationContextUtils;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.nigealm.common.utils.Tracer;
import com.nigealm.mongodb.MongoConnectionManager;
import com.nigealm.mongodb.MongoConnectionManager.MongoDBCollection;
import com.nigealm.rules.svc.RulesService;
import com.nigealm.usermanagement.UserEntity;

/**
 * This class serves as a servlet listener which is invoked on server startup.
 * Creates the administrator user. Creates the UserEntity if needed.
 * 
 */
public class ServerStartupListener implements javax.servlet.ServletContextListener
{
	private static Tracer tracer = new Tracer(ServerStartupListener.class);
	
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private SaltSource saltSource;
	@Autowired
	private RulesService rulesService;

	@Override
	public void contextDestroyed(ServletContextEvent context)
	{
		// nothing to do yet
	}

	@Override
	public void contextInitialized(ServletContextEvent context) 
	{
		tracer.entry("contextInitialized");

		//load autowired service
		WebApplicationContextUtils.getRequiredWebApplicationContext(context.getServletContext())
				.getAutowireCapableBeanFactory().autowireBean(this);
        addRootPath(context);
		initDBProperties();
		createDefaultUser(context);
		createDefaultRules();
	}

    private void addRootPath(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        System.setProperty("serverRootPath", context.getRealPath("/"));
    }

    private void initDBProperties()
	{
		Properties prop = new Properties();
		try
		{
			prop.load(ServerStartupListener.class.getResourceAsStream("/server.properties"));

			//host
			MongoConnectionManager.MONGO_DB_HOST = prop.getProperty(MongoConnectionManager.MONGO_DB_KEY_HOST,
					MongoConnectionManager.MONGO_DB_DEFAULT_HOST);

			//port
			MongoConnectionManager.MONGO_DB_PORT = Integer.valueOf(prop.getProperty(
					MongoConnectionManager.MONGO_DB_KEY_PORT, MongoConnectionManager.MONGO_DB_DEFAULT_PORT));

			//db name
			MongoConnectionManager.MONGO_DB_NAME = prop.getProperty(MongoConnectionManager.MONGO_DB_KEY_DB_NAME,
					MongoConnectionManager.MONGO_DB_DEFAULT_NAME);

			//db URI
			MongoConnectionManager.MONGO_DB_URI = prop.getProperty(MongoConnectionManager.MONGO_DB_KEY_URI, null);

			tracer.info(
					"-------------------------------------------------------------------------------------------------------");
			String host = MongoConnectionManager.MONGO_DB_HOST == null ? "" : MongoConnectionManager.MONGO_DB_HOST;
			int port = MongoConnectionManager.MONGO_DB_PORT > 0 ? MongoConnectionManager.MONGO_DB_PORT : -1;
			String name = MongoConnectionManager.MONGO_DB_NAME == null ? "" : MongoConnectionManager.MONGO_DB_NAME;
			String uri = MongoConnectionManager.MONGO_DB_URI == null ? "" : MongoConnectionManager.MONGO_DB_URI;
			tracer.info(
					"-------------------------------------------------------------------------------------------------------");
		
			tracer.info("MONGO DB DETAILS:" + host + "/" + port + "/" + name + "/" + uri);
		}
		catch (IOException e)
		{
			//do nothing
		}
	}

	private void createDefaultRules()
	{
		FindIterable<Document> docs = MongoConnectionManager.findAllDocsInCollection(MongoDBCollection.RULES.name());
		MongoCursor<Document> cursor = docs.iterator();
		if (cursor.hasNext())
			return;//rules exist, no need to create

		//rules do not exist - create
		rulesService.insertDefaultRules();
	}

	private void createDefaultUser(ServletContextEvent context)
	{
		FindIterable<Document> adminUsers = MongoConnectionManager.findAllDocsInCollectionByValue(
				MongoDBCollection.USERS.name(), "username", UserEntity.ADMINISTRATOR_USER_NAME);
		MongoCursor<Document> cursor = adminUsers.iterator();
		int counter = 0;
		while (cursor.hasNext())
		{
			tracer.trace("admin user already exists ! ! !");

			counter++;
			Document doc = cursor.next();
			if (counter > 1)
			{
				tracer.trace("more than 1 admin users were found. goind to delete document: " + doc.toString());
				MongoConnectionManager.removeDocument(MongoDBCollection.USERS.name(), doc);
				tracer.trace("document deleted");
			}
		}

		//if an admin user exists
		if (counter > 0)
		{
			return;
		}
		else
		{
			tracer.trace("admin user not found. Creating . . . .");

			Document doc = new Document();
			doc.put("username", UserEntity.ADMINISTRATOR_USER_NAME);
			doc.put("role", UserEntity.ADMINISTRATOR_USER_ROLE);
			doc.put("email", UserEntity.ADMINISTRATOR_USER_EMAIL);
			doc.put("data", UserEntity.ADMINISTRATOR_USER_DATA);
			doc.put("pictureLink", UserEntity.ADMINISTRATOR_USER_PICTURE_LINK);
			doc.put("enabled", UserEntity.ADMINISTRATOR_USER_ENABLED);
			doc.put("accountNonExpired", UserEntity.ADMINISTRATOR_USER_ACCOUNT_NON_EXPIRED);
			doc.put("credentialsNonExpired", UserEntity.ADMINISTRATOR_USER_CREDENTIALS_NON_EXPIRED);
			doc.put("accountNonLocked", UserEntity.ADMINISTRATOR_USER_ACCOUNT_NON_LOCKED);
			doc.put("tenant", UserEntity.ADMINISTRATOR_USER_TENENT);

			//encode password
			Collection<GrantedAuthority> authorities = new ArrayList<GrantedAuthority>();
			authorities.add(new SimpleGrantedAuthority(UserEntity.ADMINISTRATOR_USER_ROLE));
			UserDetails user = new User(UserEntity.ADMINISTRATOR_USER_NAME, UserEntity.ADMINISTRATOR_USER_PASSWORD,
					true, true, true, true, authorities);
			String hashedPassword = passwordEncoder.encodePassword(UserEntity.ADMINISTRATOR_USER_PASSWORD,
					saltSource.getSalt(user));

//			String hashedPassword = passwordEncoder.encodePassword(UserEntity.ADMINISTRATOR_USER_PASSWORD, null);

			doc.put("password", hashedPassword);
			MongoConnectionManager.addDocument(MongoDBCollection.USERS.name(), doc);

			tracer.trace("admin user created: " + doc.toString());
		}
	}
}
