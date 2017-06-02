package com.segment.analytics.android.integrations.comscore;

import static com.comscore.UsagePropertiesAutoUpdateMode.DISABLED;
import static com.comscore.UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND;
import static com.comscore.UsagePropertiesAutoUpdateMode.FOREGROUND_ONLY;
import static com.segment.analytics.internal.Utils.isNullOrEmpty;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.comscore.Analytics;
import com.comscore.PartnerConfiguration;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.streaming.StreamingAnalytics;
import com.segment.analytics.Properties;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ComScoreIntegration extends Integration<Void> {

  @SuppressWarnings("WeakerAccess")
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
      builder.usagePropertiesAutoUpdateMode(FOREGROUND_AND_BACKGROUND);
    } else if (foregroundOnly) {
      builder.usagePropertiesAutoUpdateMode(FOREGROUND_ONLY);
    } else {
      builder.usagePropertiesAutoUpdateMode(DISABLED);
    }

    PartnerConfiguration partnerConfig =
        new PartnerConfiguration.Builder().partnerId("24186693").build();
    PublisherConfiguration myPublisherConfig = builder.build();

    Analytics.getConfiguration().addClient(partnerConfig);
    Analytics.getConfiguration().addClient(myPublisherConfig);
    Analytics.start(analytics.getApplication());
  }

  /**
   * Store a value for {@param k} in {@param asset} by checking {@param comScoreOptions} first and
   * falling back to {@param properties}. Uses {@code "*null"} it not found in either.
   */
  private void setNullIfNotProvided(
      @NonNull Map<String, String> asset,
      @NonNull Map<String, ?> comScoreOptions,
      @NonNull Map<String, ?> stringProperties,
      @NonNull String key) {
    String option = getStringOrDefaultValue(comScoreOptions, key, null);
    if (option != null) {
      asset.put(key, option);
      return;
    }

    String property = getStringOrDefaultValue(stringProperties, key, null);
    if (property != null) {
      asset.put(key, property);
      return;
    }
    asset.put(key, "*null");
  }

  private Map<String, String> mapSpecialKeys(
      @NonNull Properties properties, @NonNull Map<String, String> mapper) {
    Map<String, String> asset = new LinkedHashMap<>(mapper.size());

    // Map special keys and preserve only the special keys.
    for (Map.Entry<String, ?> entry : properties.entrySet()) {
      String key = entry.getKey();
      String mappedKey = mapper.get(key);
      Object value = entry.getValue();
      if (!isNullOrEmpty(mappedKey)) {
        asset.put(mappedKey, String.valueOf(value));
      }
    }

    return asset;
  }

  private @NonNull Map<String, String> buildPlaybackAsset(
      @NonNull Properties properties,
      @NonNull Map<String, ?> options,
      @NonNull Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    boolean fullScreen = properties.getBoolean("fullScreen", false);
    asset.put("ns_st_ws", fullScreen ? "full" : "norm");

    int bitrate = properties.getInt("bitrate", 0) * 1000; // comScore expects bps.
    asset.put("ns_st_br", String.valueOf(bitrate));

    setNullIfNotProvided(asset, options, properties, "c3");
    setNullIfNotProvided(asset, options, properties, "c4");
    setNullIfNotProvided(asset, options, properties, "c6");

    return asset;
  }

  private @NonNull Map<String, String> buildContentAsset(
      @NonNull Properties properties,
      @NonNull Map<String, ?> options,
      @NonNull Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    if (properties.containsKey("totalLength")) {
      int length = properties.getInt("totalLength", 0) * 1000; // comScore expects milliseconds.
      asset.put("ns_st_cl", String.valueOf(length));
    }

    if(options.containsKey("contentClassificationType")) {
      String contentClassificationType = String.valueOf(options.get("contentClassificationType"));
      asset.put("ns_st_ct", contentClassificationType);
    } else {
      asset.put("ns_st_ct", "vc00");
    }

    if(options.containsKey("digitalAirdate")) {
      String digitalAirdate = String.valueOf(options.get("digitalAirdate"));
      asset.put("ns_st_ddt",  digitalAirdate);
    }

    if(options.containsKey("tvAirdate")) {
      String tvAirdate = String.valueOf(options.get("tvAirdate"));
      asset.put("ns_st_tdt", tvAirdate);
    }

    setNullIfNotProvided(asset, options, properties, "c3");
    setNullIfNotProvided(asset, options, properties, "c4");
    setNullIfNotProvided(asset, options, properties, "c6");

    return asset;
  }

  private @NonNull Map<String, String> buildAdAsset(
      @NonNull Properties properties,
      @NonNull Map<String, ?> options,
      @NonNull Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    if (properties.containsKey("totalLength")) {
      int length = properties.getInt("totalLength", 0) * 1000; // comScore expects milliseconds.
      asset.put("ns_st_cl", String.valueOf(length));
    }

    if(options.containsKey("adClassificationType")) {
      String adClassificationType = String.valueOf(options.get("adClassificationType"));
      asset.put("ns_st_ct", adClassificationType);
    } else {
      asset.put("ns_st_ct", "va00");
    }

    String adType = String.valueOf(properties.get("type"));
    switch (adType) {
      case "pre-roll":
      case "mid-roll":
      case "post-roll":
        asset.put("ns_st_ad", adType);
        break;
      default:
        asset.put("ns_st_ad", "1");
    }
    setNullIfNotProvided(asset, options, properties, "c3");
    setNullIfNotProvided(asset, options, properties, "c4");
    setNullIfNotProvided(asset, options, properties, "c6");
    return asset;
    }

  /**
   * Returns the value mapped by {@code key} if it exists and is a string, or can be coerced to a
   * string. Returns {@code defaultValue} otherwise.
   *
   * <p>This will return {@code defaultValue} only if the value does not exist, since all types can
   * have a String representation.
   */
  private @NonNull String getStringOrDefaultValue(
      @NonNull Map<String, ?> m, @NonNull String key, @NonNull String defaultValue) {
    Object value = m.get(key);
    if (value instanceof String) {
      return (String) value;
    }
    if (value != null) {
      return String.valueOf(value);
    }

    return defaultValue;
  }

  private void trackVideoPlayback(
      TrackPayload track, Properties properties, Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> playbackMapper = new LinkedHashMap<>();
    playbackMapper.put("videoPlayer", "ns_st_mp");
    playbackMapper.put("sound", "ns_st_vo");

    Map<String, String> playbackAsset = buildPlaybackAsset(properties, comScoreOptions, playbackMapper);

    if (name.equals("Video Playback Started")) {
      streamingAnalytics = streamingAnalyticsFactory.create();
      streamingAnalytics.createPlaybackSession();
      streamingAnalytics.setLabels(playbackAsset);

      // The label ns_st_ci must be set through a setAsset call
      Map<String, String> contentIdMapper = new LinkedHashMap<>();
      contentIdMapper.put("assetId", "ns_st_ci");

      Map<String, String> contentIdAsset = mapSpecialKeys(properties, contentIdMapper);
      streamingAnalytics.getPlaybackSession().setAsset(contentIdAsset);
      return;
    }

    if (streamingAnalytics == null) {
      logger.verbose(
          "streamingAnalytics instance not initialized correctly. Please call Video Playback Started to initialize.");
      return;
    }

    streamingAnalytics.setLabels(playbackAsset);

    switch (name) {
      case "Video Playback Paused":
      case "Video Playback Interrupted":
        streamingAnalytics.notifyPause(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPause(%s)", playbackPosition);
        break;
      case "Video Playback Buffer Started":
        streamingAnalytics.notifyBufferStart(playbackPosition);
        logger.verbose("streamingAnalytics.notifyBufferStart(%s)", playbackPosition);
        break;
      case "Video Playback Buffer Completed":
        streamingAnalytics.notifyBufferStop(playbackPosition);
        logger.verbose("streamingAnalytics.notifyBufferStop(%s)", playbackPosition);
        break;
      case "Video Playback Seek Started":
        streamingAnalytics.notifySeekStart(playbackPosition);
        logger.verbose("streamingAnalytics.notifySeekStart(%s)", playbackPosition);
        break;
      case "Video Playback Seek Completed":
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
      case "Video Playback Resumed":
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;
    }
  }

  private void trackVideoContent(
      TrackPayload track, Properties properties, Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> contentMapper = new LinkedHashMap<>();
    contentMapper.put("assetId", "ns_st_ci");
    contentMapper.put("title", "ns_st_ep");
    contentMapper.put("season", "ns_st_sn");
    contentMapper.put("episode", "ns_st_en");
    contentMapper.put("genre", "ns_st_ge");
    contentMapper.put("program", "ns_st_pr");
    contentMapper.put("channel", "ns_st_st");
    contentMapper.put("publisher", "ns_st_pu");
    contentMapper.put("fullEpisode", "ns_st_ce");
    contentMapper.put("pod_id", "ns_st_pn");


    Map<String, String> contentAsset = buildContentAsset(properties, comScoreOptions, contentMapper);

    if (streamingAnalytics == null) {
      logger.verbose(
          "streamingAnalytics instance not initialized correctly. Please call Video Playback Started to initialize.");
      return;
    }

    switch (name) {
      case "Video Content Started":
        streamingAnalytics.getPlaybackSession().setAsset(contentAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", contentAsset);
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Content Playing":
        // The presence of ns_st_ad on the StreamingAnalytics's asset means that we just exited an ad break, so
        // we need to call setAsset with the content metadata.  If ns_st_ad is not present, that means the last
        // observed event was related to content, in which case a setAsset call should not be made (because asset
        // did not change).
        if (streamingAnalytics.getPlaybackSession().getAsset().containsLabel("ns_st_ad")) {
          streamingAnalytics.getPlaybackSession().setAsset(contentAsset);
          logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", contentAsset);
        }

        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);

        break;

      case "Video Content Completed":
        streamingAnalytics.notifyEnd(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
    }
  }

  public void trackVideoAd(
      TrackPayload track, Properties properties, Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> adMapper = new LinkedHashMap<>();
    adMapper.put("assetId", "ns_st_ami");
    adMapper.put("title", "ns_st_amt");
    adMapper.put("publisher", "ns_st_pu");

    Map<String, String> adAsset = buildAdAsset(properties, comScoreOptions, adMapper);

    if (streamingAnalytics == null) {
      logger.verbose(
          "streamingAnalytics instance not initialized correctly. Please call Video Playback Started to initialize.");
      return;
    }

    switch (name) {
      case "Video Ad Started":
        // The ID for content is not available on Ad Start events, however it will be available on the current
        // StreamingAnalytics's asset. This is because ns_st_ci will have already been set on Content Started
        // calls (if this is a mid or post-roll), or on Video Playback Started (if this is a pre-roll).
        String contentId = streamingAnalytics.getPlaybackSession().getAsset().getLabel("ns_st_ci");

        if (!isNullOrEmpty(contentId)) {
          adAsset.put("ns_st_ci", contentId);
        }

        streamingAnalytics.getPlaybackSession().setAsset(adAsset);
        logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", adAsset);
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Ad Playing":
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Ad Completed":
        streamingAnalytics.notifyEnd(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
    }
  }

  @Override
  public void track(TrackPayload track) {
    String event = track.event();
    Properties properties = track.properties();

    Map<String, Object> comScoreOptions = track.integrations().getValueMap("comScore");
    if (isNullOrEmpty(comScoreOptions)) {
      comScoreOptions = Collections.emptyMap();
    }

    switch (event) {
      case "Video Playback Started":
      case "Video Playback Paused":
      case "Video Playback Interrupted":
      case "Video Playback Buffer Started":
      case "Video Playback Buffer Completed":
      case "Video Playback Seek Started":
      case "Video Playback Seek Completed":
      case "Video Playback Resumed":
        trackVideoPlayback(track, properties, comScoreOptions);
        break;
      case "Video Content Started":
      case "Video Content Playing":
      case "Video Content Completed":
        trackVideoContent(track, properties, comScoreOptions);
        break;
      case "Video Ad Started":
      case "Video Ad Playing":
      case "Video Ad Completed":
        trackVideoAd(track, properties, comScoreOptions);
        break;
      default:
        Map<String, String> props = properties.toStringMap();
        props.put("name", event);
        Analytics.notifyHiddenEvent(props);
        logger.verbose("Analytics.notifyHiddenEvent(%s)", props);
    }
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    Analytics.getConfiguration().setPersistentLabels(traits);
    logger.verbose("Analytics.getConfiguration().setPersistentLabels(%s)", traits);
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
