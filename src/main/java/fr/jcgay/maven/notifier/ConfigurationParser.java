package fr.jcgay.maven.notifier;

import com.google.common.annotations.VisibleForTesting;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.GROWL_HOST;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.GROWL_PASSWORD;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.GROWL_PORT;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.IMPLEMENTATION;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFICATION_CENTER_ACTIVATE;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFICATION_CENTER_PATH;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFICATION_CENTER_SOUND;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFY_SEND_PATH;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFY_SEND_TIMEOUT;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.NOTIFY_WITH;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.PUSHBULLET_API_KEY;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.PUSHBULLET_DEVICE;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.SHORT_DESCRIPTION;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.SNARL_HOST;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.SNARL_PASSWORD;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.SNARL_PORT;
import static fr.jcgay.maven.notifier.ConfigurationParser.ConfigurationProperties.Property.SYSTEM_TRAY_WAIT;
import static java.lang.Boolean.parseBoolean;

@Component(role = ConfigurationParser.class, hint = "maven-notifier-configuration")
public class ConfigurationParser {

    @Requirement
    private Logger logger;

    public Configuration get() {
        URL url = getConfigurationUrl();

        if (url == null) {
            return defaultConfiguration();
        }

        try {
            return get(readProperties(url));
        } catch (IOException e) {
            logger.debug("Cannot read default configuration file: " + url, e);
            return defaultConfiguration();
        }
    }

    public static Properties readProperties() {
        try {
            return readProperties(configurationFile());
        } catch (IOException e) {
            return new Properties();
        }
    }

    @VisibleForTesting
    static Properties readProperties(URL url) throws IOException {
        Properties properties = new Properties();
        properties.load(url.openStream());
        String overrideImplementation = System.getProperty(NOTIFY_WITH.key());
        if (overrideImplementation != null) {
            properties.put(IMPLEMENTATION.key(), overrideImplementation);
        }
        return properties;
    }

    private URL getConfigurationUrl() {
        try {
            return configurationFile();
        } catch (MalformedURLException e) {
            logger.debug("Cannot create URL for default configuration file.", e);
            return null;
        }
    }

    private static URL configurationFile() throws MalformedURLException {
        return new URL(ConfigurationParser.class.getProtectionDomain().getCodeSource().getLocation(), "maven-notifier.properties");
    }

    @VisibleForTesting Configuration get(Properties properties) {
        Configuration configuration = parse(new ConfigurationProperties(properties));
        logger.debug("Notifier will use configuration: " + configuration);
        return configuration;
    }

    private Configuration parse(ConfigurationProperties properties) {
        Configuration configuration = new Configuration();
        configuration.setImplementation(properties.get(IMPLEMENTATION));
        configuration.setNotifySendPath(properties.get(NOTIFY_SEND_PATH));
        configuration.setNotifySendTimeout(properties.get(NOTIFY_SEND_TIMEOUT));
        configuration.setNotificationCenterPath(properties.get(NOTIFICATION_CENTER_PATH));
        configuration.setNotificationCenterActivate(properties.get(NOTIFICATION_CENTER_ACTIVATE));
        configuration.setNotificationCenterSound(properties.get(NOTIFICATION_CENTER_SOUND));
        configuration.setGrowlHost(properties.get(GROWL_HOST));
        configuration.setGrowlPort(properties.get(GROWL_PORT));
        configuration.setGrowlPassword(properties.get(GROWL_PASSWORD));
        configuration.setSystemTrayWaitBeforeEnd(properties.get(SYSTEM_TRAY_WAIT));
        configuration.setSnarlPort(properties.get(SNARL_PORT));
        configuration.setSnarlHost(properties.get(SNARL_HOST));
        configuration.setSnarlPassword(properties.get(SNARL_PASSWORD));
        configuration.setShortDescription(parseBoolean(properties.get(SHORT_DESCRIPTION)));
        configuration.setPushbulletKey(properties.get(PUSHBULLET_API_KEY));
        configuration.setPushbulletDevice(properties.get(PUSHBULLET_DEVICE));
        return configuration;
    }

    private Configuration defaultConfiguration() {
        return get(new Properties());
    }

    public static class ConfigurationProperties {

        private static final String OS_NAME = "os.name";

        private final Properties properties;

        private ConfigurationProperties(Properties properties) {
            this.properties = properties;
            if (currentOs() == null) {
                properties.put(OS_NAME, System.getProperty(OS_NAME));
            }
        }

        public String get(Property property) {
            switch (property) {
                case IMPLEMENTATION:
                    return properties.getProperty(property.key(), defaultImplementation());
                default:
                    return properties.getProperty(property.key(), property.defaultValue());
            }
        }

        public String currentOs() {
            return properties.getProperty(OS_NAME);
        }

        private String defaultImplementation() {
            String os = currentOs().toLowerCase();
            if (isMacos(os) || isWindows(os)) {
                return "growl";
            }
            return "notifysend";
        }

        private boolean isMacos(String os) {
            return os.contains("mac");
        }

        private boolean isWindows(String os) {
            return os.contains("win");
        }

        public static enum Property {
            IMPLEMENTATION("notifier.implementation"),
            NOTIFY_SEND_PATH("notifier.notify-send.path", "notify-send"),
            NOTIFY_SEND_TIMEOUT("notifier.notify-send.timeout", String.valueOf(TimeUnit.SECONDS.toMillis(2))),
            NOTIFICATION_CENTER_PATH("notifier.notification-center.path", "terminal-notifier"),
            NOTIFICATION_CENTER_ACTIVATE("notifier.notification-center.activate", "com.apple.Terminal"),
            GROWL_PORT("notifier.growl.port", String.valueOf(23053)),
            GROWL_HOST("notifier.growl.host"),
            GROWL_PASSWORD("notifier.growl.password"),
            SYSTEM_TRAY_WAIT("notifier.system-tray.wait", String.valueOf(TimeUnit.SECONDS.toMillis(2))),
            SNARL_PORT("notifier.snarl.port", String.valueOf(9887)),
            SNARL_HOST("notifier.snarl.host", "localhost"),
            SNARL_PASSWORD("notifier.snarl.password"),
            NOTIFICATION_CENTER_SOUND("notifier.notification-center.sound"),
            SHORT_DESCRIPTION("notifier.message.short", "false"),
            PUSHBULLET_API_KEY("notifier.pushbullet.apikey"),
            PUSHBULLET_DEVICE("notifier.pushbullet.device"),
            NOTIFY_WITH("notifyWith");

            private final String key;
            private String defaultValue;

            private Property(String key) {
                this.key = key;
            }

            private Property(String key, String defaultValue) {
                this.key = key;
                this.defaultValue = defaultValue;
            }

            public String key() {
                return key;
            }

            public String defaultValue() {
                return defaultValue;
            }
        }
    }
}
