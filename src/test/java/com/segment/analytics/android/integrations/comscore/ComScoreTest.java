package com.segment.analytics.android.integrations.comscore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.Application;

import com.comscore.PartnerConfiguration;
import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.streaming.Asset;
import com.comscore.streaming.PlaybackSession;
import com.comscore.streaming.StreamingAnalytics;

import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class ComScoreTest {

  @Mock Application context;
  @Mock ComScoreAnalytics comScoreAnalytics;
  @Mock com.segment.analytics.Analytics analytics;
  @Mock StreamingAnalytics streamingAnalytics;

  private Logger logger;
  private ComScoreIntegration integration;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    Mockito.when(comScoreAnalytics.createStreamingAnalytics()).thenReturn(streamingAnalytics);

    ValueMap settings = new ValueMap();
    settings.putValue("customerC2", "foobarbar");
    settings.putValue("publisherSecret", "illnevertell");

    logger = Logger.with(com.segment.analytics.Analytics.LogLevel.VERBOSE);
    Mockito.when(analytics.logger("comScore")).thenReturn(logger);
    Mockito.when(analytics.getApplication()).thenReturn(context);
    integration = new ComScoreIntegration(analytics, settings, comScoreAnalytics);
  }

  @Test
  public void initialize() {
    ValueMap settings =
        new ValueMap().putValue("c2", "foobarbar").putValue("publisherSecret", "illnevertell");

    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);

    assertEquals("foobarbar", integration.customerC2);
    assertEquals("illnevertell", integration.publisherSecret);
  }

  @Test
  public void initializeWithSettings() {
    ValueMap settings = new ValueMap() //
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("setSecure", true);

    integration = (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);

    assertEquals("foobarbar", integration.customerC2);
    assertEquals("illnevertell", integration.publisherSecret);
    assertTrue(integration.useHTTPS);
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

    ArgumentCaptor<PartnerConfiguration> partnerCaptor =
        ArgumentCaptor.forClass(PartnerConfiguration.class);

    ArgumentCaptor<PublisherConfiguration> publisherCaptor =
            ArgumentCaptor.forClass(PublisherConfiguration.class);

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).start(Mockito.eq(context), partnerCaptor.capture(), publisherCaptor.capture());

    System.out.println(partnerCaptor.getValue());

    // Can't get some Ids because they are not exposed in the current SDK.
    //PartnerConfiguration partner = (PartnerConfiguration) partnerCaptor.getValue();
    //assertEquals("24186693", partner.getPartnerId());

    PublisherConfiguration publisher = publisherCaptor.getValue();
    //assertEquals("foobarbar", publisher.getPublisherId());
    //assertEquals("illnevertell", publisher.getPublisherSecret());
    assertEquals("testApp", publisher.getApplicationName());
    assertEquals(2000, publisher.getUsagePropertiesAutoUpdateInterval());
    assertEquals(UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND, publisher.getUsagePropertiesAutoUpdateMode());
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

    ArgumentCaptor<PartnerConfiguration> partnerCaptor =
            ArgumentCaptor.forClass(PartnerConfiguration.class);

    ArgumentCaptor<PublisherConfiguration> publisherCaptor =
            ArgumentCaptor.forClass(PublisherConfiguration.class);

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).start(Mockito.eq(context), partnerCaptor.capture(), publisherCaptor.capture());

    // Can't get some Ids because they are not exposed in the current SDK.
    //PartnerConfiguration partner = (PartnerConfiguration) partnerCaptor.getValue();
    //assertEquals("24186693", partner.getPartnerId());

    PublisherConfiguration publisher = publisherCaptor.getValue();
    //assertEquals("foobarbar", publisher.getPublisherId());
    //assertEquals("illnevertell", publisher.getPublisherSecret());
    assertEquals("testApp", publisher.getApplicationName());
    assertEquals(60, publisher.getUsagePropertiesAutoUpdateInterval());
    assertEquals(UsagePropertiesAutoUpdateMode.DISABLED, publisher.getUsagePropertiesAutoUpdateMode());
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

  void setupWithVideoPlaybackStarted() {
    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).createPlaybackSession();
    Mockito.verify(streamingAnalytics).getPlaybackSession();
    Mockito.verify(playbackSession).setAsset(contentIdMapper);

    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackStarted() {
    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Started")
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

    Mockito.verify(streamingAnalytics).createPlaybackSession();
    Mockito.verify(streamingAnalytics).getPlaybackSession();
    Mockito.verify(playbackSession).setAsset(contentIdMapper);

    Mockito.verify(streamingAnalytics).setLabels(expected);
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

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyPause(10);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackBufferStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyBufferStart(20);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackBufferCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Buffer Completed")
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

    Mockito.verify(streamingAnalytics).notifyBufferStop(30);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackSeekStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbacksession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbacksession);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Playback Seek Started")
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

    Mockito.verify(streamingAnalytics).notifySeekStart(40);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackSeekCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyPlay(50);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoPlaybackResumed() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyPlay(60);
    Mockito.verify(streamingAnalytics).setLabels(expected);
  }

  @Test
  public void videoContentStartedWithDigitalAirdate() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyPlay(70);
    Mockito.verify(playbackSession).setAsset(expected);
  }

  @Test
  public void videoContentStartedWithTVAirdate() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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
            .putValue("fullEpisode", true)
            .putValue("podId", "segment A")
            .putValue("totalLength", "120")
            .putValue("playbackPosition", 70))
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

    Mockito.verify(streamingAnalytics).notifyPlay(70);
    Mockito.verify(playbackSession).setAsset(expected);
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

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);


    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

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

    Mockito.verify(streamingAnalytics).notifyPlay(70);
  }

  @Test
  public void videoContentPlayingWithAdType() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);
    Mockito.when(asset.containsLabel("ns_st_ad")).thenReturn(true);

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

    Mockito.verify(playbackSession).setAsset(expected);
    Mockito.verify(streamingAnalytics).notifyPlay(70);
  }

  @Test
  public void videoContentCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

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

    Mockito.verify(streamingAnalytics).notifyEnd(80);
  }

  @Test
  public void videoAdStarted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

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

    Mockito.verify(streamingAnalytics).notifyPlay(0);
    Mockito.verify(playbackSession).setAsset(expected);
  }

  @Test
  public void videoAdStartedWithContentId() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);
    Mockito.when(asset.getLabel("ns_st_ci")).thenReturn("1234");

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

    Mockito.verify(streamingAnalytics).notifyPlay(0);
    Mockito.verify(playbackSession).setAsset(expected);
  }

  @Test
  public void videoAdStartedWithAdClassificationType() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    Asset asset = Mockito.mock(Asset.class);
    Mockito.when(streamingAnalytics.getPlaybackSession().getAsset()).thenReturn(asset);

    Map<String, Object> comScoreOptions = new LinkedHashMap<>();
    comScoreOptions.put("adClassificationType", "va14");

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Started")
        .properties(new Properties().putValue("assetId", 4311)
            .putValue("podId", "adSegmentA")
            .putValue("type", "pre-roll")
            .putValue("totalLength", 120)
            .putValue("playbackPosition", 0)
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

    Mockito.verify(streamingAnalytics).notifyPlay(0);
    Mockito.verify(playbackSession).setAsset(expected);
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

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Playing")
        .properties(new Properties().putValue("assetId", 4311)
            .putValue("podId", "adSegmentA")
            .putValue("type", "pre-roll")
            .putValue("totalLength", 120)
            .putValue("playbackPosition", 20)
            .putValue("title", "Helmet Ad"))
        .build());

    Mockito.verify(streamingAnalytics).notifyPlay(20);
  }

  @Test
  public void videoAdCompleted() {
    setupWithVideoPlaybackStarted();

    PlaybackSession playbackSession = Mockito.mock(PlaybackSession.class);
    Mockito.when(streamingAnalytics.getPlaybackSession()).thenReturn(playbackSession);

    integration.track(new TrackPayload.Builder().anonymousId("foo").event("Video Ad Completed")
        .properties(new Properties().putValue("assetId", 3425)
            .putValue("podId", "adSegmentb")
            .putValue("type", "mid-roll")
            .putValue("totalLength", 100)
            .putValue("playbackPosition", 100)
            .putValue("title", "Helmet Ad"))
        .build());

    Mockito.verify(streamingAnalytics).notifyEnd(100);

  }

  @Test
  public void identify() {
    Traits traits = new Traits();
    traits.putValue("firstName", "Kylo");
    traits.putValue("lastName", "Ren");

    integration.identify(new IdentifyPayload.Builder().userId("foo").anonymousId("foobar").traits(traits).build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("anonymousId", "foobar");
    expected.put("firstName", "Kylo");
    expected.put("lastName", "Ren");
    expected.put("userId", "foo");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).setPersistentLabels(expected);
  }

  @Test
  public void screen() {
    integration.screen(
        new ScreenPayload.Builder().anonymousId("foo").name("SmartWatches").category("Purchase Screen").build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "SmartWatches");
    expected.put("category", "Purchase Screen");

    Mockito.verify(comScoreAnalytics, Mockito.times(1)).notifyViewEvent(expected);
  }
}
