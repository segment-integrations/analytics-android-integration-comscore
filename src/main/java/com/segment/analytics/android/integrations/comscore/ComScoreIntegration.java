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

  @SuppressWarnings("WeakerAccess") public static final Factory FACTORY = new Factory() {
    @Override
    public Integration<?> create(ValueMap settings, com.segment.analytics.Analytics analytics) {
      return new ComScoreIntegration(analytics, settings, StreamingAnalyticsFactory.REAL);
    }

    @Override public String key() {
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

    StreamingAnalyticsFactory REAL = new StreamingAnalyticsFactory() {
      @Override public StreamingAnalytics create() {
        return new StreamingAnalytics();
      }
    };
  }

  ComScoreIntegration(com.segment.analytics.Analytics analytics, ValueMap settings,
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
  private void setNullIfNotProvided(@NonNull Map<String, String> asset,
      @NonNull Map<String, ?> comScoreOptions, @NonNull Map<String, ?> stringProperties,
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

  private @NonNull Map<String, String> buildAsset(@NonNull Properties properties,
      @NonNull Map<String, ?> options, @NonNull Map<String, String> mapper) {
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

    boolean fullScreen = properties.getBoolean("fullScreen", false);
    asset.put("ns_st_ws", fullScreen ? "full" : "norm");

    if (properties.containsKey("bitrate")) {
      int bitrate = properties.getInt("bitrate", 0) * 1000; // comScore expects bps.
      asset.put("ns_st_br", String.valueOf(bitrate));
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
  private @NonNull String getStringOrDefaultValue(@NonNull Map<String, ?> m, @NonNull String key,
      @NonNull String defaultValue) {
    Object value = m.get(key);
    if (value instanceof String) {
      return (String) value;
    }
    if (value != null) {
      return String.valueOf(value);
    }

    return defaultValue;
  }

  private void trackVideoPlayback(TrackPayload track, Properties properties,
      Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> playbackMapper = new LinkedHashMap<>();
    playbackMapper.put("asset_id", "ns_st_ci");
    playbackMapper.put("ad_type", "ns_st_ad");
    playbackMapper.put("length", "nst_st_cl");
    playbackMapper.put("video_player", "ns_st_mp");
    playbackMapper.put("sound", "ns_st_vo");

    Map<String, String> playbackAsset = buildAsset(properties, comScoreOptions, playbackMapper);

    if (name.equals("Video Playback Started")) {
      streamingAnalytics = streamingAnalyticsFactory.create();
      streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
      streamingAnalytics.createPlaybackSession();
      return;
    }

    if (streamingAnalytics == null) {
      return;
    }

    switch (name) {
      case "Video Playback Paused":
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

    streamingAnalytics.getPlaybackSession().setAsset(playbackAsset);
    logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", playbackAsset);
  }

  private void trackVideoContent(TrackPayload track, Properties properties,
      Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> contentMapper = new LinkedHashMap<>();
    contentMapper.put("asset_id", "ns_st_ci");
    contentMapper.put("title", "ns_st_ep");
    contentMapper.put("season", "ns_st_sn");
    contentMapper.put("episode", "ns_st_en");
    contentMapper.put("genre", "ns_st_ge");
    contentMapper.put("program", "ns_st_pr");
    contentMapper.put("channel", "ns_st_st");
    contentMapper.put("publisher", "ns_st_pu");
    contentMapper.put("full_episode", "ns_st_ce");
    contentMapper.put("airdate", "ns_st_ddt");

    Map<String, String> contentAsset = buildAsset(properties, comScoreOptions, contentMapper);

    if (streamingAnalytics == null) {
      return;
    }

    switch (name) {
      case "Video Content Started":
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Content Playing":
        streamingAnalytics.notifyPlay(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;

      case "Video Content Completed":
        streamingAnalytics.notifyEnd(playbackPosition);
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
    }

    streamingAnalytics.getPlaybackSession().setAsset(contentAsset);
    logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", contentAsset);
  }

  public void trackVideoAd(TrackPayload track, Properties properties,
      Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);

    Map<String, String> adMapper = new LinkedHashMap<>();
    adMapper.put("asset_id", "ns_st_ami");
    adMapper.put("pod_id", "ns_st_pn");
    adMapper.put("type", "ns_st_ad");
    adMapper.put("length", "ns_st_cl");
    adMapper.put("title", "ns_st_amt");

    Map<String, String> adAsset = buildAsset(properties, comScoreOptions, adMapper);

    if (streamingAnalytics == null) {
      return;
    }

    switch (name) {
      case "Video Ad Started":
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

    streamingAnalytics.getPlaybackSession().setAsset(adAsset);
    logger.verbose("streamingAnalytics.getPlaybackSession().setAsset(%s)", adAsset);
  }

  @Override public void track(TrackPayload track) {
    String event = track.event();
    Properties properties = track.properties();

    Map<String, Object> comScoreOptions = track.integrations().getValueMap("comScore");
    if (isNullOrEmpty(comScoreOptions)) {
      comScoreOptions = Collections.emptyMap();
    }

    switch (event) {
      case "Video Playback Started":
      case "Video Playback Paused":
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

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    Analytics.getConfiguration().setPersistentLabels(traits);
    logger.verbose("Analytics.getConfiguration().setPersistentLabels(%s)", traits);
  }

  @Override public void screen(ScreenPayload screen) {
    String name = screen.name();
    String category = screen.category();
    HashMap<String, String> properties = (HashMap<String, String>) //
        screen.properties().toStringMap();
    properties.put("name", name);
    properties.put("category", category);
    Analytics.notifyViewEvent(properties);
    logger.verbose("Analytics.notifyViewEvent(%s)", properties);
  }
}
