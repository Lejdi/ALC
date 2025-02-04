# ALC
Music player for Android OS (library and app)

## Library

Library provides simple way to implement music player service in application. The features include setting entire playlist, changing playback mode and handling external actions such as unplug headphone, incoming phone call or handle physical media buttons on headphones or bluetooth devices.

## Requirements
- sdk Version 28
- Manifest:
```
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
```
```
<application
...
        <service android:name="pl.lejdi.alcmusicplayer.service.MusicService"/>
...
>
```

- build.gradle:
```
implementation "com.orhanobut:hawk:2.0.1"
implementation 'androidx.localbroadcastmanager:localbroadcastmanager:1.0.0'
implementation "androidx.lifecycle:lifecycle-service:2.2.0"
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9'
```

- also, if you want to use default notification provided by ALC, you have to register another BroadcastReceiver in your application:

```
<receiver android:name="pl.lejdi.alcmusicplayer.service.MusicService$Companion$PendingIntentsForwarder"/>
```

## Dependency

Add to your module's build.gradle file:

```
repositories {
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/Lejdi/ALC")
        credentials {
            username = YOUR_GITHUB_USERNAME
            password = YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
        }
    }
}
```
and then in your dependencies:

```
dependencies {
...
    implementation 'pl.lejdi.alcmusicplayer:alcmusicplayer:1.2.0'
}
```

### Usage

AlcProvider is a class that provides access to music player service.

##### AlcProvider(context : Context)

Constructor takes a context as a parameter.

#### setPlaylist(files : MutableList<File>)

Function for setting playlist in music player. Recommended way to provide files is [FileBrowser](https://github.com/Lejdi/FileBrowser) library.

#### setNotification(id: Int, notification: Notification)

Function for setting notification that will allow playback in background.  
If none is provided, ALC uses default notification from [NotifProvider](https://github.com/Lejdi/NotifProvider) library.

#### setMode(mode : Mode)

Function allows setting mode of playback. There are two modes available:
- ` Mode.ALPHABETICAL ` - songs are played in alphabetical order
- ` Mode.RANDOM ` - songs are played in well designed random order

#### getMode() : Mode

Function that returns mode actually set in ALC.

#### fun start(file: File?)

Starting playback from file given in the argument. If file is null, ALC starts playback from the first item.

#### fun pauseOrResume()

Toggle state of ALC, either pause if it's playing or resume if it's paused.  
You can also start playback with this function (the same as calling ` start(null) `).

#### fun stop()

Function to stop ALC playback and destroy service.

####  fun next()

Skip to next song in order (dependent on ` Mode ` set).

#### fun previous()

Play previous song.

#### fun setSongProgress(progress : Int)

Set progress of currently played song.

#### MutableLiveData values

Values that can be observed to update app's UI
- currentFile - currently played file
- currentProgress - progress of currently played song (updated every 1000ms)

#### PendingIntentsForwarder

PendingIntentsForwarder is a BroadcastReceiver that handles PendingIntents from external components (notifications, widgets).  
Intent received should have an Extra value, with key equal to `Constants.intent_extra` provided by ALC, and value equal to one of the Message enum class values:
- NOTIFY_CHANGE - must be send when ALC is running and mode or playlist have changed
- START - start ALC. You can provide here also and extra with key `Constants.file` and value equal to the File you want to start music player with.
- STOP - stopping ALC
- PAUSE_OR_RESUME - pausing or resuming playback
- NEXT - skipping song
- PREVIOUS - previous song
- SEEK_TO - seeking to specific position in song. You must also provide here another extra with key `Constants.mediaplayer_position` and value of Integer.

## Application

ALC application represents an example usage of ALC library.

#### Controler

In the bottom of main view there is a controler, which initializes ALC. Controller displays five buttons (from the left):  
- button for setting playlist - navigate the main fragment to [FileBrowser](https://github.com/Lejdi/FileBrowser) and send the saved songs to ALC.  
- playback control buttons - previous song, pause or resume, next song  
- button for changing mode - allow to toggle playback mode of ALC  
Controler displays also name of currently played file and a seekbar which allows user to play song from any second.

#### Main Fragment

Displays list of saved files, providing their name, duration of the song and metadata author and title. Functionalities:  
- clicking on an item starts playback from specific file  
- long click on an item allow to delete file from the list  

#### Toolbar

Toolbar provides a menu with an option for clearing the entire list of saved files.

#### Widget

Widget allow to use ALC even when application is closed. However, it used the application's playlist, so it has to be set in advance.  
Widget looks the same as the default notification. That means: it displays currentyl played file name, song's title and author. Playback can be controlled with previous, pause or resume and next buttons.
