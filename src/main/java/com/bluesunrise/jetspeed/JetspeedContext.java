package com.bluesunrise.jetspeed;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.jetspeed.Jetspeed;
import org.apache.jetspeed.components.ComponentManager;
import org.apache.jetspeed.components.JetspeedBeanDefinitionFilter;
import org.apache.jetspeed.components.SpringComponentManager;
import org.apache.jetspeed.components.factorybeans.ServletConfigFactoryBean;
import org.apache.jetspeed.components.jndi.JetspeedTestJNDIComponent;
import org.apache.jetspeed.engine.JetspeedEngine;
import org.apache.jetspeed.engine.JetspeedEngineConstants;
import org.apache.jetspeed.security.User;
import org.apache.jetspeed.security.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;

import javax.security.auth.Subject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created by rwatler on 4/11/14.
 */
public class JetspeedContext {

    private static final Logger log = LoggerFactory.getLogger(JetspeedContext.class);

    public static final String DATABASE_URL_PROP_NAME = "org.apache.jetspeed.database.url";
    public static final String DATABASE_DRIVER_PROP_NAME = "org.apache.jetspeed.database.driver";
    public static final String DATABASE_USER_PROP_NAME = "org.apache.jetspeed.database.user";
    public static final String DATABASE_PASSWORD_PROP_NAME = "org.apache.jetspeed.database.password";

    private File appRoot;
    private JetspeedTestJNDIComponent jndiDS;
    private JetspeedEngine engine;

    public JetspeedContext(String springFilter, Properties initProperties) throws Exception {
        // create jetspeed componenents
        JetspeedBeanDefinitionFilter beanFilter = new JetspeedBeanDefinitionFilter("/spring-filter.properties", springFilter);
        String [] bootConfigurations = new String[]{"/assembly/boot/datasource.xml"};
        String [] configurations = new String[]{"/assembly/*.xml"};
        this.appRoot = setupAppRoot();
        String appRootPath = this.appRoot.getAbsolutePath();
        PropertiesConfiguration config = loadConfig(initProperties);
        config.setProperty(JetspeedEngineConstants.SPRING_FILTER_KEY, springFilter);
        config.setProperty(JetspeedEngineConstants.APPLICATION_ROOT_KEY, appRootPath);
        SpringComponentManager cm = new SpringComponentManager(beanFilter, bootConfigurations, configurations, appRootPath, initProperties, false);
        cm.addComponent("ProductionConfiguration", config);
        // create jetspeed engine
        ServletConfig servletConfig = new JetspeedContextServletConfig();
        ServletConfigFactoryBean.setServletConfig(servletConfig);
        engine = new JetspeedEngine(config, appRootPath, servletConfig, cm);
        Jetspeed.setEngine(engine);
        // setup JNDI database resource
        System.setProperty(DATABASE_URL_PROP_NAME, initProperties.getProperty(DATABASE_URL_PROP_NAME));
        System.setProperty(DATABASE_DRIVER_PROP_NAME, initProperties.getProperty(DATABASE_DRIVER_PROP_NAME));
        System.setProperty(DATABASE_USER_PROP_NAME, initProperties.getProperty(DATABASE_USER_PROP_NAME));
        System.setProperty(DATABASE_PASSWORD_PROP_NAME, initProperties.getProperty(DATABASE_PASSWORD_PROP_NAME));
        this.jndiDS = new JetspeedTestJNDIComponent();
    }

    public void start() throws Exception {
        jndiDS.setup();
        engine.start();
    }

    public Map<String,Object> getComponents() throws Exception {
        Map<String,Object> components = new TreeMap<String,Object>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.toLowerCase().compareTo(o2.toLowerCase());
            }
        });
        SpringComponentManager cm = (SpringComponentManager)engine.getComponentManager();
        ApplicationContext applicationContext = cm.getApplicationContext();
        do {
            ConfigurableListableBeanFactory beanFactory = ((AbstractApplicationContext)applicationContext).getBeanFactory();
            for (String componentName : beanFactory.getSingletonNames()) {
                if (!componentName.startsWith("_")) {
                    String shortComponentName = componentName;
                    int packageSeparator = shortComponentName.lastIndexOf('.');
                    if (packageSeparator >= 0) {
                        shortComponentName = shortComponentName.substring(packageSeparator+1);
                    }
                    shortComponentName = shortComponentName.replaceAll("[#]", "");
                    if (components.containsKey(shortComponentName)) {
                        int i = 2;
                        while (components.containsKey(shortComponentName+i)) {
                            i++;
                        }
                        shortComponentName = shortComponentName+1;
                    }
                    Object component = beanFactory.getSingleton(componentName);
                    if (component instanceof FactoryBean) {
                        FactoryBean factoryBean = (FactoryBean)component;
                        if (factoryBean.isSingleton()) {
                            component = factoryBean.getObject();
                        }
                    }
                    components.put(shortComponentName, component);
                }
            }
            applicationContext = applicationContext.getParent();
        } while (applicationContext != null);
        return components;
    }

    public Subject getUserSubject(String userName) {
        try {
            ComponentManager cm = engine.getComponentManager();
            UserManager userManager = (UserManager)cm.getComponent(UserManager.class.getName());
            if (userManager != null) {
                User user = userManager.getUser(userName);
                return userManager.getSubject(user);
            }
        } catch (Exception e) {
            log.error("Unable to lookup user: "+userName+", "+e);
        }
        return null;
    }

    public void stop() throws Exception {
        try {
            engine.shutdown();
            jndiDS.tearDown();
        } finally {
            FileUtils.deleteDirectory(appRoot);
        }
    }

    private static File setupAppRoot() throws IOException {
        File appRoot = File.createTempFile("jetspeed-script-", Long.toString(System.nanoTime()));
        appRoot.delete();
        appRoot.mkdir();
        (new File(appRoot, "decorations")).mkdir();
        File webInfRoot = new File(appRoot, "WEB-INF");
        webInfRoot.mkdir();
        (new File(webInfRoot, "templates")).mkdir();
        return appRoot;
    }

    private static PropertiesConfiguration loadConfig(Properties initProperties) throws ConfigurationException {
        PropertiesConfiguration config = new PropertiesConfiguration();
        config.load(JetspeedContext.class.getResourceAsStream("/jetspeed.properties"));
        PropertiesConfiguration overrides = new PropertiesConfiguration();
        overrides.load(JetspeedContext.class.getResourceAsStream("/override.properties"));
        ConfigurationUtils.copy(overrides, config);
        for (Map.Entry<?,?> initPropertyEntry : initProperties.entrySet()) {
            config.setProperty(initPropertyEntry.getKey().toString(), initPropertyEntry.getValue());
        }
        return config;
    }

    public static class JetspeedContextServletConfig implements ServletConfig {

        @Override
        public String getServletName() {
            return null;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration getInitParameterNames() {
            return null;
        }
    }
}
