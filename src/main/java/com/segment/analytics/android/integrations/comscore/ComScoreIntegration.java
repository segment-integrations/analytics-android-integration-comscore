package com.segment.analytics.android.integrations.comscore;

import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import java.util.HashMap;

public class ComScoreIntegration extends Integration<comScore> {
  public static final Factory FACTORY = new Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      return new ComScoreIntegration(analytics, settings);
    }

    @Override public String key() {
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

  ComScoreIntegration(Analytics analytics, ValueMap settings) {
    logger = analytics.logger(COMSCORE_KEY);
    customerC2 = settings.getString("c2");
    publisherSecret = settings.getString("publisherSecret");
    appName = settings.getString("appName");
    useHTTPS = settings.getBoolean("useHTTPS", true);
    autoUpdateInterval = settings.getInt("autoUpdateInterval", 60);
    autoUpdate = settings.getBoolean("autoUpdate", false);
    foregroundOnly = settings.getBoolean("foregroundOnly", true);




    PublisherConfiguration myPublisherConfig = new PublisherConfiguration.Builder()
            .publisherId(customerC2)
            .publisherSecret(publisherSecret)
            .usagePropertiesAutoUpdateInterval(autoUpdateInterval)
            // how do I set usagePropertiesAutoUpdateMode to ForegroundOnly, ForegroundAndBackground, or Disabled
            .usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.DISABLED)
            .build();
    com.comscore.Analytics.getConfiguration().addClient(myPublisherConfig);
    // unsure as to why getApplicationContext isn't found
    com.comscore.Analytics.start(this.getApplicationContext());
  }

  @Override public void track(TrackPayload track) {
    String name = track.event();
    HashMap<String, String> properties = (HashMap<String, String>) track.properties().toStringMap();
    properties.put("name", name);
    com.comscore.Analytics.notifyHiddenEvent(properties);
    // comScore.hidden(properties);
    logger.verbose("comScore.hidden(%s)", properties);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    com.comscore.Analytics.getConfiguration().setPersistentLabels(traits);
    logger.verbose("comScore.setPersistentLabels(%s)", traits);
  }

  @Override public void screen(ScreenPayload screen) {
    String name = screen.name();
    String category = screen.category();
    HashMap<String, String> properties = (HashMap<String, String>) //
        screen.properties().toStringMap();
    properties.put("name", name);
    properties.put("category", category);

    // comScore.view(properties);
    com.comscore.Analytics.notifyViewEvent(properties);
    logger.verbose("comScore.notifyViewEvent(%s)", properties);
  }

}
