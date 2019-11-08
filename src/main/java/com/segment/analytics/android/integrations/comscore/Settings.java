package com.segment.analytics.android.integrations.comscore;

import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.segment.analytics.ValueMap;

/**
 * Encapsulates all settings required to initialize the ComsCore destination.
 */
public class Settings {

    private final static int DEFAULT_INTERVAL = 60;
    private final static boolean DEFAULT_HTTPS = true;
    private final static boolean DEFAULT_AUTOUPDATE = false;
    private final static boolean DEFAULT_FOREGROUND = true;

    private String c2;
    private String appName;
    private String publisherSecret;
    private boolean autoUpdate;
    private int autoUpdateInterval;
    private boolean useHTTPS;
    private boolean foregroundOnly;

    /**
     * Creates the settings from the provided map.
     * @param destinationSettings Destination settings
     */
    public Settings(ValueMap destinationSettings) {
        this.c2 = destinationSettings.getString("c2");
        this.publisherSecret = destinationSettings.getString("publisherSecret");
        this.autoUpdateInterval = destinationSettings.getInt("autoUpdateInterval", DEFAULT_INTERVAL);
        this.autoUpdate = destinationSettings.getBoolean("autoUpdate", DEFAULT_AUTOUPDATE);
        this.foregroundOnly = destinationSettings.getBoolean("foregroundOnly", DEFAULT_FOREGROUND);
        this.useHTTPS = destinationSettings.getBoolean("useHTTPS", DEFAULT_HTTPS);
        this.appName = destinationSettings.getString("appName");

        if (appName != null && appName.trim().length() == 0) {
            // Application name as null
            appName = null;
        }
    }

    /**
     * Retrieves the customerId (a.k.a customerC2, publisherId, c2).
     * @return Customer Id.
     */
    public String getCustomerId() {
        return c2;
    }

    /**
     * Retrieves the application name for this instance.
     * @return Application name.
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Retrieves the publisher secret.
     * @return Publisher secret.
     */
    public String getPublisherSecret() {
        return publisherSecret;
    }

    /**
     * Retrieves if the usage properties must autoupdate.
     * @return <code>true</code> if the usage properties will auto-update. <code>false</code> otherwise.
     */
    public boolean isAutoUpdate() {
        return autoUpdate;
    }

    /**
     * Retrieves the interval for auto-update.
     * @return Interval in seconds.
     */
    public int getAutoUpdateInterval() {
        return autoUpdateInterval;
    }

    /**
     * Retrieves if the comscore install will use HTTPS.
     * @return <code>true</code> if HTTPS is enabled. <code>false</code> otherwise.
     */
    public boolean isUseHTTPS() {
        return useHTTPS;
    }

    /**
     * Retrieves if usage tracking is enabled when the application is in foreground.
     * @return <code>true</code> if foreground only tracking is enabled. <code>false</code> otherwise.
     */
    public boolean isForegroundOnly() {
        return foregroundOnly;
    }

    /**
     * Creates the publisher configuration with the specified settings.
     * @return Publisher configuration for ComScore.
     */
    public PublisherConfiguration toPublisherConfiguration() {
        PublisherConfiguration.Builder publisher = new PublisherConfiguration.Builder();
        publisher.publisherId(c2);
        publisher.publisherSecret(publisherSecret);
        if (appName != null) {
            publisher.applicationName(appName);
        }
        publisher.secureTransmission(useHTTPS);
        publisher.usagePropertiesAutoUpdateInterval(autoUpdateInterval);

        if (autoUpdate) {
            publisher.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND);
        } else if (foregroundOnly) {
            publisher.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.FOREGROUND_ONLY);
        } else {
            publisher.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED);
        }

        return publisher.build();
    }
}
