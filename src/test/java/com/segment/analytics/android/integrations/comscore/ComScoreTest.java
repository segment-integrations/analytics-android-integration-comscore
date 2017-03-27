package com.segment.analytics.android.integrations.comscore;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

import android.app.Application;
import com.comscore.Analytics;
import com.comscore.ClientConfiguration;
import com.comscore.Configuration;
import com.comscore.PartnerConfiguration;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.streaming.Asset;
import com.comscore.streaming.PlaybackSession;
import com.comscore.streaming.StreamingAnalytics;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Analytics.class) public class ComScoreTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  @Mock Configuration configuration;
  private Logger logger;
  @Mock com.segment.analytics.Analytics analytics;
  private ComScoreIntegration integration;
  @Mock Analytics comScore;
  @Mock StreamingAnalytics streamingAnalytics;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(Analytics.class);
    logger = Logger.with(com.segment.analytics.Analytics.LogLevel.DEBUG);
    when(analytics.logger("comScore")).thenReturn(Logger.with(VERBOSE));
    when(Analytics.getConfiguration()).thenReturn(configuration);
    when(analytics.getApplication()).thenReturn(context);
    integration = new ComScoreIntegration(analytics,
        new ValueMap().putValue("customerC2", "foobarbar")
            .putValue("publisherSecret", "illnevertell"),
        new ComScoreIntegration.StreamingAnalyticsFactory() {
          @Override public StreamingAnalytics create() {
            return streamingAnalytics;
          }
        });

    // mock it twice so we can initialize it for tests, but reset the mock after initialization.
    mockStatic(Analytics.class);
  }

  @Test public void factory() {
    ValueMap settings =
        new ValueMap().putValue("c2", "foobarbar").putValue("publisherSecret", "illnevertell");
    when(Analytics.getConfiguration()).thenReturn(configuration);

    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);

    assertThat(integration.customerC2).isEqualTo("foobarbar");
    assertThat(integration.publisherSecret).isEqualTo("illnevertell");
  }

  @Test public void initializeWithDefaultArguments() {
    ValueMap settings = new ValueMap() //
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("setSecure", true);
    when(Analytics.getConfiguration()).thenReturn(configuration);

    ComScoreIntegration integration =
        (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);

    assertThat(integration.customerC2).isEqualTo("foobarbar");
    assertThat(integration.publisherSecret).isEqualTo("illnevertell");
    assertThat(integration.useHTTPS).isTrue();
  }

  @Test public void initializeWithAutoUpdateMode() throws IllegalStateException {
    Configuration configuration = mock(Configuration.class);
    when(Analytics.getConfiguration()).thenReturn(configuration);

    integration = new ComScoreIntegration(analytics, new ValueMap() //
        .putValue("partnerId", "24186693")
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("appName", "testApp")
        .putValue("useHTTPS", true)
        .putValue("autoUpdateInterval", 2000)
        .putValue("autoUpdate", true)
        .putValue("foregroundOnly", true), null);

    ArgumentCaptor<ClientConfiguration> configurationCaptor =
        ArgumentCaptor.forClass(ClientConfiguration.class);
    verify(configuration, times(2)).addClient(configurationCaptor.capture());

    List<ClientConfiguration> capturedConfig = configurationCaptor.getAllValues();
    assertThat(((PartnerConfiguration) capturedConfig.get(0)).getPartnerId()).isEqualTo("24186693");
    assertThat(((PublisherConfiguration) capturedConfig.get(1)).getPublisherId()).isEqualTo(
        "foobarbar");
    assertThat(((PublisherConfiguration) capturedConfig.get(1)).getPublisherSecret()).isEqualTo(
        "illnevertell");
    assertThat(capturedConfig.get(1).getApplicationName()).isEqualTo("testApp");
    assertThat(capturedConfig.get(1).getUsagePropertiesAutoUpdateInterval()).isEqualTo(2000);
    assertThat(capturedConfig.get(1).getUsagePropertiesAutoUpdateMode()).isEqualTo(
        UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND);
  }

  @Test public void initializeWithoutAutoUpdateMode() throws IllegalStateException {
    Configuration configuration = mock(Configuration.class);
    when(Analytics.getConfiguration()).thenReturn(configuration);

    integration = new ComScoreIntegration(analytics, new ValueMap() //
        .putValue("partnerId", "24186693")
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("appName", "testApp")
        .putValue("useHTTPS", true)
        .putValue("autoUpdateInterval", null)
        .putValue("autoUpdate", false)
        .putValue("foregroundOnly", false), null);

    ArgumentCaptor<ClientConfiguration> configurationCaptor =
        ArgumentCaptor.forClass(ClientConfiguration.class);
    verify(configuration, times(2)).addClient(configurationCaptor.capture());

    List<ClientConfiguration> capturedConfig = configurationCaptor.getAllValues();
    assertThat(((PartnerConfiguration) capturedConfig.get(0)).getPartnerId()).isEqualTo("24186693");
    assertThat(((PublisherConfiguration) capturedConfig.get(1)).getPublisherId()).isEqualTo(
        "foobarbar");
    assertThat(((PublisherConfiguration) capturedConfig.get(1)).getPublisherSecret()).isEqualTo(
        "illnevertell");
    assertThat(capturedConfig.get(1).getApplicationName()).isEqualTo("testApp");
    assertThat(capturedConfig.get(1).getUsagePropertiesAutoUpdateInterval()).isEqualTo(60);
    assertThat(capturedConfig.get(1).getUsagePropertiesAutoUpdateMode()).isEqualTo(
        UsagePropertiesAutoUpdateMode.DISABLED);
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    Properties properties = new Properties().putValue("name", "foo");
    Map<String, String> expected = properties.toStringMap();

    verifyStatic();
    Analytics.notifyHiddenEvent(expected);
  }

  @Test public void trackWithProps() {
    integration.track(new TrackPayloadBuilder() //
        .event("Completed Order")
        .properties(new Properties().putValue(20.0).putValue("product", "Ukelele"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "Completed Order");
    expected.put("value", "20.0");
    expected.put("product", "Ukelele");

    verifyStatic();
    Analytics.notifyHiddenEvent(expected);
  }

  void setupWithVideoPlaybackStarted() {
    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Started")
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



    Map<String, String> contentIdMapper = new LinkedHashMap<>();
    contentIdMapper.put("ns_st_ci", "1234");

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "youtube");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "norm");
    expected.put("ns_st_br", "0");
    expected.put("c3", "some value");
    expected.put("c4", "another value");
    expected.put("c6", "and another one");

    verify(streamingAnalytics).createPlaybackSession();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(contentIdMapper);

    verify(streamingAnalytics).setLabels(expected);
    Mockito.reset(streamingAnalytics);
  }

  @Test public void videoPlaybackStarted() {
    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Started")
        .properties(new Properties().putValue("assetId", 1234)
            .putValue("adType", "pre-roll")
            .putValue("totalLength", 120)
            .putValue("videoPlayer", "youtube")
            .putValue("sound", 80)
            .putValue("bitrate", 40)
            .putValue("fullScreen", true))
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

    verify(streamingAnalytics).createPlaybackSession();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(contentIdMapper);

    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackPausedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayloadBuilder() //
        .event("Video Playback Paused")
        .properties(new Properties().putValue("assetId", 1234))
        .build());

    verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test public void videoPlaybackPaused() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("c3", "abc");

    integration.track(new TrackPayloadBuilder() //
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
        .options(new Options().setIntegrationOptions("comScore", comScoreOptions))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_mp", "vimeo");
    expected.put("ns_st_vo", "80");
    expected.put("ns_st_ws", "full");
    expected.put("ns_st_br", "50000");
    expected.put("c3", "abc");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    verify(streamingAnalytics).notifyPause(10);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackBufferStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Buffer Started")
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

    verify(streamingAnalytics).notifyBufferStart(20);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackBufferCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Buffer Completed")
        .properties(new Properties().putValue("assetId", 1029)
            .putValue("adType", "pre-roll")
            .putValue("totalLength", 800)
            .putValue("videoPlayer", "vimeo")
            .putValue("playbackPosition", 30)
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

    verify(streamingAnalytics).notifyBufferStop(30);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackSeekStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbacksession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbacksession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Seek Started")
        .properties(new Properties().putValue("assetId", 3948)
            .putValue("adType", "mid-roll")
            .putValue("totalLength", 900)
            .putValue("videoPlayer", "youtube")
            .putValue("playbackPosition", 40)
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

    verify(streamingAnalytics).notifySeekStart(40);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackSeekCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Seek Completed")
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

    verify(streamingAnalytics).notifyPlay(50);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoPlaybackResumed() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Resumed")
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

    verify(streamingAnalytics).notifyPlay(60);
    verify(streamingAnalytics).setLabels(expected);
  }

  @Test public void videoContentStartedWithDigitalAirdate() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("digitalAirdate", "2014-01-20");
    comScoreOptions.put("contentClassificationType", "vc12");

    integration.track(new TrackPayloadBuilder().event("Video Content Started")
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
        .options(new Options().setIntegrationOptions("comScore", comScoreOptions))
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

    verify(streamingAnalytics).notifyPlay(70);
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoContentStartedWithTVAirdate() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("tvAirdate", "2017-05-14");
    comScoreOptions.put("contentClassificationType", "vc12");

    integration.track(new TrackPayloadBuilder().event("Video Content Started")
        .properties(new Properties()
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
        .options(new Options().setIntegrationOptions("comScore", comScoreOptions))
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

    verify(streamingAnalytics).notifyPlay(70);
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoContentStartedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayloadBuilder() //
        .event("Video Content Started")
        .properties(new Properties().putValue("assetId", 5678))
        .build());

    verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test public void videoContentPlaying() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);


    Asset asset = mock(Asset.class);
    when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

    integration.track(new TrackPayloadBuilder().event("Video Content Playing")
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

    verify(streamingAnalytics).notifyPlay(70);
  }

  @Test public void videoContentPlayingWithAdType() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = mock(Asset.class);
    when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);
    when(asset.containsLabel("ns_st_ad")).thenReturn(true);

    integration.track(new TrackPayloadBuilder().event("Video Content Playing")
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

    verify(playbackSession).setAsset(expected);
    verify(streamingAnalytics).notifyPlay(70);
  }

  @Test public void videoContentCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Content Completed")
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

    verify(streamingAnalytics).notifyEnd(80);
  }

  @Test public void videoAdStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = mock(Asset.class);
    when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

    integration.track(new TrackPayloadBuilder().event("Video Ad Started")
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

    verify(streamingAnalytics).notifyPlay(0);
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoAdStartedWithContentId() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = mock(Asset.class);
    when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);
    when(asset.getLabel("ns_st_ci")).thenReturn("1234");

    integration.track(new TrackPayloadBuilder().event("Video Ad Started")
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

    verify(streamingAnalytics).notifyPlay(0);
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoAdStartedWithAdClassificationType() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = mock(Asset.class);
    when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("adClassificationType", "va14");

    integration.track(new TrackPayloadBuilder().event("Video Ad Started")
        .properties(new Properties().putValue("assetId", 4311)
            .putValue("podId", "adSegmentA")
            .putValue("type", "pre-roll")
            .putValue("totalLength", 120)
            .putValue("playbackPosition", 0)
            .putValue("title", "Helmet Ad"))
        .options(new Options().setIntegrationOptions("comScore", comScoreOptions))
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

    verify(streamingAnalytics).notifyPlay(0);
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoAdStartedWithoutVideoPlaybackStarted() {
    integration.track(new TrackPayloadBuilder() //
        .event("Video Ad Started")
        .properties(new Properties().putValue("assetId", 4324))
        .build());

    verifyNoMoreInteractions(streamingAnalytics);
  }

  @Test public void videoAdPlaying() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Ad Playing")
        .properties(new Properties().putValue("assetId", 4311)
            .putValue("podId", "adSegmentA")
            .putValue("type", "pre-roll")
            .putValue("totalLength", 120)
            .putValue("playbackPosition", 20)
            .putValue("title", "Helmet Ad"))
        .build());

    verify(streamingAnalytics).notifyPlay(20);
  }

  @Test public void videoAdCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Ad Completed")
        .properties(new Properties().putValue("assetId", 3425)
            .putValue("podId", "adSegmentb")
            .putValue("type", "mid-roll")
            .putValue("totalLength", 100)
            .putValue("playbackPosition", 100)
            .putValue("title", "Helmet Ad"))
        .build());

    verify(streamingAnalytics).notifyEnd(100);
  }

  @Test public void identify() throws JSONException {
    Configuration configuration = mock(Configuration.class);
    when(Analytics.getConfiguration()).thenReturn(configuration);
    Traits traits = createTraits("foo") //
        .putValue("anonymousId", "foobar")
        .putValue("firstName", "Kylo")
        .putValue("lastName", "Ren");

    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("anonymousId", "foobar");
    expected.put("firstName", "Kylo");
    expected.put("lastName", "Ren");
    expected.put("userId", "foo");

    verify(configuration).setPersistentLabels(expected);
  }

  @Test public void screen() {
    integration.screen(
        new ScreenPayloadBuilder().name("SmartWatches").category("Purchase Screen").build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "SmartWatches");
    expected.put("category", "Purchase Screen");

    verifyStatic();
    Analytics.notifyViewEvent(expected);
  }
}