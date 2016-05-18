package com.segment.analytics.android.integrations.comscore;

import com.comscore.analytics.comScore;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.Logger;


public class ComScoreIntegration extends Integration<Void> {
  public static final Factory FACTORY = new Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      return new ComScoreIntegration(analytics, settings);
    }

    @Override public String key() {
      return COMSCORE_KEY;
    }
  };
  private static final String COMSCORE_KEY = "ComScore";
  private final Logger logger;

  ComScoreIntegration(Analytics analytics, ValueMap settings) {
    logger = analytics.logger(COMSCORE_KEY);

    comScore.setAppContext(analytics.getApplication());

    String customerC2 = settings.getString("customerC2");
    comScore.setCustomerC2(customerC2);
  }
}
