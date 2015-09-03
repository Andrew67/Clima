DdrFinder-legacy
================

Native Android version of [ddr-finder](https://github.com/Andrew67/ddr-finder) with Google Maps integration.

See [DdrFinder](https://github.com/Andrew67/DdrFinder) for the new Android Studio-based project.

License
-------
MIT license (with some exceptions; see LICENSE).

Developing your own version
---------------------------
* Request a Google Maps V2 API Key from the [Google API Developer Console](https://code.google.com/apis/console/).
* Modify AndroidManifest.xml and add the new key.
* Modify res/values/analytics.xml with your own Google Analytics tracking ID (or remove it if you have none).
* Deploy your own version of [ddr-finder](https://github.com/Andrew67/ddr-finder) (this app will always be compatible with version 1.0 deployments).
* Modify LOADER_API_URL in com.andrew67.ddrfinder.adapters.MapLoader to point to your API endpoint.
* Modify about_url in res/values/strings.xml to point to your about page.

Acknowledgments
---------------
* [Zenius -I- vanisher.com](http://zenius-i-vanisher.com/) for inspiring me to make this.
* All acknowledgments listed in the [ddr-finder page](https://github.com/Andrew67/ddr-finder#acknowledgments).
