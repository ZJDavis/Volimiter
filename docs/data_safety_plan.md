# Volimiter Data Safety Plan

Last updated: May 11, 2026

This document summarizes the intended Google Play Data safety declarations for Volimiter.

## Current App Behavior

Volimiter does not transmit user data off the device.

Volimiter stores local app settings, including:

- Maximum speaker volume setting
- Service active/inactive state
- PIN-protected app control state
- Restart-after-boot state

## Data Collection

Planned declaration:

Volimiter does not collect or share user data.

This assumes the app continues to use no analytics, ads, crash reporting, accounts, cloud sync, remote logging, or third-party SDKs that collect data.

## Data Sharing

Volimiter does not share user data with third parties.

## Data Security

Volimiter does not transmit user data to a server.

The PIN is stored locally and protected using Android security features.

## Account Creation

Volimiter does not allow users to create an account.

## Data Deletion

Volimiter stores data locally. Users may delete local app data by clearing app storage or uninstalling the app after disabling Device Admin access.

## Permissions

Volimiter may use:

- Foreground service permission
- Foreground service media playback permission
- Receive boot completed permission
- Device Admin access