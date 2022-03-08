
# <img src="graphics/logo.png" width="50px" alt=""></img> Native Alpha
![OS](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white&style=plastic)
![SDK](https://img.shields.io/badge/SDK-30-yellowgreen)
[![GitHub release](https://img.shields.io/github/v/release/cylonid/NativeAlphaForAndroid?include_prereleases&color=blueviolet)](https://github.com/cylonid/NativeAlphaForAndroid/releases)
[![Github all releases](https://img.shields.io/github/downloads/cylonid/NativeAlphaForAndroid/total?color=blue&label=GitHub%E2%87%A9&style=plastic)](https://somsubhra.github.io/github-release-stats/?username=cylonid&repository=NativeAlphaForAndroid&page=1&per_page=10)
[![GitHub license](https://img.shields.io/github/license/cylonid/NativeAlphaForAndroid?color=orange)](https://github.com/cylonid/NativeAlphaForAndroid/blob/master/LICENSE)
![Maintenance](https://img.shields.io/badge/Maintained%3F-yes-green.svg)


## Features
  * Shows any website in a borderless full-screen window using Android System WebView.
  * Create home screen shortcuts and retrieves icons in suitable resolution.
  * Various settings (JavaScript, Cookies, adblocking, location access) can be set for every web app individually
  * Navigation with multi-touch gestures while browsing.
  * Opt-in adblock using an AdBlock Plus custom webview.
  * Less memory footprint and no privacy-invading app permissions in comparison to native apps
  * Dark mode for Android 10+

## Pro Features
  * Sandbox containers: Web Apps are loaded in fully separated sandboxes, cookies or other data are not shared with other Web Apps
  * Experimental dark mode also available for websites (configurable)
  * True fullscreen mode ("immersive") without any menu bars
 
## Download Options
[![IzzyOnDroid Download Badge](graphics/IzzyOnDroid.png)](https://apt.izzysoft.de/fdroid/index/apk/com.cylonid.nativealpha)
[![APK Download Badge](graphics/apk_badge.png)](https://github.com/cylonid/NativeAlphaForAndroid/releases/download/v1.01/NativeAlpha-standard-universal-release-v1.1.apk)

## Latest Changes (v1.1)
* Support for mailto and tel hyperlinks
* Support for file picker dialogs
* Support for DRM-protected content
* Enhanced support for video playback
* Enabled for usage with non-internet addresses
* Color theme will from now on always follow system settings

## FAQ
*Q: Why would I need this app if any mobile browser can do the same?*

A: Mobile browsers usually only are able to create shortcuts which give a native, borderless fullscreen experience if the website has a Progressive Web App (PWA) manifest. Unfortunately, most websites do not offer this feature yet. Additionally, you cannot set different settings for different websites with an usual browser.

*Q: Is this a web browser?*

A: No. As stated, this app relies on the system built-in Android WebView in order to display the website. For privacy reasons, you can opt to use alternative webviews such as [Bromite](https://www.bromite.org/system_web_view) on rooted phones. Always make sure to use to most recent version of any WebView implementation you use!

*Q: Why isn't the sandbox feature enabled by default?*

A: The sandboxing approach is recommended for specific usage rather than general usage because it can limit the performance of the application and increase the disk usage. Therefore, use it for privacy-invasive websites or websites where you want to be logged in twice, but not for any website just because you can.

*Q: Why is it not possible to find an icon for a certain website?*

A: This problem can occur due to multiple reasons. In most cases, the website does not offer a high-resolution icon. If you are a website maintainer and your website icon cannot be found, look at [RealFaviconGenerator](https://realfavicongenerator.net) for further information. If you think it should work, feel free to post the URL and I will look into it.


*Q: In constrast to your promise, this app has a large memory footprint!*

A: This is because Native Alpha makes use of caching in the same way your browser app does, i.e., it saves web content locally on your device. Then it can be loaded faster if you visit the same page again. You can either delete cache regularly yourself or set the "Clear cache after usage" setting in the global settings if memory footprint is a concern for you. However, then websites will take a longer time to load because everything has to be loaded from net.

## Used libraries/resources
* [CircularProgressBar](https://github.com/lopspower/CircularProgressBar)
* [JSoup](https://jsoup.org/)
* [AboutPage](https://github.com/medyo/android-about-page)
* [AdBlock+WebView](https://github.com/adblockplus/libadblockplus-android)
* [MovableFloatingActionButton](https://stackoverflow.com/questions/46370836/android-movable-draggable-floating-action-button-fab)
* [Android About Page](https://github.com/medyo/android-about-page)
* [Android Databinding](https://developer.android.com/topic/libraries/data-binding)
* [AboutLibraries](https://github.com/mikepenz/AboutLibraries)

For testing purposes:
* [Robolectric](https://github.com/robolectric/robolectric)
* [Espresso](https://developer.android.com/training/testing/espresso/)

A list of used open-source libraries can also be found inside the app ("About" section).

## Screenshots
<div style="text-align: center; margin: auto;">
<img src="graphics/sc2.png" alt="Native Alpha Logo" width="350"/>
<img src="graphics/sc3.png" alt="Native Alpha Logo" width="350"/>
<img src="graphics/sc4.png" alt="Native Alpha Logo" width="350"/>
<img src="graphics/sc5.png" alt="Native Alpha Logo" width="350"/>
</div>


## License
Native Alpha is Free Software: You can use, study share and improve it at your
will. Specifically you can redistribute and/or modify it under the terms of the
[GNU General Public License](https://www.gnu.org/licenses/gpl.html) as
published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

## End User License Agreement
THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
