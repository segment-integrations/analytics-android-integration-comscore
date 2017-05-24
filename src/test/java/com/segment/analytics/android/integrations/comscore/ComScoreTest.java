package com.segment.analytics.android.integrations.comscore;

import android.app.Application;
import com.comscore.ClientConfiguration;
import com.comscore.Configuration;
import com.comscore.PartnerConfiguration;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.Analytics;
import com.comscore.streaming.PlaybackSession;
import com.comscore.streaming.StreamingAnalytics;
import com.segment.analytics.Options;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.TrackPayload;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.maven.artifact.ant.shaded.cli.Arg;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.BDDMockito.*;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.support.membermodification.MemberModifier.replace;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Analytics.class) public class ComScoreTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  @Mock Configuration configuration;
  Logger logger;
  @Mock com.segment.analytics.Analytics analytics;
  ComScoreIntegration integration;
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

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    Properties expected = new Properties().putValue("name", "foo");
    Map<String, String> properties = expected.toStringMap();

    verifyStatic();
    Analytics.notifyHiddenEvent(properties);
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
        .properties(new Properties().putValue("asset_id", "1234")
            .putValue("ad_type", "pre-roll")
            .putValue("length", "120")
            .putValue("video_player", "youtube"))
        .build());

    verify(streamingAnalytics).createPlaybackSession();
    verify(streamingAnalytics).getPlaybackSession();

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "1234");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("nst_st_cl", "120");
    expected.put("ns_st_st", "youtube");
    expected.put("c3", "*null");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackStarted() {
    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Started")
        .properties(new Properties().putValue("asset_id", "1234")
            .putValue("ad_type", "pre-roll")
            .putValue("length", "120")
            .putValue("video_player", "youtube"))
        .build());

    verify(streamingAnalytics).createPlaybackSession();
    verify(streamingAnalytics).getPlaybackSession();

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "1234");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("nst_st_cl", "120");
    expected.put("ns_st_st", "youtube");

    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackPaused() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Map<String, Object> options = new LinkedHashMap<>();
    options.put("c3", "abc");


    integration.track(new TrackPayloadBuilder().event("Video Playback Paused")
        .properties(new Properties().putValue("asset_id", "1234")
            .putValue("ad_type", "mid-roll")
            .putValue("length", "100")
            .putValue("video_player", "vimeo")
            .putValue("playbackPosition", "10")
        )
        .options(new Options().setIntegrationOptions("comScore", options))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "1234");
    expected.put("ns_st_ad", "mid-roll");
    expected.put("nst_st_cl", "100");
    expected.put("ns_st_st", "vimeo");
    expected.put("c3", "abc");
    expected.put("c4", "*null");
    expected.put("c6", "*null");

    verify(streamingAnalytics).notifyPause(10);
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackBufferStarted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Buffer Started")
        .properties(new Properties().putValue("asset_id", "7890")
            .putValue("ad_type", "post-roll")
            .putValue("length", "700")
            .putValue("video_player", "youtube"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "7890");
    expected.put("ns_st_ad", "post-roll");
    expected.put("nst_st_cl", "700");
    expected.put("ns_st_st", "youtube");

    verify(streamingAnalytics).notifyBufferStart();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackBufferCompleted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Buffer Completed")
        .properties(new Properties().putValue("asset_id", "1029")
            .putValue("ad_type", "pre-roll")
            .putValue("length", "800")
            .putValue("video_player", "vimeo"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "1029");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("nst_st_cl", "800");
    expected.put("ns_st_st", "vimeo");

    verify(streamingAnalytics).notifyBufferStop();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackSeekStarted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbacksession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbacksession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Seek Started")
        .properties(new Properties().putValue("asset_id", "3948")
            .putValue("ad_type", "mid-roll")
            .putValue("length", "900")
            .putValue("video_player", "youtube"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "3948");
    expected.put("ns_st_ad", "mid-roll");
    expected.put("nst_st_cl", "900");
    expected.put("ns_st_st", "youtube");

    verify(streamingAnalytics).notifySeekStart();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbacksession).setAsset(expected);
  }

  @Test public void videoPlaybackSeekCompleted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Seek Completed")
        .properties(new Properties().putValue("asset_id", "6767")
            .putValue("ad_type", "post-roll")
            .putValue("length", "400")
            .putValue("video_player", "vimeo"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "6767");
    expected.put("ns_st_ad", "post-roll");
    expected.put("nst_st_cl", "400");
    expected.put("ns_st_st", "vimeo");

    verify(streamingAnalytics).notifyEnd();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoPlaybackResumed() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Resumed")
        .properties(new Properties().putValue("asset_id", "5332")
            .putValue("ad_type", "post-roll")
            .putValue("length", "100")
            .putValue("video_player", "youtube"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "5332");
    expected.put("ns_st_ad", "post-roll");
    expected.put("nst_st_cl", "100");
    expected.put("ns_st_st", "youtube");

    verify(streamingAnalytics).notifyPlay();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoContentStarted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Content Started")
        .properties(new Properties().putValue("asset_id", "9324")
            .putValue("title", "Meeseeks and Destroy")
            .putValue("keywords", "Science Fiction")
            .putValue("season", "1")
            .putValue("episode", "5")
            .putValue("genre", "cartoon")
            .putValue("program", "Rick and Morty")
            .putValue("channel", "cartoon network")
            .putValue("full_episode", "true")
            .putValue("airdate", "2014-01-20"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "9324");
    expected.put("ns_st_ep", "Meeseeks and Destroy");
    expected.put("ns_st_ge", "Science Fiction");
    expected.put("ns_st_sn", "1");
    expected.put("ns_st_en", "5");
    expected.put("ns_st_ge", "cartoon");
    expected.put("ns_st_pr", "Rick and Morty");
    expected.put("ns_st_pu", "cartoon network");
    expected.put("ns_st_ce", "true");
    expected.put("ns_st_ddt", "2014-01-20");

    verify(streamingAnalytics).notifyPlay();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoContentCompleted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Content Completed")
        .properties(new Properties().putValue("asset_id", "9324")
            .putValue("title", "Raising Gazorpazorp")
            .putValue("keywords", "Science Fiction")
            .putValue("season", "1")
            .putValue("episode", "7")
            .putValue("genre", "cartoon")
            .putValue("program", "Rick and Morty")
            .putValue("channel", "cartoon network")
            .putValue("full_episode", "true")
            .putValue("airdate", "2014-10-20"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "9324");
    expected.put("ns_st_ep", "Raising Gazorpazorp");
    expected.put("ns_st_ge", "Science Fiction");
    expected.put("ns_st_sn", "1");
    expected.put("ns_st_en", "7");
    expected.put("ns_st_ge", "cartoon");
    expected.put("ns_st_pr", "Rick and Morty");
    expected.put("ns_st_pu", "cartoon network");
    expected.put("ns_st_ce", "true");
    expected.put("ns_st_ddt", "2014-10-20");

    verify(streamingAnalytics).notifyEnd();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoAdStarted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Ad Started")
        .properties(new Properties().putValue("asset_id", "4311")
            .putValue("pod_id", "adSegmentA")
            .putValue("type", "pre-roll")
            .putValue("publisher", "Segment")
            .putValue("length", "120"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "4311");
    expected.put("ns_st_pn", "adSegmentA");
    expected.put("ns_st_ad", "pre-roll");
    expected.put("ns_st_pu", "Segment");
    expected.put("ns_st_cl", "120");

    verify(streamingAnalytics).notifyPlay();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
  }

  @Test public void videoAdCompleted() {
    setupWithVideoPlaybackStarted();
    Mockito.reset(streamingAnalytics);

    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Ad Completed")
        .properties(new Properties().putValue("asset_id", "3425")
            .putValue("pod_id", "adSegmentb")
            .putValue("type", "mid-roll")
            .putValue("publisher", "Adult Swim")
            .putValue("length", "100"))
        .build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "3425");
    expected.put("ns_st_pn", "adSegmentb");
    expected.put("ns_st_ad", "mid-roll");
    expected.put("ns_st_pu", "Adult Swim");
    expected.put("ns_st_cl", "100");

    verify(streamingAnalytics).notifyEnd();
    verify(streamingAnalytics).getPlaybackSession();
    verify(playbackSession).setAsset(expected);
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
