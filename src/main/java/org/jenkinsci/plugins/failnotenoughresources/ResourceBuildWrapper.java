package org.jenkinsci.plugins.failnotenoughresources;
import hudson.AbortException;
import hudson.Launcher;
import hudson.Extension;
import hudson.util.FormValidation;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.AbstractProject;
import hudson.node_monitors.DiskSpaceMonitor;
import hudson.tasks.BuildWrapper;
import hudson.tasks.BuildWrapperDescriptor;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.QueryParameter;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * Sample {@link BuildWrapper}.
 *
 * <p>
 * This plugin is used to check resources, before a build
 * if the Resources are not met the build will fail
 *
 * <p>
 * before a build is performed, the {@link #setUp(AbstractBuild, Launcher, BuildListener)}
 * method will be invoked.
 *
 * @author Patrick Eklund
 */
public class ResourceBuildWrapper extends BuildWrapper {

    /**
     * @param configDiskLimitVariable
     *   parameter for disk limit.
     */
    private String configDiskLimitVariable;

    /**
     * Function that gets the class name.
     *
     * @return
     *   Returns the String representation of the Class name.
     **/
    @SuppressWarnings("unused")
    private String getLongName() {
        return this.getClass().getName();
    }

    /**
     * Function that gets the class 'simple' name.
     *
     * @return
     *   Returns the String representation of the Class 'Simple' name.
     **/
    private String getSimpleName() {
        return this.getClass().getSimpleName();
    }

    /**
     * Function that gets the Display name.
     *
     * @return
     *   Returns the Message.properties id for ResourceBuildWrapper.DisplayName.
     **/
    public final String getName() {
        return Messages.ResourceBuildWrapper_DisplayName();
    }

    /**
     * Function that checks if a string value is an integer.
     *
     * @param value
     *   String parameter that is used to to check if its an integer.
     *
     * @return
     *   Returns whether the String was an integer or not.
     **/
    public static boolean isInteger(final String value) {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    /**
     * Function that prints a formated message to the console log.
     *
     * @param listener
     *   Listener that we can use to send output to.
     * @param line1
     *   First Line to print.
     * @param line2
     *   Second Line to print.
     **/
    public final void println(final BuildListener listener, final String line1, final String line2) {
        listener.getLogger().println(String.format("%20s: %25s : [%s]", getSimpleName(), line1, line2));
    }

    /**
     * Function that prints a formated message to the console log.
     *
     * @param listener
     *   Listener that we can use to send output to.
     * @param line1
     *   First Line to print.
     * @param line2
     *   Second Line to print.
     * @param line3
     *   Third Line to print.
     **/
    public final void println(final BuildListener listener, final String line1, final String line2, final String line3) {
        listener.getLogger().println(String.format("%20s: %25s : [%s] %s", getSimpleName(), line1, line2, line3));
    }

    /**
     * Function that prints a formated message to the console log.
     *
     * @param listener
     *   Listener that we can use to send output to.
     * @param line1
     *   First Line to print.
     * @param int1
     *   Firts integer to print.
     * @param line2
     *   Second Line to print.
    **/
    public final void println(final BuildListener listener, final String line1, final int int1, final String line2) {
        listener.getLogger().println(String.format("%20s: %25s : [%d] %s", getSimpleName(), line1, int1, line2));
    }

    /**
     * This method returns the value of the configuration item configDiskLimit.
     *
     * The method name is bit awkward because <tt>config.jelly</tt> calls this method by the naming convention.
     *
     * @return
     *   Returns the string value for configDiskLimit.
     **/
    public final String getconfigDiskLimit() {
        return this.configDiskLimitVariable;
    }

    /**
     *  Fields in config.jelly must match the parameter names in the "DataBoundConstructor".
     *
     *  If the DataBound Constructor is used, otherwise set them with @DataBoundSetter
     */
    @DataBoundConstructor
    public ResourceBuildWrapper() {
        /**
         *  Nothing implemented yet here.
         **/
    }

    /**
     * This method sets the value of the configuration item configDiskLimit.
     * Empty value will result in global value being used instead
     *
     * The method name is bit awkward because <tt>config.jelly</tt> calls this method by the naming convention.
     *
     * @param configDiskLimit
     *   Numerical value to set the job configDiskLimit
     *
     * @throws AbortException
     *   Trying to save a none numerical value, that isn't null, will result in an AbortException being thrown
     **/
    @DataBoundSetter
    public final void setconfigDiskLimit(final String configDiskLimit) throws AbortException {
        if (configDiskLimit.isEmpty() || isInteger(configDiskLimit)) {
            this.configDiskLimitVariable = configDiskLimit;
        } else {
            throw new AbortException("Please remove or set Integer value [" + configDiskLimit + "] Isn't a valid valuer");
        }
    }

    /**
     * Declare setUp.
     *
     * Needed by BuildWraper extension
     *  Return empty env, nothing special needed for Job
     *
     * @param build
     *   Jenkins Build class
     * @param launcher
     *   Jenkins Launcher class
     * @param listener
     *  Jenkins listener class
     *
     * @return
     *   return environment needed for the build
     **/
    @SuppressWarnings("rawtypes")
    @Override
    public final Environment setUp(final AbstractBuild build, final Launcher launcher, final BuildListener listener) {
        return new Environment() {
        };
    }

    /**
     * Declare preCheckout.
     *
     * Preform all checks
     *   Sends an abort exception when to little resources are detected
     *
     * @param build
     *   Jenkins Build class
     * @param launcher
     *   Jenkins Launcher class
     * @param listener
     *  Jenkins listener class
     *
     * @throws IOException
     *   Is used if To little resources are detected.
     *   Is used if we cant calculate the Node size.
     * @throws InterruptedException
     *  Jenkins default.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public final void preCheckout(final AbstractBuild build, final Launcher launcher, final BuildListener listener) throws IOException, InterruptedException {
        /**
         *  Assign local variables
         */
        final int kb = 1024;
        String builtOnStr = build.getBuiltOnStr();
        int convertedNodeDiskSpace = 0;
        if (builtOnStr.equals("")) {
            builtOnStr = "Master";
        }
        try {
            convertedNodeDiskSpace = (int) (DiskSpaceMonitor.DESCRIPTOR.get(build.getBuiltOn().toComputer()).size / (kb * kb * kb));
        } catch (NullPointerException e) {
            throw new AbortException("Aborting Build Could not resolve Node[" + builtOnStr + "] Diskspace, due to NullPointerException" + e);
        }
        int diskLimitUsed = -1;
        int globalDiskLimitVariable = getDescriptor().getglobalDiskLimit();

        /*
         * Global value is forcefully set to 1, or integer value.
         * Job configuration value is forcefully set to "" or integer value.
         */
        if (!configDiskLimitVariable.isEmpty()) {
            diskLimitUsed = Integer.parseInt(configDiskLimitVariable);
        } else {
            diskLimitUsed = globalDiskLimitVariable;
        }
        println(listener, "Inside", "preCheckout");
        println(listener, "Built On", builtOnStr);
        println(listener, "Space Left on Node", convertedNodeDiskSpace , "Gb");
        println(listener, "Global Threshold", globalDiskLimitVariable, "Gb");
        println(listener, "Job Threshold", configDiskLimitVariable, "Gb");
        println(listener, "Using Threshold", diskLimitUsed, "Gb");

        if (convertedNodeDiskSpace < diskLimitUsed) {
            throw new AbortException("Aborting Build Diskspace[" + convertedNodeDiskSpace + "] Gb < Threshold [" + diskLimitUsed + "] Gb");
        }
    }

    /*
     *  Overridden for better type safety.
     *  If your plugin doesn't really define any property on Descriptor,
     *  you don't have to do this.
     *
     *  @return
     *    Returns DescriptorImpl
     *
     */
    @Override
    public final DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    /**
     * Descriptor for {@link DescriptorImpl}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     *
     */
    @Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        /**
         * To persist global configuration information,
         * simply store it in a field and call save().
         *
         * <p>
         * If you don't want fields to be persisted, use <tt>transient</tt>.
         */
        private int globalDiskLimitVariable;

        /**
         * In order to load the persisted global configuration, you have to
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        @Override
        public final boolean isApplicable(final AbstractProject<?, ?> item) {
            return true;
        }

        /**
         * Performs on-the-fly validation of the form
         *   The name of the method follows the convention "doCheckXyz" where "xyz" is the name of the field you put in your view.
         *   The method gets invoked in response to the onchange event on HTML DOM.
         *
         *   The parameter name "value" is also significant. The 'throws' clause isn't.
         *   - https://wiki.jenkins-ci.org/display/JENKINS/Form+Validation
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         *
         * @throws IOException
         *  Jenkins default
         * @throws ServletException
         *  Jenkins default
         *
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user.
         */
        public final FormValidation doCheckConfigDiskLimit(@QueryParameter final String value) throws IOException, ServletException {
            //load the global parameters.
            super.load();

            if (value.isEmpty()) {
                return FormValidation.warning(Messages.ResourceBuildWrapper_GlobalValueUsed(getglobalDiskLimit()));
            } else if (!isInteger(value)) {
                return FormValidation.error(Messages.ResourceBuildWrapper_OnlyUseDigits());
            }
            return FormValidation.ok();
        }

        /**
         * Performs on-the-fly validation of the form.
         *   The name of the method follows the convention "doCheckXyz" where "xyz" is the name of the field you put in your view.
         *   The method gets invoked in response to the onchange event on HTML DOM.
         *
         *   The parameter name "value" is also significant. The 'throws' clause isn't.
         *   - https://wiki.jenkins-ci.org/display/JENKINS/Form+Validation
         *
         * @param value
         *      This parameter receives the value that the user has typed.
         *
         * @throws IOException
         *  Jenkins default
         * @throws ServletException
         *  Jenkins default
         *
         * @return
         *      Indicates the outcome of the validation. This is sent to the browser.
         *      <p>
         *      Note that returning {@link FormValidation#error(String)} does not
         *      prevent the form from being saved. It just means that a message
         *      will be displayed to the user.
         */
        public final FormValidation doCheckGlobalDiskLimit(@QueryParameter final String value) throws IOException, ServletException {
            // This is how you include a value for a form check
            if (value.isEmpty()) {
                return FormValidation.error(Messages.ResourceBuildWrapper_EnterDigits());
            }
            if (!isInteger(value)) {
                return FormValidation.error(Messages.ResourceBuildWrapper_OnlyUseDigits());
            }
            return FormValidation.ok();
        }

        /**
         * used to Indicates that this buildWrapper can be used with all kinds of project types.
         *
         * @param aClass
         *   Jenkins parameter.
         *
         * @return
         *      Indicates the this builder is applicable for all kinds of projects.
         */
        @SuppressWarnings("rawtypes")
        public final boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         *
         *  @return
         *    returns human readable display name of plugin.
         **/
        public final String getDisplayName() {
            return Messages.ResourceBuildWrapper_DisplayName();
        }

        /**
         * This method is used to store the global configuration items.
         *
         * @param req
         *   Jenkins parameter.
         * @param formData
         *   Jenkins parameter.
         *
         * @throws FormException
         *  used when problems with configuring global parameters.
         *
         * @return
         *      returns whether saving was successful or not.
         **/
        @Override
        public final boolean configure(final StaplerRequest req, final JSONObject formData) throws FormException {
            // To persist global configuration information,
            // set that to properties and call save().
            globalDiskLimitVariable = formData.getInt("globalDiskLimit");
            // ^Can also use req.bindJSON(this, formData);
            //  (easier when there are many fields; need set* methods for this, like setUseFrench)
            save();
            return super.configure(req, formData);
        }

        /**
         * This method returns the value of the global configuration item globalDiskLimit.
         *
         * The method name is bit awkward because <tt>global.jelly</tt> calls this method by the naming convention.
         *
         * @return
         *      returns the Global Disk limit
         *      Always reverts to default, value of 1 Gb, if no value is set
         **/
        public final int getglobalDiskLimit() {
            if (globalDiskLimitVariable == 0) {
                return 1;
            } else {
                return this.globalDiskLimitVariable;
            }
        }
    }
}

