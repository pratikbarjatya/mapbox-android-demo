package com.mapbox.mapboxandroiddemo.analytics;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;

import com.mapbox.mapboxandroiddemo.R;
import com.segment.analytics.Analytics;
import com.segment.analytics.messages.IdentifyMessage;
import com.segment.analytics.messages.ScreenMessage;
import com.segment.analytics.messages.TrackMessage;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * This class abstracts various analytics calls to Segment analytics' Java library.
 */

public class AnalyticsTracker {

  private Context appContext = null;

  private static volatile AnalyticsTracker analyticsInstance;
  private static volatile Analytics analytics;

  private static final String OPENED = "Opened app";

  private static final String SKIPPED_ACCOUNT_BUTTON_EVENT_NAME = "Skipped account creation/login";
  private static final String CLICKED_ON_NAV_DRAWER_SECTION_EVENT_NAME = "Clicked on nav drawer section";
  private static final String CLICKED_ON_INDIVIDUAL_EXAMPLE_EVENT_NAME = "Clicked on individual example";
  private static final String SECTION_NAME_MAP_KEY = "section name";
  private static final String EXAMPLE_NAME_MAP_KEY = "example name";
  private static final String IS_TABLET_MAP_VALUE = "tablet";
  private static final String IS_PHONE_MAP_VALUE = "phone";
  public static final String CLICKED_ON_CREATE_ACCOUNT_BUTTON_SHARED_PREF_KEY = "CLICKED_ON_CREATE_BUTTON";
  public static final String CLICKED_ON_SIGN_IN_BUTTON_SHARED_PREF_KEY = "CLICKED_ON_SIGN_IN_BUTTON";
  private static final String MAPBOX_SHARED_PREFERENCE_KEY_ANALYTICS_ENABLED = "mapboxAnalyticsEnabled";
  private static final String MAPBOX_SHARED_PREFERENCES_FILE = "MapboxSharedPreferences";
  public static String MAPBOX_USERNAME;

  private Boolean analyticsEnabled = null;

  /**
   * Initializes instance of AnalyticsTracker class
   **/
  public static AnalyticsTracker getInstance(@NonNull Context context) {
    if (analyticsInstance == null) {  // Single check
      synchronized (AnalyticsTracker.class) {
        if (analyticsInstance == null) {  // Double check
          analyticsInstance = new AnalyticsTracker();
          analyticsInstance.appContext = context;
          analytics = Analytics.builder(context.getString(R.string.mapbox_segment_write_key)).build();
        }
      }
    }
    return analyticsInstance;
  }

  public void setMapboxUsername() {
    MAPBOX_USERNAME = PreferenceManager.getDefaultSharedPreferences(
        analyticsInstance.appContext).getString("USERNAME", "not logged in");
  }

  /**
   * Gets and adds device information to analytics call. Ideally, this method is called
   * when app is opened for the first time or if shared preferences is cleared.
   **/

  public void openedAppForFirstTime(boolean isTablet) {
    if (isAnalyticsEnabled()) {
      Map<String, String> properties = new HashMap<>();
      properties.put("email", getSharedPreferences(appContext)
          .getString("EMAIL", "not logged in"));
      properties.put("model", Build.MODEL);
      properties.put("brand", Build.BRAND);
      properties.put("product", Build.PRODUCT);
      properties.put("manufacturer", Build.MANUFACTURER);
      properties.put("device", Build.DEVICE);
      properties.put("tags", Build.TAGS);
      properties.put("ISO03 language", Locale.getDefault().getISO3Language());
      properties.put("language", Locale.getDefault().getLanguage());
      properties.put("ISO03 country", Locale.getDefault().getISO3Country());
      properties.put("country", Locale.getDefault().getCountry());
      properties.put("display country", Locale.getDefault().getDisplayCountry());
      properties.put("display name", Locale.getDefault().getDisplayName());
      properties.put("display language", Locale.getDefault().getDisplayLanguage());
      properties.put("size", isTablet ? IS_TABLET_MAP_VALUE : IS_PHONE_MAP_VALUE);

      analytics.enqueue(TrackMessage.builder("New install")
          .userId(MAPBOX_USERNAME)
          .properties(properties)
      );
    }
  }

  /**
   * Makes an analytics call telling Segment that the app was opened. Ideally used
   * in the MapboxApplication class' onCreate(). Could also be called in the MainActivity's onCreate()
   */
  public void openedApp() {
    if (isAnalyticsEnabled()) {
      trackEvent(OPENED, true);
    }
  }

  /**
   * Saves button click status to shared preferences because the clickedOnSignInButton()
   * method above needs to be called after the user signs in so that "null" isn't
   * sent for the userId field.
   */
  public void adjustClickedSignInToStatusSharedPref(boolean currentStatus) {
    PreferenceManager.getDefaultSharedPreferences(appContext).edit()
        .putBoolean(CLICKED_ON_SIGN_IN_BUTTON_SHARED_PREF_KEY, currentStatus)
        .apply();
  }

