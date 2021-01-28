# CHANGELOG

## Version 0.85.1
* Fixed an issue where the app could not restore a backup properly
* Fixed an issue where an unavailable favicon led to a crash during favicon retrieval 
## Version 0.85
* Custom icons supported
* Progress bar indicating reload added
* Custom user-agent option added
* Periodical page refresh option added
* Several minor fixes, e.g., new LibAdblockWebView version (causing a larger APK size)

## Version 0.84
* Web App label on mainscreen can be changed by user
* File downloads are supported
* Location access is supported (experimental)

## Version 0.83
* Multiple Web Apps will be kept open in background (similar to opened tabs in a mobile browser). This behaviour is limited by system settings and your device's RAM.
* Backup & Restore of Native Alpha settings + Web Apps
* Refined "Go Back" behaviour to get back to Native Alpha Main Screen from an openend Web App.
* New multi-touch gesture (two-finger down) to reload page.
 
## Version 0.82.1
* Removed Google Play Services dependency for displaying used open-source libraries to comply with F-Droid policy

## Version 0.82
* HTTP connection handling: Added an option "Allow HTTP" on Web App level. Upon establishing a HTTP connection for the first time, the user gets a prompt and can decide to allow HTTP within the currently opened Web App (i.e., the user will not get prompted again).
* SSL error handling: Alert dialog is shown, the user is advised to leave.
* Data savings: Added options to send "Save-data" HTTP header and to block the loading of images.
* Redesigned and restructured the Web App Settings + Global Settings page.
* Added PayPal donation button to About page.


