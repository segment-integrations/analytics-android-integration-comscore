package com.segment.analytics.android.integrations.comscore;


import android.app.Application;
import android.app.usage.UsageStatsManager;

import com.comscore.PublisherConfiguration;
import com.comscore.UsagePropertiesAutoUpdateMode;
import com.comscore.Analytics;
import com.segment.analytics.Properties;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Logger;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Analytics.LogLevel.VERBOSE;
import static com.segment.analytics.Utils.createTraits;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareForTest(Analytics.class) public class ComScoreTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  Logger logger;
  @Mock
  com.segment.analytics.Analytics analytics;
  ComScoreIntegration integration;
  @Mock Analytics Comscore;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(Analytics.class);
    logger = Logger.with(com.segment.analytics.Analytics.LogLevel.DEBUG);
    when(analytics.logger("ComScore")).thenReturn(Logger.with(VERBOSE));
    when(analytics.getApplication()).thenReturn(context);
    integration = new ComScoreIntegration(analytics, new ValueMap().putValue("customerC2", "foobarbar").putValue("publisherSecret", "illnevertell"));
    mockStatic(Analytics.class);
  }

  @Test public void factory() {
    ValueMap settings = new ValueMap() //
        .putValue("c2", "foobarbar").putValue("publisherSecret", "illnevertell");
    integration =
        (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);
    verifyStatic();
    assertThat(integration.customerC2).isEqualTo("foobarbar");
    assertThat(integration.publisherSecret).isEqualTo("illnevertell");
  }

  @Test public void initializeWithDefaultArguments() {
    ValueMap settings = new ValueMap() //
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("setSecure", true);
    ComScoreIntegration integration =
        (ComScoreIntegration) ComScoreIntegration.FACTORY.create(settings, analytics);
    verifyStatic();
    assertThat(integration.customerC2).isEqualTo("foobarbar");
    assertThat(integration.publisherSecret).isEqualTo("illnevertell");
    assertThat(integration.useHTTPS).isTrue();
  }

  @Test public void initializeWithAutoUpdateMode() throws IllegalStateException {
    integration = new ComScoreIntegration(analytics, new ValueMap() //
        .putValue("c2", "foobarbar")
        .putValue("publisherSecret", "illnevertell")
        .putValue("appName", "testApp")
        .putValue("useHTTPS", true)
        .putValue("autoUpdateInterval", 2000)
        .putValue("autoUpdate", true)
        .putValue("foregroundOnly", true));

    PublisherConfiguration.Builder builder = new PublisherConfiguration.Builder();
    verifyStatic();
    builder.publisherId("foobarbar");
    verifyStatic();
    builder.publisherSecret("illnevertell");
    verifyStatic();
    builder.applicationName("testApp");
    verifyStatic();
    builder.secureTransmission(true);
    verifyStatic();
    builder.usagePropertiesAutoUpdateMode(UsagePropertiesAutoUpdateMode.FOREGROUND_AND_BACKGROUND);
  }


  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    Properties expected = new Properties().putValue("name", "foo");
    Map<String, String> properties = expected.toStringMap();
    verifyStatic();
    Analytics.notifyHiddenEvent((HashMap<String, String>) properties);
  }

  @Test public void trackWithProps() {
    integration.track(new TrackPayloadBuilder() //
        .event("Completed Order")
        .properties(new Properties()
            .putValue(20.0)
            .putValue("product","Ukelele")
        ).build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "Completed Order");
    expected.put("value", "20.0");
    expected.put("product", "Ukelele");

    verifyStatic();
    Analytics.notifyHiddenEvent(expected);
  }

  @Test public void identify() throws JSONException {
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

    verifyStatic();
      Analytics.getConfiguration().setPersistentLabels(expected);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("SmartWatches").category("Purchase Screen").build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "SmartWatches");
    expected.put("category", "Purchase Screen");

    verifyStatic();
      Analytics.notifyViewEvent(expected);
  }
}