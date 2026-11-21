# Release builds

Release APKs are minified (R8) and signed with Kairo's own release key.

## The key (one time)

- Keystore: `~/.android/kairo-release.jks`, alias `kairo`. **Back it up** (with its password) somewhere safe:
  if it's lost, no future update can install over the app; users would have to uninstall first.
- Gradle reads it through `keystore.properties` in the `kairo/` folder (git-ignored):

```
storeFile=/home/<you>/.android/kairo-release.jks
storePassword=...
keyAlias=kairo
keyPassword=...
```

## Build

```
./gradlew :app:assembleRelease --max-workers=2
```

Output: `app/build/outputs/apk/release/app-release.apk`.
The release key differs from the debug key, so the first install over a debug build needs an uninstall (that deletes the app's data).
