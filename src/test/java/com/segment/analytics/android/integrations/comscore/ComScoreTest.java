package com.segment.analytics.android.integrations.comscore;

import android.app.Application;
import android.content.pm.ApplicationInfo;

import com.comscore.PublisherConfiguration;
import com.comscore.streaming.AdvertisementMetadata;
import com.comscore.streaming.ContentMetadata;
import com.comscore.streaming.StreamingAnalytics;
import com.comscore.streaming.StreamingConfiguration;
import com.segment.analytics.Analytics;
import com.segment.analytics.AnalyticsContext;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.segment.analytics.Utils.createContext;
import static com.segment.analytics.Utils.createTraits;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)

//specifying earlier android SDK so Robolectric doesn't ask for Java 9
@Config(sdk = 28)

public class ComScoreTest {

  private final static String EXPECTED_PARTNER_ID = "24186693";

  @Mock
  Application context;
  @Mock
  ComScoreAnalytics comScoreAnalytics;
  @Mock
  com.segment.analytics.Analytics analytics;
  @Mock
  StreamingAnalytics streamingAnalytics;
  @Mock
  StreamingConfiguration streamingConfiguration;

  @Mock
  ComScoreIntegration integration;


  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(comScoreAnalytics.createStreamingAnalytics()).thenReturn(streamingAnalytics);

    ValueMap settings = new ValueMap();
    settings.putValue("customerC2", "foobarbar");
    settings.putValue("publisherSecret", "illnevertell");

