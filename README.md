# PanView
A bidirectional panning container for Android with native scrolling.

What is it?
-----------

In short, PanView is an Android library that provides a container widget that allows the user to pan across a child view in a native fashion. PanView calls upon the native scrolling and layout functionality of both ScrollView and HorizontalScrollView, splicing these containers between itself and its child and rerouting touch events to allow seamless simultaneous scrolling on both axes. Also, because PanView uses native scroll containers, it implicitly fits into any system configuration.

Alright, but why?
-----------------

So, why not just nest a ScrollView within a HorizontalScrollView yourself? This is a fair question. After all, that is basically what PanView does. There is a pretty major problem there, though. These containers are simply not designed to handle touch events that way. When the outermost scroll view detects its respective scroll gesture, it blocks propagation of touch events to its child, even if that child is the other scroll view. This results in a poor user experience, as only one axis may be manipulated at once.

PanView intercepts and redistributes touch events such that both scroll views operate in unison. You should keep in mind that, after window attachment, the PanView is no longer the parent of the child view.

Usage
-----

For convenient installation, use a service such as [JitPack](https://jitpack.io) to pull directly from GitHub.

To use JitPack, enable their Maven repo in the project-level `build.gradle` like this:

```gradle
allprojects {
    repositories {
        jcenter()
        
        // Add the following line
        maven { url "https://jitpack.io" }
    }
}
```

Once that's done, include PanView in a module-level `build.gradle` like this:

```gradle
dependencies {
    // Add the following line
    compile 'com.github.tylerfilla:PanView:v0.0.0'
}
```

Obviously you can select the version to use by modifying the above line.

As far as actually using the library in your application, it's really simple. Use the PanView class just as you would a ScrollView. See the included demo app as an example. Here are the relevant files:

* [activity_main.xml](https://github.com/tylerfilla/PanView/blob/master/demo/src/main/res/layout/activity_main.xml)
* [styles.xml](https://github.com/tylerfilla/PanView/blob/master/demo/src/main/res/values/styles.xml)
* [MainActivity.java](https://github.com/tylerfilla/PanView/blob/master/demo/src/main/java/com/gmail/tylerfilla/widget/panview/demo/MainActivity.java)

The Future
----------

What I plan to do:

* Use this library in my own projects and make revisions/fixes as needed
* Keep the library up-to-date with Android OS and SDK updates (unless a native solution arises and gets backported)

What I do not plan to do:

* Utilize a formal testing or build system
* Release updates on a scheduled basis

License
-------

Copyright (c) 2016 Tyler Filla  
This software may be modified and distributed under the terms of [the MIT license](https://opensource.org/licenses/MIT). See the [LICENSE](https://github.com/tylerfilla/PanView/blob/master/LICENSE) file for details.