  /**
   * Saves button click status to shared preferences because the clickedOnCreateAccountButton()
   * method above needs to be called after the user signs in so that "null" isn't
   * sent for the userId field.
   */
  public void adjustClickedCreateAccountStatusSharedPref(boolean currentStatus) {
    PreferenceManager.getDefaultSharedPreferences(appContext).edit()
        .putBoolean(CLICKED_ON_CREATE_ACCOUNT_BUTTON_SHARED_PREF_KEY, currentStatus)
        .apply();
  }

  /**
   * Makes an analytics call telling Segment which particular navigation drawer section has been selected.
   *
   * @param sectionName Name of the selected navigation drawer category
   */
  public void clickedOnNavDrawerSection(@NonNull String sectionName, boolean loggedIn) {
    if (isAnalyticsEnabled()) {
      trackEventWithProperties(CLICKED_ON_NAV_DRAWER_SECTION_EVENT_NAME, SECTION_NAME_MAP_KEY,
          sectionName, loggedIn);
    }
  }

  /**
   * Makes an analytics call telling Segment which particular example has selected
   *
   * @param exampleName Name of the selected example
   */
  public void clickedOnIndividualExample(@NonNull String exampleName, boolean loggedIn) {
    if (isAnalyticsEnabled()) {
      trackEventWithProperties(CLICKED_ON_INDIVIDUAL_EXAMPLE_EVENT_NAME, EXAMPLE_NAME_MAP_KEY, exampleName, loggedIn);
    }
  }

  /**
   * Makes an analytics call telling Segment what custom-named event has happened. Because there are no
   * custom parameters involved with the call, this method is the most "basic" analytics call that's
   * available in this class.
   *
   * @param eventName Name of the event that's being recorded
   */
  public void trackEvent(@NonNull String eventName, boolean loggedIn) {
    if (isAnalyticsEnabled()) {
      trackEventWithProperties(eventName, null, null, loggedIn);
    }
  }

  /**
   * Makes an analytics call telling Segment what custom-named event has happened. Custom parameters provided
   * are also included in the call.
   *
   * @param eventName             Name of the event that's being recorded
   * @param keyForPropertiesMap   Key to the property being attached to the event that's being called
   * @param valueForPropertiesMap Value of the property being attached to the event that's being called
   */
  public void trackEventWithProperties(@NonNull String eventName, String keyForPropertiesMap,
                                       String valueForPropertiesMap, boolean loggedIn) {
    if (isAnalyticsEnabled()) {
      if (keyForPropertiesMap == null || valueForPropertiesMap == null) {
        analytics.enqueue(TrackMessage.builder(eventName)
            .userId(loggedIn ? MAPBOX_USERNAME : "not logged in"));
      }
      if (keyForPropertiesMap != null && valueForPropertiesMap != null) {
        Map<String, String> properties = new HashMap<>();
        properties.put(keyForPropertiesMap, valueForPropertiesMap);

        analytics.enqueue(TrackMessage.builder(eventName).userId(loggedIn ? MAPBOX_USERNAME : "not logged in")
            .properties(properties));
      }
    }
  }

  /**
   * Makes an analytics call telling Segment that a certain screen has been viewed via a "ScreenMessage" call.
   * Ideally called in onCreate(), onCreateView(), etc.
   *
   * @param nameOfScreen Name of the screen/activity that's being viewed
   */
  public void viewedScreen(String nameOfScreen, boolean loggedIn) {
    if (isAnalyticsEnabled()) {
      analytics.enqueue(ScreenMessage.builder(nameOfScreen).userId(loggedIn ? MAPBOX_USERNAME : "not logged in"));
    }
  }

  /**
   * Makes an analytics call telling Segment the user's identity via a "IdentifyMessage" call.
   *
   * @param userEmailAddress Email address associated with user's Mapbox account
   */
  public void identifyUser(@NonNull String userEmailAddress) {
    if (isAnalyticsEnabled()) {
      Map<String, String> traits = new HashMap<>();
      traits.put("email", userEmailAddress);

      analytics.enqueue(IdentifyMessage.builder()
          .userId(MAPBOX_USERNAME)
          .traits(traits)
      );
    }
  }

  /**
   * Returns the opt-out status for the current device and analytics client combination.
   */
  public boolean isAnalyticsEnabled() {
    if (analyticsEnabled == null) {
      // Cache value
      analyticsEnabled = AnalyticsTracker.getSharedPreferences(appContext)
          .getBoolean(AnalyticsTracker.MAPBOX_SHARED_PREFERENCE_KEY_ANALYTICS_ENABLED, true);
    }
    return analyticsEnabled;
  }

  /**
   * Saves the current device's analytics opt-out status to shared preferences.
   *
   * @param analyticsEnabled true to enable analytics tracking, false to disable
   */
  public void optUserIntoAnalytics(boolean analyticsEnabled) {
    this.analyticsEnabled = analyticsEnabled;
    SharedPreferences prefs = AnalyticsTracker.getSharedPreferences(appContext);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putBoolean(AnalyticsTracker.MAPBOX_SHARED_PREFERENCE_KEY_ANALYTICS_ENABLED, analyticsEnabled);
    editor.apply();
  }

  /**
   * Returns the device's shared preferences file.
   */
  public static SharedPreferences getSharedPreferences(Context context) {
    return context.getSharedPreferences(
        AnalyticsTracker.MAPBOX_SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
  }
}