    when(analytics.logger("comScore")).thenReturn(Logger.with(Analytics.LogLevel.VERBOSE));
    when(analytics.getApplication()).thenReturn(context);
    when(streamingAnalytics.getConfiguration()).thenReturn(streamingConfiguration);
    integration = new ComScoreIntegration(analytics, settings, comScoreAnalytics);
  }

  @Test
  public void initialize() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    when(context.getPackageName()).thenReturn("Code is running");

    //updating Context mock to return roboelectric's RunTimeEnvironment object
    when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application.getApplicationContext());

    ApplicationInfo testApplicationInfo = Mockito.mock(ApplicationInfo.class);

    when(context.getApplicationInfo()).thenReturn(testApplicationInfo);
    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(destinationSettings, analytics);

    Settings settings = integration.getSettings();

    assertEquals("foobarbar", settings.getCustomerId());
    assertEquals("illnevertell", settings.getPublisherSecret());

    // Defaults
    assertNull(settings.getAppName());
    assertEquals(60, settings.getAutoUpdateInterval());
    assertTrue(settings.isUseHTTPS());
    assertFalse(settings.isAutoUpdate());
    assertTrue(settings.isForegroundOnly());
  }

  @Test
  public void initializeWithSettings() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("appName", "Agapito");
    destinationSettings.putValue("useHTTPS", false);
    destinationSettings.putValue("foregroundOnly", false);
    destinationSettings.putValue("autoUpdate", true);
    destinationSettings.putValue("autoUpdateInterval", 12345);

    //updating Context mock to return roboelectric's RunTimeEnvironment object
    when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application.getApplicationContext());

    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(destinationSettings, analytics);

    Settings settings = integration.getSettings();

    assertEquals("foobarbar", settings.getCustomerId());
    assertEquals("illnevertell", settings.getPublisherSecret());
    assertEquals("Agapito", settings.getAppName());
    assertEquals(12345, settings.getAutoUpdateInterval());
    assertFalse(settings.isUseHTTPS());
    assertTrue(settings.isAutoUpdate());
    assertFalse(settings.isForegroundOnly());
  }

  @Test
  public void initializeWithAutoUpdateMode() throws IllegalStateException, NoSuchFieldException, IllegalAccessException {
    ValueMap settings = new ValueMap();
    settings.putValue("partnerId", "24186693");
    settings.putValue("c2", "foobarbar");
    settings.putValue("publisherSecret", "illnevertell");
    settings.putValue("appName", "testApp");
    settings.putValue("useHTTPS", true);
    settings.putValue("autoUpdateInterval", 2000);
    settings.putValue("autoUpdate", true);
    settings.putValue("foregroundOnly", true);

    Mockito.reset(comScoreAnalytics);
    integration = new ComScoreIntegration(analytics, settings, comScoreAnalytics);

    ArgumentCaptor<PublisherConfiguration> publisherCaptor =
            ArgumentCaptor.forClass(PublisherConfiguration.class);

    Mockito.verify(comScoreAnalytics, Mockito.times(1))
            .start(Mockito.eq(context), Mockito.eq(EXPECTED_PARTNER_ID), publisherCaptor.capture());

    Settings integrationSettings = integration.getSettings();
    assertEquals("testApp", integrationSettings.getAppName());
    assertEquals(2000, integrationSettings.getAutoUpdateInterval());
    assertTrue(integrationSettings.isForegroundOnly());
  }

  @Test
  public void initializeWithoutAutoUpdateMode() throws IllegalStateException {
    ValueMap settings = new ValueMap();
    settings.putValue("partnerId", "24186693");
    settings.putValue("c2", "foobarbar");
    settings.putValue("publisherSecret", "illnevertell");
    settings.putValue("appName", "testApp");
    settings.putValue("useHTTPS", true);
    settings.putValue("autoUpdateInterval", null);
    settings.putValue("autoUpdate", false);
    settings.putValue("foregroundOnly", false);

    integration = new ComScoreIntegration(analytics, settings, comScoreAnalytics);

    Mockito.reset(comScoreAnalytics);
    integration = new ComScoreIntegration(analytics, settings, comScoreAnalytics);

    ArgumentCaptor<PublisherConfiguration> publisherCaptor =
            ArgumentCaptor.forClass(PublisherConfiguration.class);

    Mockito.verify(comScoreAnalytics, Mockito.times(1))
            .start(Mockito.eq(context), Mockito.eq(EXPECTED_PARTNER_ID), publisherCaptor.capture());

    Settings integrationSettings = integration.getSettings();

    assertEquals("testApp", integrationSettings.getAppName());
    assertEquals(60, integrationSettings.getAutoUpdateInterval());
//    assertEquals(UsagePropertiesAutoUpdateMode.DISABLED, publisher.getUsagePropertiesAutoUpdateMode());
  }

  @Test
  public void initializeWithConsentFlag() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("appName", "Agapito");
    destinationSettings.putValue("useHTTPS", false);
    destinationSettings.putValue("foregroundOnly", false);
    destinationSettings.putValue("autoUpdate", true);
    destinationSettings.putValue("autoUpdateInterval", 12345);
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    //updating Context mock to return roboelectric's RunTimeEnvironment object
    when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application.getApplicationContext());

    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(destinationSettings, analytics);

    Settings settings = integration.getSettings();

    assertEquals("foobarbar", settings.getCustomerId());
    assertEquals("illnevertell", settings.getPublisherSecret());
    assertEquals("Agapito", settings.getAppName());
    assertEquals(12345, settings.getAutoUpdateInterval());
    assertEquals("consentFlagProp", settings.getConsentFlagProp());
    assertFalse(settings.isUseHTTPS());
    assertTrue(settings.isAutoUpdate());
    assertFalse(settings.isForegroundOnly());
  }

  @Test
  public void track() {
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("foo").build());
    Properties properties = new Properties().putValue("name", "foo");
    Map<String, String> expected = properties.toStringMap();

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expected);
  }

  @Test
  public void trackWithProps() {
    integration.track(new TrackPayload.Builder().anonymousId("foo") //
            .event("Completed Order")
            .properties(new Properties().putValue(20.0).putValue("product", "Ukelele"))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "Completed Order");
    expected.put("value", "20.0");
    expected.put("product", "Ukelele");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expected);
  }

  @Test
  public void setupWithVideoPlaybackStarted() {
    setupWithVideoPlaybackStarted(false);
  }

  @Test
  public void setupWithVideoPlaybackStartedCamelCase() {
    setupWithVideoPlaybackStarted(true);
  }

  private void setupWithVideoPlaybackStarted(boolean useCamelCaseProperties) {
    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "norm");
    expected.put("ns_st_br", "0");
    expected.put("c3", "some value");
    expected.put("c4", "another value");
    expected.put("c6", "and another one");

    // Both camel case and snake case properties should be supported.
    if (useCamelCaseProperties) {
      integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Started")
              .properties(new Properties().putValue("assetId", 1234)
                      .putValue("adType", "pre-roll")
                      .putValue("totalLength", 120)
                      .putValue("videoPlayer", "youtube")
                      .putValue("sound", 80)
                      .putValue("fullScreen", false)
                      .putValue("c3", "some value")
                      .putValue("c4", "another value")
                      .putValue("c6", "and another one"))
              .build());
    } else {
      integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Started")
              .properties(new Properties().putValue("asset_id", 1234)
                      .putValue("ad_type", "pre-roll")
                      .putValue("total_length", 120)
                      .putValue("video_player", "youtube")
                      .putValue("sound", 80)
                      .putValue("full_screen", false)
                      .putValue("c3", "some value")
                      .putValue("c4", "another value")
                      .putValue("c6", "and another one"))
              .build());
    }


    Map<String, String> contentIdMapper = new LinkedHashMap<>();
    contentIdMapper.put("ns_st_ci", "1234");

    Mockito.verify(streamingAnalytics).createPlaybackSession();
    Mockito.verify(streamingAnalytics, atLeast(1))
            .setMetadata(refEq(getContentMetadata(contentIdMapper)));

    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackStarted() {
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Started")
            .properties(new Properties().putValue("asset_id", 1234)
                    .putValue("ad_type", "pre-roll")
                    .putValue("total_length", 120)
                    .putValue("video_player", "youtube")
                    .putValue("sound", 80)
                    .putValue("bitrate", 40)
                    .putValue("full_screen", true))
            .build());

    Map<String, String> contentIdMapper = new LinkedHashMap<>();
    contentIdMapper.put("ns_st_ci", "1234");

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "40000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).createPlaybackSession();
    Mockito.verify(streamingAnalytics, atLeast(1))
            .setMetadata(refEq(getContentMetadata(contentIdMapper)));
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackPausedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayload.Builder().anonymousId("foo") //
            .event("Video Playback Paused")
            .properties(new Properties().putValue("assetId", 1234))
            .build());

    Mockito.verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test
  public void videoPlaybackPaused() {
    setupWithVideoPlaybackStarted();
    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("c3", "abc");

    integration.track(new TrackPayload.Builder().anonymousId("foo") //
            .event("Video Playback Paused")
            .properties(new Properties() //
                    .putValue("assetId", 1234)
                    .putValue("adType", "mid-roll")
                    .putValue("totalLength", 100)
                    .putValue("videoPlayer", "vimeo")
                    .putValue("playbackPosition", 10)
                    .putValue("fullScreen", true)
                    .putValue("bitrate", 50)
                    .putValue("sound", 80))
            .integration("comScore", comScoreOptions)
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "vimeo");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "50000");
    expected.put("c3", "abc");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).notifyPause();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackBufferStarted() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Buffer Started")
            .properties(new Properties().putValue("assetId", 7890)
                    .putValue("adType", "post-roll")
                    .putValue("totalLength", 700)
                    .putValue("videoPlayer", "youtube")
                    .putValue("playbackPosition", 20)
                    .putValue("fullScreen", false)
                    .putValue("bitrate", 500)
                    .putValue("sound", 80))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "norm");
    expected.put("ns_st_br", "500000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");
    Mockito.verify(streamingAnalytics).startFromPosition(20);
    Mockito.verify(streamingAnalytics).notifyBufferStart();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackBufferCompleted() {
    videoPlaybackBufferCompleted(false);
  }

  @Test
  public void videoPlaybackBufferCompletedCamelCase() {
    videoPlaybackBufferCompleted(true);
  }

  /*
   * One test that verifies camel case and snake case are handled in properties.
   */
  private void videoPlaybackBufferCompleted(boolean useCamelCaseProperties) {
    setupWithVideoPlaybackStarted(useCamelCaseProperties);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Buffer Completed")
            .properties(new Properties().putValue("asset_id", 1029)
                    .putValue("ad_type", "pre-roll")
                    .putValue("total_length", 800)
                    .putValue("video_player", "vimeo")
                    .putValue("position", 30)
                    .putValue("full_screen", true)
                    .putValue("bitrate", 500)
                    .putValue("sound", 80))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "vimeo");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "500000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).startFromPosition(30);
    Mockito.verify(streamingAnalytics).notifyBufferStop();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackSeekStarted() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Seek Started")
            .properties(new Properties().putValue("asset_id", 3948)
                    .putValue("ad_type", "mid-roll")
                    .putValue("total_length", 900)
                    .putValue("video_player", "youtube")
                    .putValue("position", 40)
                    .putValue("full_screen", true)
                    .putValue("bitrate", 500)
                    .putValue("sound", 80))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "500000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");
    Mockito.verify(streamingAnalytics).notifySeekStart();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackSeekCompleted() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Seek Completed")
            .properties(new Properties().putValue("assetId", 6767)
                    .putValue("adType", "post-roll")
                    .putValue("totalLength", 400)
                    .putValue("videoPlayer", "vimeo")
                    .putValue("playbackPosition", 50)
                    .putValue("fullScreen", true)
                    .putValue("bitrate", 500)
                    .putValue("sound", 80))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "vimeo");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "500000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).startFromPosition(50);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoPlaybackResumed() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Resumed")
            .properties(new Properties().putValue("assetId", 5332)
                    .putValue("adType", "post-roll")
                    .putValue("totalLength", 100)
                    .putValue("videoPlayer", "youtube")
                    .putValue("playbackPosition", 60)
                    .putValue("fullScreen", true)
                    .putValue("bitrate", 500)
                    .putValue("sound", 80))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "500000");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).startFromPosition(60);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics.getConfiguration()).addLabels(expected);
  }

  @Test
  public void videoContentStartedWithDigitalAirdate() {
    setupWithVideoPlaybackStarted();
    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("digitalAirdate", "2014-01-20");
    comScoreOptions.put("contentClassificationType", "vc12");

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Content Started")
            .properties(new Properties()
                    .putValue("assetId", 9324)
                    .putValue("title", "Meeseeks and Destroy")
                    .putValue("season", 1)
                    .putValue("episode", 5)
                    .putValue("genre", "cartoon")
                    .putValue("program", "Rick and Morty")
                    .putValue("channel", "cartoon network")
                    .putValue("publisher", "Turner Broadcasting System")
                    .putValue("fullEpisode", true)
                    .putValue("podId", "segment A")
                    .putValue("totalLength", "120")
                    .putValue("playbackPosition", 70))
            .integration("comScore", comScoreOptions)
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "9324");
    expected.put("ns_st_ep", "Meeseeks and Destroy");
    expected.put("ns_st_sn", "1");
    expected.put("ns_st_en", "5");
    expected.put("ns_st_ge", "cartoon");
    expected.put("ns_st_pr", "Rick and Morty");
    expected.put("ns_st_st", "cartoon network");
    expected.put("ns_st_pu", "Turner Broadcasting System");
    expected.put("ns_st_ce", "true");
    expected.put("ns_st_ddt", "2014-01-20");
    expected.put("ns_st_pn", "segment A");
    expected.put("ns_st_cl", "120000");
    expected.put("ns_st_ct", "vc12");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");


    Mockito.verify(streamingAnalytics).startFromPosition(70);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics, atLeast(1))
            .setMetadata(refEq(getContentMetadata(expected)));
  }

  @Test
  public void videoContentStartedWithTVAirdate() {
    setupWithVideoPlaybackStarted();
    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("tvAirdate", "2017-05-14");
    comScoreOptions.put("contentClassificationType", "vc12");

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Content Started")
            .properties(new Properties()
                    .putValue("title", "Meeseeks and Destroy")
                    .putValue("season", 1)
                    .putValue("episode", 5)
                    .putValue("genre", "cartoon")
                    .putValue("program", "Rick and Morty")
                    .putValue("channel", "cartoon network")
                    .putValue("publisher", "Turner Broadcasting System")
                    .putValue("full_episode", true)
                    .putValue("pod_id", "segment A")
                    .putValue("total_length", "120")
                    .putValue("position", 70))
            .integration("comScore", comScoreOptions)
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "0");
    expected.put("ns_st_ep", "Meeseeks and Destroy");
    expected.put("ns_st_sn", "1");
    expected.put("ns_st_en", "5");
    expected.put("ns_st_ge", "cartoon");
    expected.put("ns_st_pr", "Rick and Morty");
    expected.put("ns_st_st", "cartoon network");
    expected.put("ns_st_pu", "Turner Broadcasting System");
    expected.put("ns_st_ce", "true");
    expected.put("ns_st_tdt", "2017-05-14");
    expected.put("ns_st_pn", "segment A");
    expected.put("ns_st_cl", "120000");
    expected.put("ns_st_ct", "vc12");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).startFromPosition(70);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics, atLeast(1))
            .setMetadata(refEq(getContentMetadata(expected)));
  }

  @Test
  public void videoContentStartedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayload.Builder().anonymousId("foo") //
            .event("Video Content Started")
            .properties(new Properties().putValue("assetId", 5678))
            .build());
    Mockito.verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test
  public void videoContentPlaying() {
    setupWithVideoPlaybackStarted();
    assertTrue(integration.configurationLabels.containsKey("ns_st_ad") && integration.configurationLabels.get("ns_st_ad") != null);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Content Playing")
            .properties(new Properties().putValue("assetId", 123214)
                    .putValue("title", "Look Who's Purging Now")
                    .putValue("season", 2)
                    .putValue("episode", 9)
                    .putValue("genre", "cartoon")
                    .putValue("program", "Rick and Morty")
                    .putValue("channel", "cartoon network")
                    .putValue("publisher", "Turner Broadcasting System")
                    .putValue("fullEpisode", true)
                    .putValue("airdate", "2015-09-27")
                    .putValue("podId", "segment A")
                    .putValue("playbackPosition", 70))
            .build());

    Mockito.verify(streamingAnalytics).

            startFromPosition(70);
    Mockito.verify(streamingAnalytics).notifyPlay();

  }

  @Test
  public void videoContentPlayingWithAdType() {
    setupWithVideoPlaybackStarted();
    assertTrue(integration.configurationLabels.containsKey("ns_st_ad") && integration.configurationLabels.get("ns_st_ad") != null);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Content Playing")
            .properties(new Properties().putValue("assetId", 123214)
                    .putValue("title", "Look Who's Purging Now")
                    .putValue("season", 2)
                    .putValue("episode", 9)
                    .putValue("genre", "cartoon")
                    .putValue("program", "Rick and Morty")
                    .putValue("channel", "cartoon network")
                    .putValue("publisher", "Turner Broadcasting System")
                    .putValue("fullEpisode", true)
                    .putValue("podId", "segment A")
                    .putValue("playbackPosition", 70))
            .build());



    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "123214");
    expected.put("ns_st_ep", "Look Who's Purging Now");
    expected.put("ns_st_sn", "2");
    expected.put("ns_st_en", "9");
    expected.put("ns_st_ge", "cartoon");
    expected.put("ns_st_pr", "Rick and Morty");
    expected.put("ns_st_st", "cartoon network");
    expected.put("ns_st_pu", "Turner Broadcasting System");
    expected.put("ns_st_ce", "true");
    expected.put("ns_st_pn", "segment A");
    expected.put("ns_st_ct", "vc00");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics, atLeast(1))
            .setMetadata(refEq(getContentMetadata(expected)));
    Mockito.verify(streamingAnalytics).startFromPosition(70);
    Mockito.verify(streamingAnalytics).notifyPlay();
  }

  @Test
  public void videoContentCompleted() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Content Completed")
            .properties(new Properties().putValue("assetId", 9324)
                    .putValue("title", "Raising Gazorpazorp")
                    .putValue("season", 1)
                    .putValue("episode", 7)
                    .putValue("genre", "cartoon")
                    .putValue("program", "Rick and Morty")
                    .putValue("channel", "cartoon network")
                    .putValue("publisher", "Turner Broadcasting System")
                    .putValue("fullEpisode", true)
                    .putValue("airdate", "2014-10-20")
                    .putValue("podId", "segment A")
                    .putValue("playbackPosition", 80))
            .build());
    Mockito.verify(streamingAnalytics).notifyEnd();
  }

  @Test
  public void videoAdStarted() {
    setupWithVideoPlaybackStarted();
    assertTrue(integration.configurationLabels.containsKey("ns_st_ad") && integration.configurationLabels.get("ns_st_ad") != null);
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Started")
            .properties(new Properties().putValue("asset_id", 4311)
                    .putValue("pod_id", "adSegmentA")
                    .putValue("type", "pre-roll")
                    .putValue("total_length", 120)
                    .putValue("position", 0)
                    .putValue("title", "Helmet Ad"))
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ami", "4311");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("ns_st_cl", "120000");
    expected.put("ns_st_amt", "Helmet Ad");
    expected.put("ns_st_ct", "va00");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    Mockito.verify(streamingAnalytics).startFromPosition(0);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics).setMetadata(refEq(getAdvertisementMetadata(expected)));

  }

  @Test
  public void videoAdStartedWithContentId() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Started")
            .properties(new Properties().putValue("assetId", 4311)
                    .putValue("podId", "adSegmentA")
                    .putValue("type", "pre-roll")
                    .putValue("totalLength", 120)
                    .putValue("playbackPosition", 0)
                    .putValue("title", "Helmet Ad"))
            .build());
    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ami", "4311");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("ns_st_cl", "120000");
    expected.put("ns_st_amt", "Helmet Ad");
    expected.put("ns_st_ct", "va00");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");
    expected.put("ns_st_ci", "1234");

    Mockito.verify(streamingAnalytics).startFromPosition(0);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics).setMetadata(refEq(getAdvertisementMetadata(expected)));
  }

  @Test
  public void videoAdStartedWithAdClassificationType() {
    setupWithVideoPlaybackStarted();
    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("adClassificationType", "va14");

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Started")
            .properties(new Properties().putValue("asset_id", 4311)
                    .putValue("pod_id", "adSegmentA")
                    .putValue("type", "pre-roll")
                    .putValue("total_length", 120)
                    .putValue("position", 0)
                    .putValue("title", "Helmet Ad"))
            .integration("comScore", comScoreOptions)
            .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ami", "4311");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("ns_st_cl", "120000");
    expected.put("ns_st_amt", "Helmet Ad");
    expected.put("ns_st_ct", "va14");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    streamingAnalytics.startFromPosition(0);
    Mockito.verify(streamingAnalytics).notifyPlay();
    Mockito.verify(streamingAnalytics).setMetadata(refEq(getAdvertisementMetadata(expected)));
  }

  @Test
  public void videoAdStartedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayload.Builder().anonymousId("foo") //
            .event("Video Ad Started")
            .properties(new Properties().putValue("assetId", 4324))
            .build());

    Mockito.verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test
  public void videoAdPlaying() {
    setupWithVideoPlaybackStarted();
    assertTrue(integration.configurationLabels.containsKey("ns_st_ad") && integration.configurationLabels.get("ns_st_ad") != null);
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Playing")
            .properties(new Properties().putValue("assetId", 4311)
                    .putValue("podId", "adSegmentA")
                    .putValue("adType", "pre-roll")
                    .putValue("totalLength", 120)
                    .putValue("playbackPosition", 20)
                    .putValue("title", "Helmet Ad"))
            .build());
    streamingAnalytics.startFromPosition(20);
    Mockito.verify(streamingAnalytics).notifyPlay();

  }

  @Test
  public void videoAdCompleted() {
    setupWithVideoPlaybackStarted();
    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Completed")
            .properties(new Properties().putValue("assetId", 3425)
                    .putValue("podId", "adSegmentb")
                    .putValue("type", "mid-roll")
                    .putValue("totalLength", 100)
                    .putValue("playbackPosition", 100)
                    .putValue("title", "Helmet Ad"))
            .build());

    Mockito.verify(streamingAnalytics).notifyEnd();
  }

  @Test
  public void identify() {
    Traits traits = new Traits();
    traits.putValue("firstName", "Kylo");
    traits.putValue("lastName", "Ren");

    integration.identify(new IdentifyPayload.Builder().userId("foo")
            .anonymousId("foobar").traits(traits).build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("anonymousId", "foobar");
    expected.put("firstName", "Kylo");
    expected.put("lastName", "Ren");
    expected.put("userId", "foo");

    Mockito.verify(comScoreAnalytics, Mockito.times(1))
            .setPersistentLabels(expected);
  }

  @Test
  public void screen() {
    integration.screen(
            new ScreenPayload.Builder().anonymousId("foo").name("SmartWatches")
                    .category("Purchase Screen").build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "SmartWatches");
    expected.put("category", "Purchase Screen");

    Mockito.verify(comScoreAnalytics, Mockito.times(1))
            .notifyViewEvent(expected);
  }


  @Test
  public void trackWithConsentFlagPropBoolTrue() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp", true).putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "true");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropStringTrue() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp", "true").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "true");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropIntOne() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp", 1).putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "1");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropStringOne() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp", "1").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "1");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropBoolFalse() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp",false).putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "false");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);

  }

  @Test
  public void trackWithConsentFlagPropStringFalse() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","false").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "false");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);

  }

  @Test
  public void trackWithConsentFlagPropIntZero() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp",0).putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "0");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);

  }


  @Test
  public void trackWithConsentFlagPropStringZero() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","0").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "0");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropUsPrivacyStringN() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","1YNY").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "1YNY");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropUsPrivacyStringY() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","1YYY").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "1YYY");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropUsPrivacyStringDash() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","1Y-Y").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "1Y-Y");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropValueNull() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp",null).putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "null");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropAdhocVal() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("consentFlagProp","somevalue").putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");
    expectedProps.put("consentFlagProp", "somevalue");
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagPropMissing() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("prop1", "foo"))
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagContextStringOne() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    Traits traits = createTraits("bar").putValue("consentFlagProp", "1");
    AnalyticsContext analyticsContext = createContext(traits);


    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);
    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("prop1", "foo"))
            .context(analyticsContext)
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");

    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagContextUsPrivacyStringDash() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    Traits traits = createTraits("bar").putValue("consentFlagProp", "1---");
    AnalyticsContext analyticsContext = createContext(traits);


    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);
    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("prop1", "foo"))
            .context(analyticsContext)
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagContextIntZero() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    Traits traits = createTraits("bar").putValue("consentFlagProp", 0);
    AnalyticsContext analyticsContext = createContext(traits);


    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);
    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .properties(new Properties().putValue("prop1", "foo"))
            .context(analyticsContext)
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("prop1", "foo");
    expectedProps.put("name", "Test Event");

    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "0");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void trackWithConsentFlagNull() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.track(new TrackPayload.Builder().anonymousId("foo anon id") //
            .event("Test Event")
            .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("name", "Test Event");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedProps);
  }

  @Test
  public void identifyConsentFlagStringOne() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagTrait");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    Traits traits = new Traits();
    traits.putValue("firstName", "Kylo");
    traits.putValue("lastName", "Ren");
    traits.putValue("consentFlagTrait", "1");

    integration.identify(new IdentifyPayload.Builder().userId("foo")
            .anonymousId("foobar").traits(traits).build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("anonymousId", "foobar");
    expected.put("firstName", "Kylo");
    expected.put("lastName", "Ren");
    expected.put("userId", "foo");
    expected.put("consentFlagTrait", "1");
    expected.put("cs_ucfr", "1");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expected);
    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
  }

  @Test
  public void screenConsentFlagPropStringOne() {
    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.screen(
            new ScreenPayload.Builder()
                    .anonymousId("foo")
                    .name("SmartWatches")
                    .category("Purchase Screen")
                    .properties(new Properties().putValue("consentFlagProp","1"))
                    .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("name", "SmartWatches");
    expectedProps.put("category", "Purchase Screen");
    expectedProps.put("consentFlagProp", "1");

    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyViewEvent(expectedProps);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
  }

  @Test
  public void screenConsentFlagContextStringOne() {

    ValueMap destinationSettings = new ValueMap();
    destinationSettings.putValue("c2", "foobarbar");
    destinationSettings.putValue("publisherSecret", "illnevertell");
    destinationSettings.putValue("consentFlag", "consentFlagProp");

    Traits traits = createTraits("bar").putValue("consentFlagProp", "1");
    AnalyticsContext analyticsContext = createContext(traits);

    integration = new ComScoreIntegration(analytics, destinationSettings, comScoreAnalytics);

    integration.screen(
            new ScreenPayload.Builder()
                    .anonymousId("foo")
                    .name("SmartWatches")
                    .category("Purchase Screen")
                    .context(analyticsContext)
                    .build());

    LinkedHashMap<String, String> expectedProps = new LinkedHashMap<>();
    expectedProps.put("name", "SmartWatches");
    expectedProps.put("category", "Purchase Screen");

    LinkedHashMap<String, String> expectedFlag = new LinkedHashMap<>();
    expectedFlag.put("cs_ucfr", "1");
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyViewEvent(expectedProps);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expectedFlag);
    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyHiddenEvent(expectedFlag);
  }

  private ContentMetadata getContentMetadata(Map<String, String> asset){
    return new ContentMetadata.Builder()
            .customLabels(asset)
            .build();
  }
  private AdvertisementMetadata getAdvertisementMetadata(Map<String, String> adAsset){
    return new AdvertisementMetadata.Builder()
            .customLabels(adAsset)
            .build();
  }
}