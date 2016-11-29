# hello-android-scala

[Wiki Entry - Android Development](https://github.com/cycle23/cycle23.github.io/wiki/I.-Android-Development)

This started as a generated app from the [Scala on Android Quickstart](http://scala-android.org/quickstart/), with some modifications needed to get it working just right from my dev environment, including ensuring it compiles with JDK 1.7 instead of 1.8.

In progress now on changes to support [Google VR Android SDK](https://github.com/googlevr/gvr-android-sdk), directly using `sbt`. It currently compiles and executes but needs to be adapted to scala functional style as the next stage. (vars? nulls? wtf!)

To try it out, with either a Android SDK v24 capable device or simulated (you can get a simulated going easily with Intelli-J generic + Android plugin or Android Studio):

```
sbt
android:run
```

It should install and run an app that lets you manipulate the Daydream Controller (not 100% sure how to do this without an actual device) and see results in a non-VR page on the device.
