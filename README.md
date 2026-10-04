# Alarm

A Kotlin Android alarm app with a plain interface. Add, edit, enable or delete alarms; choose a one-time alarm or a daily repeat. Stop a ringing alarm by solving a small multiplication question or completing 30–100 shakes. Existing alarms with a lower shake target now use 30.

## Sounds

- Phone default alarm sound or any sound offered by the phone's alarm ringtone picker.
- Your own audio selected through Android's file picker, with persistent read access.
- Up to ten bundled app ringtones. Add audio files under `app/src/main/res/raw` with names beginning with `alarm_`, for example `alarm_soft_bells.mp3`. Rebuild the app and choose **App ringtones**. The picker uses the first ten names alphabetically. No sample sounds are bundled yet.

## Running

Open in Android Studio, sync Gradle and run on Android 7.0 or newer. On first use, allow notifications and Alarms & reminders. On Android 14+, the app offers the full-screen alarm permission setting when needed. Without full-screen access, open the ringing notification to complete the challenge.

The alarm uses the phone's alarm volume, with optional vibration. Alarms are restored after reboot, app updates and time changes. Deleted or inaccessible custom audio falls back to the phone's default sound. Devices without an accelerometer use the math challenge instead.

Shake detection counts changes of direction during gentle back-and-forth motion. It uses Android's linear acceleration sensor when available, otherwise filters gravity out of raw accelerometer readings. A 120 ms minimum interval rejects repeated samples; movements more than one second apart start a new sequence. Stationary noise and sustained motion in one direction do not increment progress. Progress and math questions survive screen rotation and reopening while the service is running. Editor drafts survive activity recreation. Leaving the challenge does not stop the ringing service. Simultaneous alarms wait in a queue; editing or deleting a saved alarm does not change its already-ringing challenge.

Force-stopping the app prevents alarms until it is opened again. Manufacturer battery restrictions, muted alarm volume and system service controls can affect ringing. Test locked-screen delivery, reboot restoration, custom audio access and shake sensitivity on a physical device before relying on it.

Build: `gradlew.bat assembleDebug testDebugUnitTest lintDebug` (JDK from Android Studio).
