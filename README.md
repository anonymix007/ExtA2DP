## Extended A2DP
The [LSPosed](https://github.com/LSPosed/LSPosed) module everyone have been waiting for.
Device vendor decided to only include basic CLO/AOSP codecs? This module solves that problem once and for all.

## Installation
1. Install LSPosed Manager, Magisk companion module and ExtA2DP.
2. Reboot.
3. Enable ExtA2DP in LSPosed Manager and reboot once again.

### Current State
Works on Red Magic 8 Pro and on some other devices.

### Supported Versions
Android 13 (maybe 14?)

LSPosed v1.9.1 (6998)
**Debug version is REQUIRED for now**

### Hardware requirements
Relatively fresh Snapdragon SoC and CLO ROM.

As a rule of thumb: if aptX Adaptive is present, it'll probably work (though for the best experience 6/7/8 Gen 1 or newer are recommended)
If you're using older device, you'll most likely need a custom Magisk companion module. See examples [here](https://github.com/anonymix007/ExtA2DP/issues/4#issuecomment-1694643330) and [here](https://github.com/anonymix007/ExtA2DP/issues/2#issuecomment-1646835100) in addition to one provided on the release page.

### Logs
1. Disable Bluetooth
2. Force stop Bluetooth app in LSPosed
3. `adb logcat -c`
4. `adb logcat > logs.txt`
5. Enable Bluetooth and reproduce

#### TODO
- [x] UI in com.android.settings
- [x] Logic in com.android.bluetooth
- [x] Logic in native code
- [x] Fix codec quality selection
- [x] bt_adv_audio-related problems (done, but see next)
- [ ] Fix LE Audio (depends on bt_adv_audio, if someone knows where to get it, please contact me)
- [ ] More codecs? Opus, LC3plus HR, ...
- [ ] Add compilation of Android Bluetooth stack as a build step

### License
ExtA2DP is licensed under the terms of **GNU General Public License v3 (GPL-3)** (http://www.gnu.org/copyleft/gpl.html).
