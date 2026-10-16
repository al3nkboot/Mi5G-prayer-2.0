# Google Play — Data safety answers (Mi5G Prayer 1.0.0)

| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | N/A — no data collected/transmitted (no INTERNET permission) |
| Do you provide a way for users to request that their data is deleted? | N/A — nothing collected; uninstall/clear storage removes local settings |

Notes for reviewers:
- **Approximate location** (ACCESS_COARSE_LOCATION) is processed **on-device only** ("ephemeral/local processing" —
  Play's definition of *collection* is data transmitted off the device, which never happens). Not declared as collected.
- No SDKs that collect data (only AndroidX, Compose, Glance/WorkManager, adhan-java).
- Custom sound file: user-selected via Storage Access Framework, read locally, not transmitted.
