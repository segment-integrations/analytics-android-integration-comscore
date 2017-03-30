package com.segment.analytics.android.integrations.comscore;

import com.comscore.Analytics;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.streaming.StreamingAnalytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import com.comscore.PartnerConfiguration;
import com.segment.analytics.internal.Utils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComScoreIntegration extends Integration<Void> {

  public static final Factory FACTORY =
      new Factory() {
        @Override
        public Integration<?> create(ValueMap settings, com.segment.analytics.Analytics analytics) {
          return new ComScoreIntegration(analytics, settings, StreamingAnalyticsFactory.REAL);
        }

        @Override
        public String key() {
          return COMSCORE_KEY;
        }
      };

  private static final String COMSCORE_KEY = "comScore";
  final Logger logger;
  final StreamingAnalyticsFactory streamingAnalyticsFactory;
  final String customerC2;
  final String publisherSecret;
  final String appName;
  final boolean useHTTPS;
  final int autoUpdateInterval;
  final boolean autoUpdate;
  final boolean foregroundOnly;
  private StreamingAnalytics streamingAnalytics;

  interface StreamingAnalyticsFactory {
    StreamingAnalytics create();

    StreamingAnalyticsFactory REAL =
        new StreamingAnalyticsFactory() {
          @Override
          public StreamingAnalytics create() {
            return new StreamingAnalytics();
          }
        };
  }

  ComScoreIntegration(
      com.segment.analytics.Analytics analytics,
      ValueMap settings,
      StreamingAnalyticsFactory streamingAnalyticsFactory) {
    this.streamingAnalyticsFactory = streamingAnalyticsFactory;
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

    PartnerConfiguration partnerConfig =
        new PartnerConfiguration.Builder().partnerId("24186693").build();
    PublisherConfiguration myPublisherConfig = builder.build();

    Analytics.getConfiguration().addClient(partnerConfig);
    Analytics.getConfiguration().addClient(myPublisherConfig);
    Analytics.start(analytics.getApplication());
  }

  @Override
  public void track(TrackPayload track) {
    String name = track.event();
    Map<String, String> properties = track.properties().toStringMap();

    Map<String, String> playbackMapper = new LinkedHashMap<>();
    playbackMapper.put("asset_id", "ns_st_ci");
    playbackMapper.put("ad_type", "ns_st_ad");
    playbackMapper.put("length", "nst_st_cl");
    playbackMapper.put("video_player", "ns_st_st");

    Map<String, String> contentMapper = new LinkedHashMap<>();
    contentMapper.put("asset_id", "ns_st_ci");
    contentMapper.put("title", "ns_st_pr");
    contentMapper.put("keywords", "ns_st_ge");
    contentMapper.put("season", "ns_st_sn");
    contentMapper.put("episode", "ns_st_ep");
    contentMapper.put("genre", "ns_st_ge");
    contentMapper.put("program", "ns_st_pr");
    contentMapper.put("channel", "ns_st_pu");
    contentMapper.put("full_episode", "ns_st_ce");
    contentMapper.put("airdate", "ns_st_ddt");

    Map<String, String> adMapper = new LinkedHashMap<>();
    adMapper.put("asset_id", "ns_st_ci");
    adMapper.put("pod_id", "ns_st_pn");
    adMapper.put("type", "ns_st_ad");
    adMapper.put("publisher", "ns_st_pu");
    adMapper.put("length", "ns_st_cl");

    Map<String, String> playbackAsset = Utils.transform(properties, playbackMapper);
    Map<String, String> contentAsset = Utils.transform(properties, contentMapper);
    Map<String, String> adAsset = Utils.transform(properties, adMapper);

    switch (name) {
      case "Video Playback Started":
        streamingAnalytics = streamingAnalyticsFactory.create();
        streamingAnalytics.createPlaybackSession();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Paused":
        streamingAnalytics.notifyPause();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Buffer Started":
        streamingAnalytics.notifyBufferStart();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Buffer Completed":
        streamingAnalytics.notifyBufferStop();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Seek Started":
        streamingAnalytics.notifySeekStart();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Seek Completed":
        streamingAnalytics.notifyEnd();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Playback Resumed":
        streamingAnalytics.notifyPlay();
        streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
        break;

      case "Video Content Started":
        streamingAnalytics.notifyPlay();
        streamingAnalytics.getPlaybackSession().setAsset(contentAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", contentAsset);
        break;

      case "Video Content Completed":
        streamingAnalytics.notifyEnd();
        streamingAnalytics.getPlaybackSession().setAsset(contentAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", contentAsset);
        break;

      case "Video Ad Started":
        streamingAnalytics.notifyPlay();
        streamingAnalytics.getPlaybackSession().setAsset(adAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", adAsset);
        break;

      case "Video Ad Completed":
        streamingAnalytics.notifyEnd();
        streamingAnalytics.getPlaybackSession().setAsset(adAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", adAsset);
        break;

      default:
        properties.put("name", name);
        Analytics.notifyHiddenEvent(properties);
        logger.verbose("Analytics.hidden(%s)", properties);
    }
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
