# ImageGalleryApp
ImageGalleryApp is a simple image gallery app to showcase building an Android app with [Kotlin](https://kotlinlang.org). It queries the media library with [CursorLoader](https://developer.android.com/reference/android/support/v4/content/CursorLoader.html) (in order to follow the recommended practice of querying the database off the main thread) to show all the available images on the user's device.

It is currently a Work In Progress as more features are still coming.

Packaging (of classes) was done by feature, so as to ease navigation while developing and also to ease project maintainability.

[Material Design Guidelines](https://material.io) were also followed.

### Libraries used
* [PhotoView](https://github.com/chrisbanes/PhotoView)
* [Glide](https://github.com/bumptech/Glide)
* [Anko](https://github.com/Kotlin/anko)
