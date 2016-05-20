package com.segment.analytics.android.integrations.comscore;


import android.app.Application;
import com.comscore.analytics.comScore;
import com.segment.analytics.Analytics;
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
@PrepareForTest(comScore.class) public class ComScoreTest {

  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  Logger logger;
  @Mock Analytics analytics;
  ComScoreIntegration integration;
  @Mock comScore Comscore;

  @Before public void setUp() {
    initMocks(this);
    mockStatic(comScore.class);
    logger = Logger.with(Analytics.LogLevel.DEBUG);
    when(analytics.logger("ComScore")).thenReturn(Logger.with(VERBOSE));
    when(analytics.getApplication()).thenReturn(context);
    integration = new ComScoreIntegration(analytics, new ValueMap().putValue("customerC2", "foobarbar").putValue("publisherSecret", "illnevertell"));
    mockStatic(comScore.class);
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

    verifyStatic();
    comScore.setAppContext(analytics.getApplication());
    verifyStatic();
    comScore.setCustomerC2("foobarbar");
    verifyStatic();
    comScore.setPublisherSecret("illnevertell");
    verifyStatic();
    comScore.setAppName("testApp");
    verifyStatic();
    comScore.setSecure(true);
    verifyStatic();
    comScore.enableAutoUpdate(2000, true);
  }


  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());

    Properties expected = new Properties().putValue("name", "foo");
    Map<String, String> properties = expected.toStringMap();
    verifyStatic();
    comScore.hidden((HashMap<String, String>) properties);
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
    comScore.hidden(expected);
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
    comScore.setLabels(expected);
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("SmartWatches").category("Purchase Screen").build());

    LinkedHashMap<String, String> expected = new LinkedHashMap<>();
    expected.put("name", "SmartWatches");
    expected.put("category", "Purchase Screen");

    verifyStatic();
    comScore.view(expected);
  }
}