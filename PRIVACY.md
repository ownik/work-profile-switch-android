# Privacy Policy

**Work Profile Switch** does not collect, store, or transmit any personal
data.

The app runs entirely on-device. It only calls Android's `UserManager` API
to toggle the work profile's quiet mode (enable/disable the work profile).
It does not:

- Collect or transmit any personal or usage data
- Use analytics or crash-reporting SDKs
- Show ads
- Make any network requests
- Include any third-party SDKs

## Permissions

- `android.permission.MODIFY_QUIET_MODE` — used solely to enable/disable the
  work profile via `UserManager.requestQuietModeEnabled()`. This is a
  system-level permission that must be granted manually (via ADB or
  Shizuku) — see the [README](README.md) for setup instructions. It is
  never used for any purpose other than the toggle itself.

## Contact

Questions about this policy or the app can be raised via the GitHub
repository: https://github.com/ownik/work-profile-switch-android/issues