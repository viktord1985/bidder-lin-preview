package com.linked.bidder.utils;

// LinkedIn (Basic) allows 100–200 connections per week

public class Config {
    // UI config
    public static double leftSideSize = 0.2;
    // App Version
    public static String appVersion = "version:2.0.2";
    // openAI token
    public static String openAIToken = "sk-proj-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
    //Note: removed because the feed is always different, the algorithm keeps inserting irrelevant posts, and it differs per account
    //Old: skip scan if found same post IDs
    //Old: public static boolean skipOnSamePostFeed = true;
    // ------------------------
    // max profiles per run (per-run limit, not per-day)
    public static int maxOpenedProfilesPerRun = 6;
    public static int maxOpenedProfilesPerRunOld = 18;
    // max sessions per day
    public static int maxOpenedSessionsPerDay = 6;
    public static int maxOpenedSessionsPerDayOld = 18;
    // max profiles bot bid per_day / per_run
    public static int maxProfilesBotBidPerDay = 4;
    // old: set to the lower bound (100 per week / 14 per day)
    // new: set to 4, since there were no profile bans at this value
    public static int maxProfilesBotBidPerRun = maxProfilesBotBidPerDay / 2;
    // was 100 (seemed to work fine without bans), reduced to 63, it took too long
    public static int maxPostsPerRun = 63;
    // ------------------------
    // incoming connections config
    // auto accept connection mode
    public static Boolean isAutoAcceptConnection = true;
    // config for sending referrals and searching the suggested list
    // auto accept connection mode
    public static Boolean isAutoSendReferAndAutoAddSuggested = true;
    // max attempt to find correct suggested profile
    public static int maxAttemptsFindSuggestedProfile = 1;
    public static int maxAttemptsFindSuggestedProfileOld = 2;
    // ------------------------
    // new connections processing config
    public static int maxConnectsPerRun = 1;
    public static int maxConnectsPerRunOld = 2;
    // ------------------------
    // connections auto-cleanup config
    // auto clean connection mode
    public static Boolean isAutoCleanConnection = true;
    public static Double isAutoCleanConnectionProbability = 0.2;
    public static int activeWeeksForRunCleanConnection = 4;
    //Note: anything not in the list has to be cleaned up manually
    public static String[] cleanConnectionAge = {"2 month", "3 month", "4 month", "5 month", "6 month", "7 month", "8 month", "9 month", "10 month", "11 month"};
    public static int maxCleanConnectionPerRun = 1;
    public static int maxCleanConnectionPerRunOld = 2;
    // ------------------------
    // post publishing config
    // auto post content mode
    public static Boolean isAutoPostContent = true;
    // max content post per day
    public static int maxContentPostPerDay = 1;
    public static int maxContentPostPerDayOld = 2;
    // ------------------------
    // profile adding config
    // auto post content mode
    public static Boolean isAutoConnectProfiles = false; // disabled: not worth it, untested for a long time, may not work
    // max content post per day
    public static int maxConnectProfilesPerDay = 0;
    public static int maxConnectProfilesPerDayOld = 2;
    // ------------------------
    // defines after how many days a profile becomes old
    public static int minOldProfileInterval = 14;
    // ------------------------
    // defines the minimum interval between posts in hours
    public static int minPostInterval = 4;
    // ------------------------
    // search query selection mode
    public static Boolean isRandomSearchQuery = false;
    // ------------------------
    // Analytics Actions
    public static Boolean isAutoAnalyticsEnable = false;
    // ------------------------
    // Jobs Scanner
    public static int jobsMaxPage = 3;
}
