package com.segment.analytics.android.integrations.comscore;

import com.comscore.Analytics;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.comscore.PartnerConfiguration;
import java.util.HashMap;

public class ComScoreIntegration extends Integration<Void> {
  public static final Factory FACTORY =
      new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, com.segment.analytics.Analytics analytics) {
          return new ComScoreIntegration(analytics, settings);
        }

        @Override
        public String key() {
          return COMSCORE_KEY;
        }
      };

  private static final String COMSCORE_KEY = "comScore";
  final Logger logger;
  String customerC2;
  String publisherSecret;
  String appName;
  boolean useHTTPS;
  int autoUpdateInterval;
  boolean autoUpdate;
  boolean foregroundOnly;

  ComScoreIntegration(com.segment.analytics.Analytics analytics, ValueMap settings) {
    logger = analytics.logger(COMSCORE_KEY);
    customerC2 = settings.getString("c2");
    publisherSecret = settings.getString("publisherSecret");
    appName = settings.getString("appName");
    useHTTPS = settings.getBoolean("useHTTPS", true);
    autoUpdateInterval = settings.getInt("autoUpdateInterval", 60);
    autoUpdate = settings.getBoolean("autoUpdate", false);
    foregroundOnly = settings.getBoolean("foregroundOnly", true);

    PublisherConfiguration.Builder builder = new PublisherConfiguration.Builder();
    builder.publisherId(customerC2);
    builder.publisherSecret(publisherSecret);
    if (appName != null && appName.trim().length() != 0) {
      builder.applicationName(appName);
    }
    builder.secureTransmission(useHTTPS);
    builder.usagePropertiesAutoUpdateInterval(autoUpdateInterval);

    if (autoUpdate) {
      builder.usagePropertiesAutoUpdateMode(
          UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND);
    } else if (foregroundOnly) {
      builder.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.FOREGROUND_ONLY);
    } else {
      builder.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED);
    }

    PartnerConfiguration partnerConfig = new PartnerConfiguration.Builder()
        .partnerId("24186693")
        .build();
    PublisherConfiguration myPublisherConfig = builder.build();

    Analytics.getConfiguration().addClient(partnerConfig);
    Analytics.getConfiguration().addClient(myPublisherConfig);
    Analytics.start(analytics.getApplication());
  }

  @Override
  public void track(TrackPayload track) {
    String name = track.event();
    HashMap<String, String> properties = (HashMap<String, String>) track.properties().toStringMap();
    properties.put("name", name);
    Analytics.notifyHiddenEvent(properties);
    logger.verbose("Analytics.hidden(%s)", properties);
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    Analytics.getConfiguration().setPersistentLabels(traits);
    logger.verbose("Analytics.setPersistentLabels(%s)", traits);
  }

  @Override
  public void screen(ScreenPayload screen) {
    String name = screen.name();
    String category = screen.category();
    HashMap<String, String> properties =
        (HashMap<String, String>) //
            screen.properties().toStringMap();
    properties.put("name", name);
    properties.put("category", category);
    Analytics.notifyViewEvent(properties);
    logger.verbose("Analytics.notifyViewEvent(%s)", properties);
  }
}
