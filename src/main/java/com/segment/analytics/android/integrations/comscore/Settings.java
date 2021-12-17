package com.segment.analytics.android.integrations.comscore;

import com.comscore.Analytics;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.segment.analytics.ValueMap;

import java.util.HashMap;

/** Encapsulates all settings required to initialize the ComsCore destination. */
public class Settings {

  private static final int DEFAULT_INTERVAL = 60;
  private static final boolean DEFAULT_HTTPS = true;
  private static final boolean DEFAULT_AUTOUPDATE = false;
  private static final boolean DEFAULT_FOREGROUND = true;

  private String c2;
  private String appName;
  private String publisherSecret;
  private boolean autoUpdate;
  private int autoUpdateInterval;
  private boolean useHTTPS;
  private boolean foregroundOnly;
  private String consentFlagProp; // Consent Flag change

  /**
   * Creates the settings from the provided map.
   *
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
    this.consentFlagProp = destinationSettings.getString("consentFlag"); // Consent Flag change


    if (appName != null && appName.trim().length() == 0) {
      // Application name as null
      appName = null;
    }
  }

  /**
   * Retrieves the customerId (a.k.a customerC2, publisherId, c2).
   *
   * @return Customer Id.
   */
  public String getCustomerId() {
    return c2;
  }

  /**
   * Retrieves the application name for this instance.
   *
   * @return Application name.
   */
  public String getAppName() {
    return appName;
  }

  /**
   * Retrieves the publisher secret.
   *
   * @return Publisher secret.
   */
  public String getPublisherSecret() {
    return publisherSecret;
  }

  /**
   * Retrieves if the usage properties must autoupdate.
   *
   * @return <code>true</code> if the usage properties will auto-update. <code>false</code>
   *     otherwise.
   */
  public boolean isAutoUpdate() {
    return autoUpdate;
  }

  /**
   * Retrieves the interval for auto-update.
   *
   * @return Interval in seconds.
   */
  public int getAutoUpdateInterval() {
    return autoUpdateInterval;
  }

  /**
   * Retrieves if the comscore install will use HTTPS.
   *
   * @return <code>true</code> if HTTPS is enabled. <code>false</code> otherwise.
   */
  public boolean isUseHTTPS() {
    return useHTTPS;
  }

  /**
   * Retrieves if usage tracking is enabled when the application is in foreground.
   *
   * @return <code>true</code> if foreground only tracking is enabled. <code>false</code> otherwise.
   */
  public boolean isForegroundOnly() {
    return foregroundOnly;
  }

  /**
   * Retrieves mapped consent flag property
   *
   * @return <code>string</code> mapped property name.
   */
  public String getConsentFlagProp() {
    return consentFlagProp;
  }

  public HashMap<String,String> setConsentFlag() {
    HashMap<String, String> consentFlag = new HashMap<String,String>();
    consentFlag.put("cs_ucfr", "" );
    return consentFlag;
  }
  /**
   * Creates the publisher configuration with the specified settings.
   *
   * @return Publisher configuration for ComScore.
   */
  public PublisherConfiguration toPublisherConfiguration() {
    PublisherConfiguration.Builder publisher = new PublisherConfiguration.Builder();
    publisher.publisherId(c2);
    publisher.secureTransmission(useHTTPS);
    publisher.persistentLabels(setConsentFlag());
    return publisher.build();
  }

  public void analyticsConfig() {
    Analytics.getConfiguration().setUsagePropertiesAutoUpdateInterval(autoUpdateInterval);

    if (appName != null) {
      Analytics.getConfiguration().setApplicationName(appName);
    }
    if (autoUpdate) {
      Analytics.getConfiguration()
          .setUsagePropertiesAutoUpdateMode(
              UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND);
    } else if (foregroundOnly) {
      Analytics.getConfiguration()
          .setUsagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.FOREGROUND_ONLY);
    } else {
      Analytics.getConfiguration()
          .setUsagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED);
      Analytics.getConfiguration()
          .setUsagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED);
    }
  }
}
