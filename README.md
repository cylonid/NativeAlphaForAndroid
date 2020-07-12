#  Native Alpha for Android
<img src="graphics/logo.png" alt="Native Alpha Logo" width="200"/>

## Features
  * Show any website in a borderless full-screen window using Android System WebView.
  * Offers to create home screen shortcuts and retrieves icons in suitable resolution.
  * Various settings (JavaScript, Cookies, Third-Party-Cookies, Caching) can be set for every web app individually
  * Navigation with multi-touch gestures while browsing.
  * Opt-in adblock using an AdBlock Plus custom webview.
  * Less memory footprint and no privacy-invading app permissions in comparison to native apps



## FAQ
*Q: Why is it not possible to find an icon for a certain website?*

A: This problem can occur due to multiple reasons. In most cases, the website does not offer a high-resolution icon. If you are a website maintainer and your website icon cannot be found, look at realfavicongenerator.net for further information. If you think it should work, feel free to post the URL and I will look into it.

*Q: Why would I need this app if any mobile browser can do the same?*

A: Mobile browser can only produce shortcuts which give a native, borderless fullscreen experience if the website has a Progressive Web App (PWA) manifest. Unfortunately, most websites do not offer this feature yet.

*Q: Is this a web browser?*

A: No. As stated, this app relies on the system built-in Android WebView in order to display the website. For privacy reasons, you can opt to use alternative webviews such as Bromite on rooted phones.

*Q: Why does this app require Android Oreo?*

A: Android introduced a new shortcut API with Oreo. I will try to support legacy shortcuts later, then Nougat and Marshmallow should also work.

*Q: In constrast to your promise, this app has a huge memory footprint!*

A: This is because Native Alpha makes use of caching in the same way your browser app does, i.e., it saves web content locally on your device. Then it can be loaded faster if you visit the same page again. I will look into a way for better cache management. In the meantime, you can either delete cache regularly yourself or set the "Clear cache after usage" setting in the global settings if memory footprint is a concern for you. However, then websites will take a longer time to load because everything has to be loaded from internet.

## Used libraries/resources
* [CircularProgressBar](https://github.com/lopspower/CircularProgressBar)
* [JSoup](https://jsoup.org/)
* [AboutPage](https://github.com/medyo/android-about-page)
* [AdBlock+WebView](https://github.com/adblockplus/libadblockplus-android)
* [MovableFloatingActionButton](https://stackoverflow.com/questions/46370836/android-movable-draggable-floating-action-button-fab)
* [Android About Page](https://github.com/medyo/android-about-page)

A list of used open-source libraries can also be found inside the app ("About" section).

## License
Native Alpha is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
