package org.jenkinsci.plugins.crittercism_dsym;
import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 *
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked
 * and a new {@link HelloWorldBuilder} is created. The created
 * instance is persisted to the project configuration XML by using
 * XStream, so this allows you to use instance fields (like {@link #name})
 * to remember the configuration.
 *
 * <p>
 * When a build is performed, the {@link #perform(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked. 
 *
 * @author Kohsuke Kawaguchi
 */
public class CrittercismDsymRecorder extends Recorder
{
    private final String apiKey;
    private final String appID;
    private final String filePath;

    // Fields in config.jelly must match the parameter names in the "DataBoundConstructor"
    @DataBoundConstructor
    public CrittercismDsymRecorder(String apiKey, String appID, String filePath)
    {
        this.apiKey = apiKey;
        this.appID = appID;
        this.filePath = filePath;
    }

    public String getApiKey()
    {
    	return this.apiKey;
    }
    
    public String getAppID()
    {
    	return this.appID;
    }
    
    public String getFilePath()
    {
    	return this.filePath;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener)
    {
        if(build.getResult().isWorseOrEqualTo(Result.FAILURE))
        {
        	// Build failed
        	return false;
        }
        
        listener.getLogger().println("Uploading dSYM to Crittercism...");
        try
        {
        	EnvVars vars = build.getEnvironment(listener);
        	File file = new File(vars.expand(filePath));
        	listener.getLogger().println(file);
        	
        	HttpClient httpClient = new DefaultHttpClient();
        	HttpPost httpPost = new HttpPost("https://www.crittercism.com/api_beta/dsym/" + this.appID);
        	FileBody fileBody = new FileBody(file);
        	
        	MultipartEntity entity = new MultipartEntity();
        	entity.addPart("key", new StringBody(this.apiKey));
        	entity.addPart("dsym", fileBody);
        	httpPost.setEntity(entity);
        	
        	HttpResponse response = httpClient.execute(httpPost);        	
        	if(response.getStatusLine().getStatusCode() == 200)
        	{
        		// Successful
        		listener.getLogger().println("Successfully uploaded dSYM file to Crittercism!");
        	}
        	else
        	{
        		// Error
        		listener.getLogger().println("Failed to upload dSYM file to Crittercism - Error " + response.getStatusLine().getStatusCode() + ": " + response.getStatusLine().getReasonPhrase());
        	}
        }
        catch(Exception e)
        {
        	listener.getLogger().println(e);
        }
        
        return true;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }

    /**
     * Descriptor for {@link CrittercismDsymRecorder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     * <p>
     * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt>
     * for the actual HTML fragment for the configuration screen.
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types 
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Upload dSYM to Crittercism";
        }
    }

	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}
}

