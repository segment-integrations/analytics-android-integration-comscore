package com.segment.analytics.android.integrations.comscore;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;
import com.comscore.streaming.AdvertisementMetadata;
import com.comscore.streaming.ContentMetadata;
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
              return new ComScoreIntegration(analytics, settings);
            }

            @Override
            public String key() {
              return COMSCORE_KEY;
            }
          };

  private final static String COMSCORE_KEY = "comScore";
  private final static String PARTNER_ID = "24186693";

  private Settings settings;
  private ComScoreAnalytics comScoreAnalytics;
  private StreamingAnalytics streamingAnalytics;
  private Logger logger;

  ComScoreIntegration(com.segment.analytics.Analytics analytics,
                      ValueMap destinationSettings) {
    this(analytics, destinationSettings, new ComScoreAnalytics.DefaultcomScoreAnalytics(analytics.logger(COMSCORE_KEY)));
  }

  ComScoreIntegration(
          com.segment.analytics.Analytics analytics,
          ValueMap destinationSettings,
          ComScoreAnalytics comScoreAnalytics) {

    this.comScoreAnalytics = comScoreAnalytics;
    this.settings = new Settings(destinationSettings);
    this.logger = analytics.logger(COMSCORE_KEY);

    comScoreAnalytics.start(analytics.getApplication(), PARTNER_ID, settings.toPublisherConfiguration());
    settings.analyticsConfig();
  }

  /**
   * Store a value for {@param k} in {@param asset} by checking {@param comScoreOptions} first and
   * falling back to {@param properties}. Uses {@code "*null"} it not found in either.
   */
  private void setNullIfNotProvided(
          Map<String, String> asset,
          Map<String, ?> comScoreOptions,
          Map<String, ?> stringProperties,
          String key) {
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
          Properties properties, Map<String, String> mapper) {
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

  private Map<String, String> mapPlaybackProperties(
          Properties properties,
          Map<String, ?> options,
          Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    boolean fullScreen = properties.getBoolean("fullScreen", false);
    if (fullScreen == false) {
      fullScreen =  properties.getBoolean("full_screen", false);
    }
    asset.put("ns_st_ws", fullScreen ? "full" : "norm");

    int bitrate = properties.getInt("bitrate", 0) * 1000; // comScore expects bps.
    asset.put("ns_st_br", String.valueOf(bitrate));

    setNullIfNotProvided(asset, options, properties, "c3");
    setNullIfNotProvided(asset, options, properties, "c4");
    setNullIfNotProvided(asset, options, properties, "c6");

    return asset;
  }

  private Map<String, String> mapContentProperties(
          Properties properties,
          Map<String, ?> options,
          Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    int contentAssetId = properties.getInt("assetId", 0);
    if (contentAssetId == 0) {
      contentAssetId = properties.getInt("asset_id", 0);
    }
    asset.put("ns_st_ci", String.valueOf(contentAssetId));

    if (properties.containsKey("totalLength") || properties.containsKey("total_length")) {
      int length = properties.getInt("totalLength", 0) * 1000; // comScore expects milliseconds.
      if (length == 0) {
        length = properties.getInt("total_length", 0) * 1000;
      }
      asset.put("ns_st_cl", String.valueOf(length));
    }

    if (options.containsKey("contentClassificationType")) {
      String contentClassificationType = String.valueOf(options.get("contentClassificationType"));
      asset.put("ns_st_ct", contentClassificationType);
    } else {
      asset.put("ns_st_ct", "vc00");
    }

    if (options.containsKey("digitalAirdate")) {
      String digitalAirdate = String.valueOf(options.get("digitalAirdate"));
      asset.put("ns_st_ddt", digitalAirdate);
    }

    if (options.containsKey("tvAirdate")) {
      String tvAirdate = String.valueOf(options.get("tvAirdate"));
      asset.put("ns_st_tdt", tvAirdate);
    }

    setNullIfNotProvided(asset, options, properties, "c3");
    setNullIfNotProvided(asset, options, properties, "c4");
    setNullIfNotProvided(asset, options, properties, "c6");

    return asset;
  }

  private Map<String, String> mapAdProperties(
          Properties properties,
          Map<String, ?> options,
          Map<String, String> mapper) {

    Map<String, String> asset = mapSpecialKeys(properties, mapper);

    if (properties.containsKey("totalLength") || properties.containsKey("total_length")) {
      int length = properties.getInt("totalLength", 0) * 1000; // comScore expects milliseconds.
      if (length == 0) {
        length = properties.getInt("total_length", 0) * 1000;
      }
      asset.put("ns_st_cl", String.valueOf(length));
    }

    if (options.containsKey("adClassificationType")) {
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
  private String getStringOrDefaultValue(
          Map<String, ?> m, String key, String defaultValue) {
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
    if (playbackPosition == 0) {
      playbackPosition = properties.getLong("position", 0);
    }

    Map<String, String> playbackMapper = new LinkedHashMap<>();
    playbackMapper.put("videoPlayer", "ns_st_mp");
    playbackMapper.put("video_player", "ns_st_mp");
    playbackMapper.put("sound", "ns_st_vo");

    Map<String, String> mappedPlaybackProperties =
            mapPlaybackProperties(properties, comScoreOptions, playbackMapper);

    if (name.equals("Video Playback Started")) {
      streamingAnalytics = comScoreAnalytics.createStreamingAnalytics();
      streamingAnalytics.createPlaybackSession();
      streamingAnalytics.getConfiguration().addLabels(mappedPlaybackProperties);

      // The label ns_st_ci must be set through a setAsset call
      Map<String, String> contentIdMapper = new LinkedHashMap<>();
      contentIdMapper.put("assetId", "ns_st_ci");
      contentIdMapper.put("asset_id", "ns_st_ci");

      Map<String, String> mappedContentProperties = mapSpecialKeys(properties, contentIdMapper);

      streamingAnalytics.setMetadata(getContentMetadata(mappedContentProperties));
      return;
    }

    if (streamingAnalytics == null) {
      logger.verbose(
              "streamingAnalytics instance not initialized correctly. Please call Video Playback Started to initialize.");
      return;
    }
    streamingAnalytics.getConfiguration().addLabels(mappedPlaybackProperties);

    switch (name) {
      case "Video Playback Paused":
      case "Video Playback Interrupted":
        streamingAnalytics.notifyPause();
        logger.verbose("streamingAnalytics.notifyPause(%s)", playbackPosition);
        break;
      case "Video Playback Buffer Started":
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyBufferStart();
        logger.verbose("streamingAnalytics.notifyBufferStart(%s)", playbackPosition);
        break;
      case "Video Playback Buffer Completed":
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyBufferStop();
        logger.verbose("streamingAnalytics.notifyBufferStop(%s)", playbackPosition);
        break;
      case "Video Playback Seek Started":
        streamingAnalytics.notifySeekStart();
        logger.verbose("streamingAnalytics.notifySeekStart(%s)", playbackPosition);
        break;
      case "Video Playback Seek Completed":
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
      case "Video Playback Resumed":
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;
    }
  }


  private void trackVideoContent(
          TrackPayload track, Properties properties, Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);
    if (playbackPosition == 0) {
      playbackPosition = properties.getLong("position", 0);
    }

    Map<String, String> contentMapper = new LinkedHashMap<>();
    contentMapper.put("title", "ns_st_ep");
    contentMapper.put("season", "ns_st_sn");
    contentMapper.put("episode", "ns_st_en");
    contentMapper.put("genre", "ns_st_ge");
    contentMapper.put("program", "ns_st_pr");
    contentMapper.put("channel", "ns_st_st");
    contentMapper.put("publisher", "ns_st_pu");
    contentMapper.put("fullEpisode", "ns_st_ce");
    contentMapper.put("full_episode", "ns_st_ce");
    contentMapper.put("podId", "ns_st_pn");
    contentMapper.put("pod_id", "ns_st_pn");

    Map<String, String> mappedContentProperties =
            mapContentProperties(properties, comScoreOptions, contentMapper);

    if (streamingAnalytics == null) {
      logger.verbose(
              "streamingAnalytics instance not initialized correctly. Please call Video Playback Started to initialize.");
      return;
    }

    switch (name) {
      case "Video Content Started":
        streamingAnalytics.setMetadata(getContentMetadata(mappedContentProperties));
        logger.verbose("streamingAnalytics.setMetadata(%s)", mappedContentProperties);
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Content Playing":
        // The presence of ns_st_ad on the StreamingAnalytics's asset means that we just exited an ad break, so
        // we need to call setAsset with the content metadata.  If ns_st_ad is not present, that means the last
        // observed event was related to content, in which case a setAsset call should not be made (because asset
        // did not change).
        if(streamingAnalytics.getConfiguration().containsLabel("ns_st_ad")) {
          streamingAnalytics.setMetadata(getContentMetadata(mappedContentProperties));
          logger.verbose("streamingAnalytics.setMetadata(%s)", mappedContentProperties);
        }

        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;

      case "Video Content Completed":
        streamingAnalytics.notifyEnd();
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
    }
  }

  public void trackVideoAd(
          TrackPayload track, Properties properties, Map<String, Object> comScoreOptions) {
    String name = track.event();
    long playbackPosition = properties.getLong("playbackPosition", 0);
    if (playbackPosition == 0) {
      playbackPosition = properties.getLong("position", 0);
    }

    Map<String, String> adMapper = new LinkedHashMap<>();
    adMapper.put("assetId", "ns_st_ami");
    adMapper.put("asset_id", "ns_st_ami");
    adMapper.put("title", "ns_st_amt");
    adMapper.put("publisher", "ns_st_pu");

    Map<String, String> mappedAdProperties = mapAdProperties(properties, comScoreOptions, adMapper);

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
        String contentId = streamingAnalytics.getConfiguration().getLabel("ns_st_ci");
        if (!isNullOrEmpty(contentId)) {
          mappedAdProperties.put("ns_st_ci", contentId);
        }

        streamingAnalytics.setMetadata(getAdvertisementMetadata(mappedAdProperties));
        logger.verbose("streamingAnalytics.setMetadata(%s)", mappedAdProperties);
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Ad Playing":
        streamingAnalytics.startFromPosition(playbackPosition);
        streamingAnalytics.notifyPlay();
        logger.verbose("streamingAnalytics.notifyPlay(%s)", playbackPosition);
        break;

      case "Video Ad Completed":
        streamingAnalytics.notifyEnd();
        logger.verbose("streamingAnalytics.notifyEnd(%s)", playbackPosition);
        break;
    }
  }
  private ContentMetadata getContentMetadata(Map<String, String> mappedContentProperties){
    return new ContentMetadata.Builder()
            .customLabels(mappedContentProperties)
            .build();
  }

  private AdvertisementMetadata getAdvertisementMetadata(Map<String, String> mappedAdProperties){
    return new AdvertisementMetadata.Builder()
            .customLabels(mappedAdProperties)
            .build();
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
        comScoreAnalytics.notifyHiddenEvent(props);
    }
  }

  @Override
  public void identify(IdentifyPayload identify) {
    super.identify(identify);
    String userId = identify.userId();
    String anonymousId = identify.anonymousId();
    HashMap<String, String> traits = (HashMap<String, String>) identify.traits().toStringMap();
    traits.put("userId", userId);
    traits.put("anonymousId", anonymousId);
    comScoreAnalytics.setPersistentLabels(traits);
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
    comScoreAnalytics.notifyViewEvent(properties);
  }

  /**
   * Retrieves the settings.
   * @return Settings.
   */
  Settings getSettings() {
    return settings;
  }
}
