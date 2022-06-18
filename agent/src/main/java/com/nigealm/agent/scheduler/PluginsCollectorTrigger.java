package com.nigealm.agent.scheduler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.MDC;
import org.bson.Document;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.quartz.InterruptableJob;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.UnableToInterruptJobException;
import org.quartz.impl.StdSchedulerFactory;
import com.mongodb.client.FindIterable;
import com.nigealm.agent.svc.MongoDBAgentServiceImpl;
import com.nigealm.common.utils.Tracer;

public class PluginsCollectorTrigger {
    private static final String FIRST_RUN_INSTALL = "-install";
    private static final PluginsCollectorTrigger instance = new PluginsCollectorTrigger();
    private static final Tracer tracer = new Tracer(PluginsCollectorTrigger.class);

    private HashMap<String, JobDetail> runningJob = new HashMap<>();
    private HashMap<String, InterruptableJob> firstTimeRunningJob = new HashMap<>();
    private String serverPort;
    private String serverHost;
    private String serverPath;
    private Scheduler scheduler;


    private PluginsCollectorTrigger() {
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
        } catch (SchedulerException e) {
            tracer.exception("PluginsCollectorTrigger", e);
        }
    }

    public static PluginsCollectorTrigger getInstance() {
        return instance;
    }

    private void retrieveDataRepeatedly(JobDetail job, String confId) throws SchedulerException {

		int interval = getInterval(confId);

        SimpleTrigger trigger = TriggerBuilder.newTrigger().withIdentity(confId)
				.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(interval).repeatForever())
                .build();
        if (!scheduler.isStarted() || scheduler.isInStandbyMode()) {
            scheduler.start();
        }

        scheduler.scheduleJob(job, trigger);
        runningJob.put(confId, job);
    }

	/**
	 * Returns the interval for the given configuration id.
	 * 
	 * @param confId
	 */
	private static int getInterval(String confId)
	{
		int interval = getIntervalByConfigurationID(confId);
		if(interval > 0 )
			return interval;
		else
			return MongoDBAgentServiceImpl.AGENT_EXECUTION_INTERVAL;
	}
		
	/**
	 * Returns the interval for the given configuration id.
	 * 
	 * @param confId
	 */
	private static int getIntervalByConfigurationID(String confId)
	{
		//find the configuration by id
		MongoDBAgentServiceImpl mongoService = new MongoDBAgentServiceImpl();
		FindIterable<Document> allDocuments = mongoService.getAllDocuments();
		Document configurationDoc = null;
		for (Document document : allDocuments)
		{
			String currentConfId = document.get("_id").toString();
			if (confId.equals(currentConfId))
			{
				configurationDoc = document;
				break;
			}
		}

		int res = -1;
		if (configurationDoc != null)
			res = configurationDoc.getInteger("interval", -1);
		return res;
	}

    private void retrieveAllDataOnce(JobDetail jobDetail) {
        PluginsCollectorJob job = new PluginsCollectorJob(jobDetail);
        Thread jobThread = new Thread(job);
        jobThread.start();
        try {
            jobThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tracer.exception("retrieveAllDataOnce", e);
        }
    }

    private JobDetail createJob(String confId) {
        JobDetail job = JobBuilder.newJob(PluginsCollectorJob.class).withIdentity(confId).build();
        job.getJobDataMap().put("serverHost", serverHost);
        job.getJobDataMap().put("serverPort", serverPort);
        job.getJobDataMap().put("serverPath", serverPath);
        job.getJobDataMap().put("confId", confId);
        return job;
    }

    private void readAgentConfigurationFile() throws IOException {
        Properties prop = new Properties();
        prop.load(PluginsCollectorTrigger.class.getResourceAsStream("/agent.properties"));
        serverHost = prop.getProperty("serverHost");
        serverPort = prop.getProperty("serverPort");
        serverPath = prop.getProperty("serverPath");
    }

    public synchronized void start(boolean installAll, final String confId, final String notify) {
        try {
            readAgentConfigurationFile();
            final JobDetail job = createJob(confId);

            if (installAll) {
                Runnable firstRun = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            job.getJobDataMap().put("retrieveAll", true);
                            retrieveAllDataOnce(job);
                            tracer.info("setting configuration: " + confId + " to Available");
                            setConfigurationAvailability(confId);
                            tracer.info("sending post for configuration: " + confId);
                            doPost(notify, confId);
                            job.getJobDataMap().put("retrieveAll", false);
                            retrieveDataRepeatedly(job, confId);
                        } catch (SchedulerException | JSONException e) {
                            tracer.exception("conf_id: " + confId + "First Run Thread", e);
                        }
                    }
                };
                new Thread(firstRun).start();
            } else {
                job.getJobDataMap().put("retrieveAll", false);
                retrieveDataRepeatedly(job, confId);
            }
        } catch (SchedulerException | IOException e) {
            tracer.exception("start", e);
        }
    }

    private void setConfigurationAvailability(String confId) throws JSONException {
        MongoDBAgentServiceImpl mongoService = new MongoDBAgentServiceImpl();
        Document doc = mongoService.getDocumentByID(confId);
        if (doc != null) {
            doc.put("available", true);
            mongoService.updateConfiguration(new JSONObject(doc.toJson()), confId);
        }
    }

    private void doPost(String notify, String confId) {
        DefaultHttpClient httpClient = new DefaultHttpClient();
        try {
            HttpPost httpPost = new HttpPost(notify);
            httpPost.setHeader("Content-type", "application/json");
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("configuration_id", confId);
            httpPost.setEntity(new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
            HttpResponse response = httpClient.execute(httpPost);
            tracer.info("Response from Agent post: " + response.getStatusLine().getStatusCode());
        } catch (JSONException | IOException e) {
            tracer.exception("doPost", e);
        }
    }

    public synchronized void stopJob(String confId) {
        try {
            JobDetail jobDetail = runningJob.get(confId);
            if (jobDetail != null) {
                stopRunningJob(confId, jobDetail);

                return;
            } else {
                InterruptableJob interruptableJob = firstTimeRunningJob.get(confId);
                if (interruptableJob != null) {
                    stopFirstTimeRunningJob(interruptableJob, confId);
                }
            }
            tracer.warn("There is no running job for confId " + confId);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            tracer.exception("stopJob", e);
        } catch (SchedulerException e) {
            tracer.exception("stopJob", e);
        }
    }

    private void stopFirstTimeRunningJob(InterruptableJob interruptableJob, String confId) throws
            UnableToInterruptJobException, InterruptedException {
        tracer.info("------- Shutting Down --------");
        tracer.info("The job is: " + confId);
        interruptableJob.interrupt();
        Thread.sleep(30000);
        tracer.info("------- Shutting Down Completed--------");
        firstTimeRunningJob.remove(confId);
    }

    private void stopRunningJob(String confId, JobDetail jobDetail) throws SchedulerException, InterruptedException {
        TriggerKey tk = TriggerKey.triggerKey(confId);
        tracer.info("------- Shutting Down --------");
        scheduler.unscheduleJob(tk);
        tracer.info("The job is: " + jobDetail.getKey());
        Thread.sleep(30000);
        tracer.info("------- Shutting Down Completed--------");
        runningJob.remove(confId);
        if (runningJob.size() == 0) {
            scheduler.standby();
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length < 2) {
                return;
            }

            PluginsCollectorTrigger trigger = PluginsCollectorTrigger.getInstance();
            trigger.readAgentConfigurationFile();
            String confId = "Dummy";
            trigger.serverHost = args[0];
            trigger.serverPort = args[1];
            for (String arg : args) {
                if (FIRST_RUN_INSTALL.equals(arg)) {
                    JobDetail job = trigger.createJob("PluginsFirstInstallJob");
                    job.getJobDataMap().put("retrieveAll", true);
                    trigger.retrieveAllDataOnce(job);
                }
            }

            // Trigger the job to run every X seconds
            JobDetail job = trigger.createJob("PluginsCollectorJob");
            job.getJobDataMap().put("retrieveAll", false);
            trigger.retrieveDataRepeatedly(job, confId);
        } catch (Exception e) {
            tracer.exception("main", e);
        }
    }


}