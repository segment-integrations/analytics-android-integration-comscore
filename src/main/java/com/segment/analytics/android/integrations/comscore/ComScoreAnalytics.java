package com.segment.analytics.android.integrations.comscore;

import android.content.Context;

import com.comscore.Analytics;
import com.comscore.PartnerConfiguration;
import com.comscore.PublisherConfiguration;
import com.comscore.streaming.StreamingAnalytics;
import com.segment.analytics.integrations.Logger;

import java.util.Map;

/**
 * This is a wrapper to all ComScore components. It helps with testing since
 * ComScore relays heavily on JNI and static classes, and all needs to be
 * mocked in any case.
 */
public interface ComScoreAnalytics {

    /**
     * Creates a new streaming analytics session.
     * @return The new session.
     */
    public StreamingAnalytics createStreamingAnalytics();

    /**
     * Starts collecting analytics with the provided client configuration
     * @param context Application context
     * @param partnerId Partner ID
     * @param publisher Publisher configuration
     */
    public void start(Context context, String partnerId, PublisherConfiguration publisher);

    /**
     * Sets global labels
     * @param labels Labels.
     */
    public void setPersistentLabels(Map<String, String> labels);

    /**
     * Sends an view event with the provided properties.
     * @param properties Event properties.
     */
    public void notifyViewEvent(Map<String, String> properties);

    /**
     * Sends an unmapped event with the provided properties.
     * @param properties Event properties.
     */
    public void notifyHiddenEvent(Map<String, String> properties);

    /**
     * Default implementation of ComScoreAnalytics. It uses the methods and classes
     * provided by the ComScore SDK.
     */
    public class DefaultcomScoreAnalytics implements ComScoreAnalytics {

        private Logger logger;

        public DefaultcomScoreAnalytics() {
            logger = Logger.with(com.segment.analytics.Analytics.LogLevel.NONE);
        }

        public DefaultcomScoreAnalytics(Logger logger) {
            this.logger = logger;
        }

        @Override
        public StreamingAnalytics createStreamingAnalytics() {
            logger.verbose("Creating streaming analytics");
            return new StreamingAnalytics();
        }

        @Override
        public void start(Context context, String partnerId, PublisherConfiguration publisher) {

            PartnerConfiguration partner = new PartnerConfiguration.Builder().partnerId(partnerId).build();

            logger.verbose("Adding partner (%s)", partner);
            Analytics.getConfiguration().addClient(partner);

            logger.verbose("Adding publisher (%s)", publisher);
            Analytics.getConfiguration().addClient(publisher);

            logger.verbose("Starting ComScore Analytics");

            Analytics.start(context);
        }

        @Override
        public void setPersistentLabels(Map<String, String> labels) {
            logger.verbose("Analytics.getConfiguration().addPersistentLabels(%s)", labels);
            Analytics.getConfiguration().addPersistentLabels(labels);
        }

        @Override
        public void notifyViewEvent(Map<String, String> properties) {
            logger.verbose("Analytics.notifyViewEvent(%s)", properties);
            Analytics.notifyViewEvent(properties);
        }

        @Override
        public void notifyHiddenEvent(Map<String, String> properties) {
            logger.verbose("Analytics.notifyHiddenEvent(%s)", properties);
            Analytics.notifyHiddenEvent(properties);
        }
    }
}
