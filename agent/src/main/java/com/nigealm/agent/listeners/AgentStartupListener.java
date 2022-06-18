package com.nigealm.agent.listeners;

import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import com.nigealm.agent.svc.AgentServiceImpl;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import com.nigealm.common.utils.Tracer;

/**
 * Created by Gil on 25/03/2016.
 */
public class AgentStartupListener implements javax.servlet.ServletContextListener
{
	private static Tracer tracer = new Tracer(AgentStartupListener.class);

	@Override
	public void contextDestroyed(ServletContextEvent context)
	{
	}

	@Override
	public void contextInitialized(ServletContextEvent context)
	{
		tracer.entry("contextInitialized");
		addRootPath(context);
		initDBProperties();
		initConfigurations();

	}

	public void initDBProperties()
	{
		Properties prop = new Properties();
		try
		{
			prop.load(AgentStartupListener.class.getResourceAsStream("/agent.properties"));

			//host
			MongoDBAgentServiceImpl.MONGO_DB_HOST = prop.getProperty(MongoDBAgentServiceImpl.MONGO_DB_KEY_HOST,
					MongoDBAgentServiceImpl.MONGO_DB_DEFAULT_HOST);

			//port
			MongoDBAgentServiceImpl.MONGO_DB_PORT = Integer.valueOf(prop.getProperty(
					MongoDBAgentServiceImpl.MONGO_DB_KEY_PORT, MongoDBAgentServiceImpl.MONGO_DB_DEFAULT_PORT));

			//db name
			MongoDBAgentServiceImpl.MONGO_DB_NAME = prop.getProperty(MongoDBAgentServiceImpl.MONGO_DB_KEY_DB_NAME,
					MongoDBAgentServiceImpl.MONGO_DB_DEFAULT_NAME);

			//db URI
			MongoDBAgentServiceImpl.MONGO_DB_URI = prop.getProperty(MongoDBAgentServiceImpl.MONGO_DB_KEY_URI, null);

			tracer.info(
					"-------------------------------------------------------------------------------------------------------");
			String host = MongoDBAgentServiceImpl.MONGO_DB_HOST == null ? "" : MongoDBAgentServiceImpl.MONGO_DB_HOST;
			int port = MongoDBAgentServiceImpl.MONGO_DB_PORT > 0 ? MongoDBAgentServiceImpl.MONGO_DB_PORT : -1;
			String name = MongoDBAgentServiceImpl.MONGO_DB_NAME == null ? "" : MongoDBAgentServiceImpl.MONGO_DB_NAME;
			String uri = MongoDBAgentServiceImpl.MONGO_DB_URI == null ? "" : MongoDBAgentServiceImpl.MONGO_DB_URI;
			
			tracer.info("MONGO DB DETAILS:" + host + "/" + port + "/" + name + "/" + uri);
			tracer.info(
					"-------------------------------------------------------------------------------------------------------");

			//interval
			MongoDBAgentServiceImpl.AGENT_EXECUTION_INTERVAL = Integer.valueOf(prop.getProperty(
					MongoDBAgentServiceImpl.AGENT_EXECUTION_KEY_INTERVAL, MongoDBAgentServiceImpl.AGENT_EXECUTION_KEY_DEFAULT));

			tracer.info("Agent Execution Intervale:" + MongoDBAgentServiceImpl.AGENT_EXECUTION_INTERVAL);
		}
		catch (NumberFormatException | IOException e)
		{
			//just print trace
			tracer.trace("failed to load Agent properties. The reason is: " + e.getMessage());
		}
	}

	public void initConfigurations(){
		Properties prop = new Properties();
		try {
			prop.load(AgentStartupListener.class.getResourceAsStream("/configurations.properties"));
			String configuratiopns=prop.getProperty("configurations");
            if (!configuratiopns.isEmpty() || !configuratiopns.equals("") ) {


				// Loop over all configurations separated by comma .
				String[] tokens = configuratiopns.split(",");
				for (String token : tokens) {
					tracer.info(
							"-------------------------------------------------------------------------------------------------------");
					tracer.info("start Agent collector for configuration id:" + token);
					AgentServiceImpl agentService = new AgentServiceImpl();
					agentService.startAgentJob(token, null);
					tracer.info(
							"-------------------------------------------------------------------------------------------------------");

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private void addRootPath(ServletContextEvent event)
	{
		ServletContext context = event.getServletContext();
		System.setProperty("agentRootPath", context.getRealPath("/"));
	}

}
