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

  @Test public void videoPlaybackStarted() {
    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayloadBuilder().event("Video Playback Started")
        .properties(new Properties()
            .putValue("asset_id", "1234")
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
    PlaybackSession playbackSession = mock(PlaybackSession.class);
    when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.streamingAnalytics = streamingAnalytics;

    integration.track(new TrackPayloadBuilder().event("Video Playback Paused")
        .properties(new Properties()
            .putValue("asset_id", "1234")
            .putValue("ad_type", "mid-roll")
            .putValue("length", "100")
            .putValue("video_player", "vimeo"))
        .build());

    verify(streamingAnalytics).notifyPause();
    verify(streamingAnalytics).getPlaybackSession();

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("ns_st_ci", "1234");
    expected.put("ns_st_ad", "mid-roll");
    expected.put("nst_st_cl", "100");
    expected.put("ns_st_st", "vimeo");

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
