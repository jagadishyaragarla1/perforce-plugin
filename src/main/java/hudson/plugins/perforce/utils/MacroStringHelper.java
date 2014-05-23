/*
 * The MIT License
 *
 * Copyright 2013 Mike Wille, Brian Westrich, Victor Szoltysek,
 *                Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.perforce.utils;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Hudson;
import hudson.model.Node;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.model.ParametersDefinitionProperty;
import hudson.model.TaskListener;
import hudson.plugins.perforce.PerforceSCM;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.slaves.NodeProperty;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * The class aggregates all variables substitution methods within the plugin.
 * @author Mike Wille
 * @author Brian Westrich
 * @author Victor Szoltysek
 * @author Rob Petti
 * @author Oleg Nenashev <nenashev@synopsys.com>, Synopsys Inc.
 * @since 1.3.25
 */
public class MacroStringHelper {
    
    public static final Level SUBSTITUTION_ERROR_LEVEL = Level.WARNING;
    
    /**
     * Substitute parameters and validate contents of the resulting string.
     * This is a generic method, which invokes routines for {@link AbstractBuild} if it is not null.
     * Otherwise, the default handlers for {@link AbstractProject} and {@link Node} will be used.
     * @param string Input string to be substituted
     * @param instance Instance of {@link PerforceSCM}
     * @param build A build to be substituted
     * @param project A project
     * @param node A node to be substituted
     * @param env Additional environment variables.
     * @return Substituted string
     * @throws ParameterSubstitutionException Format error (unresolved variable, etc.)
     */
    public static String substituteParameters(
            @Nonnull String string,
            @Nonnull PerforceSCM instance,        
            @CheckForNull AbstractBuild build,
            @CheckForNull AbstractProject project,
            @CheckForNull Node node,
            @CheckForNull Map<String, String> env)
            throws ParameterSubstitutionException {
        
        return build != null
                ? substituteParameters(string, instance, build, env)
                : substituteParameters(string, instance, project, node, env);
    }
    
    /**
     * Substitute parameters and validate contents of the resulting string
     * @param string Input string to be substituted
     * @param instance Instance of {@link PerforceSCM}
     * @param project A project
     * @param node A node to be substituted
     * @param env Additional environment variables.
     * @return Substituted string
     * @throws ParameterSubstitutionException Format error (unresolved variable, etc.)
     */
    public static String substituteParameters(
            @Nonnull String string,
            @Nonnull PerforceSCM instance,
            @CheckForNull AbstractProject project,
            @CheckForNull Node node,
            @CheckForNull Map<String, String> env)
            throws ParameterSubstitutionException {
        String result = substituteParametersNoCheck(string, instance, project, node, env);
        checkString(result);
        return result;
    }
    
    /**
     * Substitute parameters and validate contents of the resulting string
     * @param string Input string
     * @param subst Variables Map
     * @return Substituted string
     * @throws ParameterSubstitutionException Format error (unresolved variable, etc.)
     */
    public static String substituteParameters(String string, Map<String, String> subst) 
            throws ParameterSubstitutionException
    {
        String result = substituteParametersNoCheck(string, subst);
        checkString(result);
        return result;
    }
    
    /**
     * Substitute parameters and validate contents of the resulting string
     * @param string Input string to be substituted
     * @param instance Instance of {@link PerforceSCM}
     * @param build A build to be substituted
     * @param env Additional environment variables.
     * @return Substituted string
     * @throws ParameterSubstitutionException Format error (unresolved variable, etc.)
     */
    public static String substituteParameters(
            @Nonnull String string,
            @Nonnull PerforceSCM instance,
            @Nonnull AbstractBuild build,
            @CheckForNull Map<String, String> env)
            throws ParameterSubstitutionException {
        String result = substituteParametersNoCheck(string, instance, build, env);
        checkString(result);
        return result;
    }
    
