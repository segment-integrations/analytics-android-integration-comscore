package com.segment.analytics.android.integrations.comscore;

import com.comscore.analytics.comScore;
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

  private static final String COMSCORE_KEY = "ComScore";
  final Logger logger;
  String customerC2;
  String publisherSecret;
  String appName;
  boolean useHTTPS;
  int autoUpdateInterval;
  String autoUpdateMode;

  ComScoreIntegration(Analytics analytics, ValueMap settings) {
    logger = analytics.logger(COMSCORE_KEY);
    customerC2 = settings.getString("customerC2");
    publisherSecret = settings.getString("publisherSecret");
    appName = settings.getString("appName");
    useHTTPS = settings.getBoolean("useHTTPS", true);
    autoUpdateInterval = settings.getInt("autoUpdateInterval", 60);
    autoUpdateMode = settings.getString("autoUpdateMode");

    comScore.setAppContext(analytics.getApplication());
    logger.verbose("comScore.setAppContext(analytics.getApplication())");
    comScore.setCustomerC2(customerC2);
    comScore.setPublisherSecret(publisherSecret);
    comScore.setAppName(appName);
    comScore.setSecure(useHTTPS);
    if (autoUpdateMode != null) {
      if (autoUpdateMode.equals("foreground")) {
        comScore.enableAutoUpdate(autoUpdateInterval, true);
      } else if (autoUpdateMode.equals("background")) {
        comScore.enableAutoUpdate(autoUpdateInterval, false);
      } else {
        comScore.disableAutoUpdate();
      }
    }
  }

  @Override public void track(TrackPayload track) {
    String name = track.event();
    HashMap<String, String> properties = (HashMap<String, String>) track.properties().toStringMap();
    properties.put("name", name);
    comScore.hidden(properties);
    logger.verbose("comScore.hidden(%s)", properties);
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    comScore.setLabels(traits);
    logger.verbose("comScore.setLabels(%s)", traits);
  }

  @Override public void screen(ScreenPayload screen) {
    String name = screen.name();
    String category = screen.category();
    HashMap<String, String> properties = (HashMap<String, String>) //
        screen.properties().toStringMap();
    properties.put("name", name);
    properties.put("category", category);

    comScore.view(properties);
    logger.verbose("comScore.hidden(%s)", properties);
  }

}
