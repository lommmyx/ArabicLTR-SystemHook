# Arabic LTR System Hook

Keep Android in Arabic and use a left-to-right layout in the apps you choose. Version 0.5.0 adds support for the standard and Drop-In editions of Shizuku+.

## Requirements

- A rooted Android phone supported by Vector, with Zygisk enabled.
- Vector v2.2 installed and active, with Vector Manager v2.2. [Official Vector setup](https://github.com/JingMatrix/Vector/tree/v2.2#readme)

## Install and use

1. Download and install the signed APK from [Releases](https://github.com/lommmyx/ArabicLTR-SystemHook/releases/latest).
2. Open **Vector Manager → Modules → Arabic LTR System Hook** and enable it.
3. In the module scope, select an app. For Shizuku+, select the edition installed on your phone.
4. Force-stop the selected app and open it again. Its layout should now run left to right while your language stays Arabic.

To undo the change, remove the app from the module scope and restart it. Reboot the phone when changing the scope for Android system components.