    /**
     * Checks string from unsubstituted variable references.
     * @param string Input string (should be substituted before call).
     *      Null string will be interpreted as OK
     * @throws ParameterSubstitutionException Substitution error
     */
    public static void checkString(@CheckForNull String string) throws ParameterSubstitutionException {
        if (string == null) {
            return;
        }

        // Conditional fail on substitution error
        if (true && string.matches(".*\\$\\{.*\\}.*")) {
            throw new ParameterSubstitutionException(string, "Found unresolved macro at '" + string + "'");
        }

        //TODO: manage validation by global params?
        //TODO: Check single brackets
        //TODO: Add checks for '$' without brackets 
    }

    /**
     * Substitute parameters and validate contents of the resulting string
     * @param string Input string
     * @param subst Variables Map
     * @return Substituted string
     */
    private static String substituteParametersNoCheck(
            @CheckForNull String string, 
            @Nonnull Map<String, String> subst) {
        if (string == null) {
            return null;
        }
        String newString = string;
        for (Map.Entry<String, String> entry : subst.entrySet()) {
            newString = newString.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return newString;
    }
    
    public static boolean containsMacro(@CheckForNull String str) {
        return str != null && str.contains("${");
    }
  
     /**
     * Substitute parameters and validate contents of the resulting string
     * @param inputString Input string to be substituted
     * @param project A project
     * @param instance Instance of {@link PerforceSCM}
     * @param node A node to be substituted
     * @param env Additional environment variables.
    *  @return Substituted string
     */        
    private static String substituteParametersNoCheck (
            @Nonnull String inputString,
            @CheckForNull PerforceSCM instance,
            @CheckForNull AbstractProject project,
            @CheckForNull Node node,
            @CheckForNull Map<String, String> env) {
        
        if (!containsMacro(inputString)) { // do nothing for the missing macro
            return inputString;
        }
        String outputString = inputString;
        
        // Substitute additional environment vars if possible
        if (env != null && !env.isEmpty()) {
            outputString = substituteParametersNoCheck(outputString, env);
            if (!containsMacro(outputString)) { //exit if no macros left
                return outputString;
            }
        }
        
        // Substitute global environment variables
        for (NodeProperty nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                outputString = ((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars().expand(outputString);
            }
        }
        
        // Substitute node variables if possible
        if (node != null) {
            for (NodeProperty nodeProperty : node.getNodeProperties()) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    outputString = ((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars().expand(outputString);
                }
            }
        }
        if (!containsMacro(outputString)) {
            return outputString;
        }
        
        // Substitute project variables
        Hashtable<String, String> substitutions = new Hashtable<String, String>();
        if (project != null) { 
            getDefaultSubstitutions(project, substitutions);
        }
        
        // Add instance variables
        if (instance != null) {
            getDefaultSubstitutions(instance, substitutions);
        }
        outputString = substituteParametersNoCheck(outputString, substitutions);    
        
        return outputString;
    }
    
    /**
     * Substitute parameters and validate contents of the resulting string
     * @param inputString Input string
     * @param instance Instance of {@link PerforceSCM}
     * @param build Related build
     * @param env Additional environment variables
     * @return Substituted string
     */        
    private static String substituteParametersNoCheck(
            @CheckForNull String inputString,
            @Nonnull PerforceSCM instance,
            @Nonnull AbstractBuild build, 
            @CheckForNull Map<String, String> env) {
        
        if (!containsMacro(inputString)) {
            return inputString;
        }
             
        String string = substituteParametersNoCheck(inputString, null, build.getProject(), build.getBuiltOn(), env);
              
        // Substitute default build variables
        Hashtable<String, String> substitutions = new Hashtable<String, String>();
        getDefaultBuildSubstitutions(build, substitutions);    
        getDefaultSubstitutions(instance, substitutions);
        String result = MacroStringHelper.substituteParametersNoCheck(string, substitutions);
        result = MacroStringHelper.substituteParametersNoCheck(result, build.getBuildVariables());
        if (!containsMacro(string)) {
            return string;
        }
        
         // The last attempts: Try to build the full environment
        Map<String, String> environmentVarsFromExtensions = new TreeMap<String, String>();
        boolean useEnvironment = true;
        for (StackTraceElement ste : (new Throwable()).getStackTrace()) { // Inspect the stacktrace to avoid the infinite recursion
            if (ste.getMethodName().equals("buildEnvVars") && ste.getClassName().equals(PerforceSCM.class.getName())) {
                useEnvironment = false;
            }
        }
        if (useEnvironment) {
            try {
                EnvVars vars = build.getEnvironment(TaskListener.NULL);
                environmentVarsFromExtensions.putAll(vars);
            } catch (IOException ex) {
                Logger.getLogger(PerforceSCM.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InterruptedException ex) {
                Logger.getLogger(PerforceSCM.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        result = MacroStringHelper.substituteParametersNoCheck(result, environmentVarsFromExtensions);
        
        return result;
    }
    
    private static String getSafeJobName(@Nonnull AbstractBuild build) {
        return getSafeJobName(build.getProject());
    }

    private static String getSafeJobName(@Nonnull AbstractProject project) {
        return project.getFullName().replace('/', '-').replace('=', '-').replace(',', '-');
    }
    
    private static void getDefaultSubstitutions(@CheckForNull PerforceSCM instance, 
            @Nonnull Hashtable<String, String> subst) {
        subst.put("P4USER", MacroStringHelper.substituteParametersNoCheck(instance.getP4User(), subst));
    }
    
    private static void getDefaultBuildSubstitutions(@CheckForNull AbstractBuild build, 
            @Nonnull Hashtable<String, String> subst) {
        subst.put("JOB_NAME", getSafeJobName(build));
        String hudsonName = Hudson.getInstance().getDisplayName().toLowerCase();
        subst.put("BUILD_TAG", hudsonName + "-" + build.getProject().getName() + "-" + String.valueOf(build.getNumber()));
        subst.put("BUILD_ID", build.getId());
        subst.put("BUILD_NUMBER", String.valueOf(build.getNumber()));
    }
        
    /**
     * Gets default variable substitutions for the {@link Node}.
     * @param node Target node. Can be null
     * @param target Output collection
     */
    private static void getDefaultNodeSubstitutions(@CheckForNull Node node, 
            @Nonnull Hashtable<String, String> target) {
        // Global node properties
        for (NodeProperty globalNodeProperty: Hudson.getInstance().getGlobalNodeProperties()) {
            if (globalNodeProperty instanceof EnvironmentVariablesNodeProperty) {
                target.putAll(((EnvironmentVariablesNodeProperty)globalNodeProperty).getEnvVars());
            }
        }
        
        // Local node properties
        if (node != null) {
            for (NodeProperty nodeProperty : node.getNodeProperties()) {
                if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                    target.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
                }
            }
        }
    }
    
    private static void getDefaultSubstitutions(
            @Nonnull AbstractProject project, @Nonnull Hashtable<String, String> subst) {
        subst.put("JOB_NAME", MacroStringHelper.getSafeJobName(project));
        for (NodeProperty nodeProperty : Hudson.getInstance().getGlobalNodeProperties()) {
            if (nodeProperty instanceof EnvironmentVariablesNodeProperty) {
                subst.putAll(((EnvironmentVariablesNodeProperty) nodeProperty).getEnvVars());
            }
        }
        ParametersDefinitionProperty pdp = (ParametersDefinitionProperty) project.getProperty(hudson.model.ParametersDefinitionProperty.class);
        if (pdp != null) {
            for (ParameterDefinition pd : pdp.getParameterDefinitions()) {
                try {
                    ParameterValue defaultValue = pd.getDefaultParameterValue();
                    if (defaultValue != null) {
                        String name = defaultValue.getName();
                        String value = defaultValue.createVariableResolver(null).resolve(name);
                        subst.put(name, value);
                    }
                } catch (Exception e) {
                    // Do nothing
                }
            }
        }

    }

}
