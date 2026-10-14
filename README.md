# Bidder-Lin

A desktop automation tool written in **Kotlin** (JVM) with a **JavaFX** UI. It drives multiple Chrome
profiles to scan LinkedIn feeds and jobs, filter posts and profiles, send connection requests, reply
to messages and publish content, while keeping per-profile limits and reporting to a Telegram bot.

## Tech stack

| Layer | Technology |
|---|---|
| Language | Kotlin 1.6 (JVM), plus a few Java utility classes |
| UI | JavaFX 17 (FXML) |
| Browser automation | Selenium / ChromeDriver, AppleScript (`osascript`), `cliclick` |
| Computer vision / OCR | OpenCV 4.11 (template matching), Tesseract |
| Storage | Local XML (Simple XML), Firebase Firestore |
| Integrations | Telegram Bot API (alerts), Google Analytics (Measurement Protocol), OpenAI |
| Build | Maven (`mvnw`), IntelliJ IDEA JAR artifact |

## Project structure

```
src/main/kotlin/com/linked/
├── bidder/        JavaFX application entry point, menu controller, UI cells
├── chromedriver/  Chrome lifecycle and per-profile run loop
├── scanner/       Feed / jobs scanners and the connection bidder
│   ├── action/    Individual actions: auto-accept, auto-connect, auto-post, messages, cleanup
│   └── logic/     Pure decision logic: filters, dynamic limits, decision makers, cover letters
├── model/         Data models (profiles, posts, jobs, app config) and UI commands
├── api/           External APIs: Telegram bot, analytics, OpenAI, remote content DBs
├── db/            Local XML DB and Firebase helpers
└── utils/         Shell command runner, clipboard, misc helpers
```

## Kotlin Multiplatform outlook

The project targets the JVM only today, but its layering maps cleanly onto a KMP structure:

- **`commonMain` candidates:** `scanner/logic` (filters, `DynamicLimits`, `DecisionMakers`, `CoverLetter`) and
  most of `model` are plain Kotlin business logic with no UI dependencies.
- **Platform-specific (`jvmMain`):** Selenium/ChromeDriver, OpenCV, JavaFX, AWT/`osascript` automation and the
  Firebase Admin SDK.
- **Migration path:** replace Simple XML with `kotlinx.serialization`, OkHttp with Ktor Client, and `java.time`
  with `kotlinx-datetime`, then expose the shared core to other targets (e.g. a Compose Multiplatform dashboard).

## Running

```
./run_dev.sh          # dev config (flutterConfigDev.xml)
./run_mac_lin1.sh     # production config for machine #1
./run_mac_lin2.sh     # production config for machine #2
```

The application takes the path to an XML `AppConfig` as its first argument.

---

# Setup notes

[Config]

### FX-zulu

https://stackoverflow.com/questions/76989593/how-and-where-can-i-download-javafx-on-m1-mac

1 install {sdk} command on https://sdkman.io/

2 sdk install java 17.0.8.fx-zulu

3 select ZuluFX on project settings

### take_screenshot

1) cd export ; mkdir screen_logs ; cd ..

2) chmod +x take_screenshot.sh

3) run and accept permission

### For mouse movements

brew install coreutils

### Display

Mac->Settings->Battery

1 Turn display off after: Never

2 Enable - Prevent computer from sleeping automatically when the display is off

### Disable update Chrome notification
Disable notification "to get future google chrome updates you'll need macos .. "
defaults write com.google.Chrome SuppressUnsupportedOSWarning -bool true

### Install shell apps
> brew install cliclick

> brew install ffmpeg

run start_screen_recording and stop_screen_recording to grant permissions
OR
download the binary and copy it to a system folder
https://ffmpeg.org/download.html -> https://evermeet.cx/ffmpeg/
sudo cp ff* /usr/local/bin (details -> https://www.toolify.ai/hardware/complete-guide-install-ffmpeg-on-macos-big-sur-intel-3064484)

Note:
1) System Preferences → Security & Privacy → Accessibility
2) add Terminal app


### Lang
add ABC keyboard to system input methods

[Info]

### Create a Launcher class:

public class Launcher {
 public static void main(String[] args) {
  MainGUI.main(args);
 }
}

### On to building the Jar

1 You probably still have a META-INF folder from the previous build so delete it.

2 Build the project as a JAR:
File->Project Structure -> Artifacts -> "+" -> JAR-> from modules with dependancies..

3 Choose the Launcher class for you main and check "copy to the output directory and link via Manifest" and Press Ok

4 Press Apply then OK.

5 go to Build -> Build artifacts-> Rebuild

### Show timestamp
open dev tools (F12)
click the three-dot menu in the top right
click settings
select Preferences in the left menu
check show timestamps in the Console section of the settings screen

### keyboardSwitcher
https://github.com/Lutzifer/keyboardSwitcher
copy after install
>brew install lutzifer/homebrew-tap/keyboardSwitcher
>which keyboardSwitcher

### OCR
>brew install tesseract

On Big Sur there was an issue with setuptools: for the locally installed python@3.14, setuptools had to be installed and verified
(use exactly the Python that the installer pulled in, and install into it)

/usr/local/opt/python@3.14/bin/pip3.14 install --upgrade pip --break-system-packages
/usr/local/opt/python@3.14/bin/pip3.14 install setuptools wheel --break-system-packages

verify the installation

/usr/local/opt/python@3.14/libexec/bin/python3 -c "import setuptools, sys; print(sys.executable, setuptools.__version__)"

#### Chrome profiles config

0) in DevTools click the gear -> Settings > Preferences > Appearance > check_box_outline_blank Show what's new after each update checkbox.

1) import contacts into the Gmail account so LinkedIn picks them up and treats the account as established

#extensions# (leave a couple out) AdBlock / LastPass / 1Password / Grammarly / Google Translate / Nimbus Screenshot

#sites# (leave a couple out)  Facebook / Instagram / Twitter / Google / Bing / BBC / CNN / NY Times / Amazon / eBay / AliExpress / YouTube / Netflix /
Trello / Slack / GitHub / Coursera / Khan Academy / Reddit / Stack Overflow /

3) chrome://settings/languages -> unique language

4) unique zoom % - 75% - 100%
   chrome://settings/appearance

6) a different number of extensions, and none of: VPN, LinkedIn; vary dev ones: PDF / JSON / React

7) dark/light theme
   chrome://settings/appearance

8) default font size and default font
   chrome://settings/appearance

9) use different bookmarks in Chrome

10) check the fingerprint hash via
    https://fingerprintjs.github.io/fingerprintjs/

11) in the JS console set logs to !!! Hide All !!!, uncheck everything in the {Default level} dropdown

### Linked account search config
1) set ENG lang
https://www.linkedin.com/mypreferences/d/settings/language
2) set profile cover
3) if Jobs search history is used, it must contain the required entry
(run it beforehand and make sure it isn't overwritten by new searches)

### Disable Video Preview
https://www.linkedin.com/mypreferences/d/settings/autoplay-videos

### JS Console
in DevTools click the gear -> Settings > Preferences > Appearance > check_box_outline_blank Show what's new after each update checkbox.

allow pasting from the clipboard -> allow pasting

### Additional
Name the Chrome profile using the common format -> S#2a L+ U+

### Bugfix
java.lang.module.FindException: Two versions of module annotations found in ./out/artifacts/bidder_up_jar (annotations-13.0.jar and annotations-4.1.1.4.jar)
how resolve: remove  annotations-4.1.1.4.jar

### Mac mini config
1) enable auto login -> Settings/Users & Groups/Login Options/Automatic login/[Select user]
2) disable BLE dialog -> Settings/BLE/Advanced/[disable for mouse and keyboard]
3) disable all updates -> Software Updates/[disable auto updates]/[disable all in Advanced]
4) install HDMI emulator
5) brew install displayplacer
6) disable lock screen
   -> Settings/Security/Request Password/Off
   -> Settings/Energy Save/Turn Display After/Never
7) start after power failure -> Settings/Energy Save/automatically after a power failure
8) disable screen server -> Settings/Screen server

### Display config
displayplacer list - show the list and how to set the current mode
displayplacer "res:1920x1080 enabled:true scaling:on"

### Permissions
Automation -> Terminal -> Google Chrome / System Events
Accessibility -> Terminal
Developers Tools -> Terminal

### Remote reboot
System Preferences → Sharing
"Remote Login" to enable SSH access

brew install ngrok/ngrok/ngrok
MacLin1 -> ngrok config add-authtoken <NGROK_AUTHTOKEN>
ngrok tcp 22

### Gmail config
set google script to track new messages
add the SenderId to the function (from the account that published the script)
https://script.google.com/home/projects/<SCRIPT_ID>
set up run triggers (every 5 min)

### MacLin1
tcp://<NGROK_HOST>:<PORT> -> localhost:22
!!! run without sshpass first !!!
sshpass -p <PASSWORD> ssh mac@<NGROK_HOST> -p <PORT>

ssh mac@<NGROK_HOST> -p <PORT> 'cat git/bidder-lin/export/linkedDB1.xml' > linkedDB1.xml ; open -a Atom linkedDB1.xml
ssh mac@<NGROK_HOST> -p <PORT> 'cat git/bidder-lin/export/application1.log' > lin-application1.log ; open -a Atom lin-application1.log

sudo reboot
networksetup -getairportnetwork en1
[OR]
pwd = <PASSWORD>
sudo pkill -f TeamViewer
osascript -e 'tell application "TeamViewer" to activate'
//
killall TeamViewer
ps aux | grep -i teamviewer
kill -9 <PID>

### Permissions /Chrome/Events/
osascript -e 'tell application "Google Chrome" to activate' -e 'tell application "System Events" to keystroke "j" using {command down, option down}'
osascript -e 'tell application "Google Chrome" to get count of windows'

### AutoJsoin
HAP_S1-HAP_SX - disable
keep only Platforma

### Banners
https://www.figma.com/design/7rHoe1lF9ckvhLZoDVUm2e/LinkedIn-banner?node-id=2001-135&t=IuctIVBZgNsaBe0r-0

### Fast Test Config

    <startUrl><![CDATA[
https://www.linkedin.com/search/results/content/?keywords=hiring%20flutter%20remote&origin=FACETED_SEARCH&sid=MbA&sortBy=%22relevance%22
]]></startUrl>

    <urlStartPathActions>

    </urlStartPathActions>

    <urlFeedActions>

    </urlFeedActions>

    <urlEndPathActions>
    
    </urlEndPathActions>

### Dev Backup

    <startUrl><![CDATA[
https://www.linkedin.com|https://www.linkedin.com/feed/|https://www.linkedin.com/in/|https://www.linkedin.com/notifications/
]]></startUrl>

    <urlStartPathActions>
        <item>click|navbar|Home|2500|1</item>
        <item>click|navbar|My Network|2500|1</item>
        <item>click|navbar|Messaging|2500|0</item>
        <item>click|navbar|Notifications|2500|0</item>
        <item>scroll|bottom|1000|1</item>
        <item>scroll|top|1000|1</item>
        <item>click|navbar|Jobs|8246|0</item>
        <item>click|navbar|Home|9636|1</item> <!-- must be 1, for position on correct input filed -->
    </urlStartPathActions>

    <urlFeedActions>
        <item>input|navbarFocus|"hiring"/"hiring"|4401|1</item> <!-- must be 1, for skip iFrame and init random-->
        <item>input|navbarText|"hiring" "flutter"/"hiring" "mobile"|2500|1</item>
        <item>input|navbarSubmit|"hiring flutter remote"/"hiring mobile remote"|2500|1</item>
        <item>click|searchBar|Posts|2500|1</item>
        <item>click|searchBar|Sort by|2500|1</item>
        <item>click|searchBarLabel|Latest|2500|1</item>
        <item>click|searchBar|Show results|2500|1</item>
        <item>click|searchBar|Date posted|2500|1</item>
        <item>click|searchBarLabel|Past 24 hours|2500|1</item>
        <item>click|searchBar|Show results/1|2500|1</item>
    </urlFeedActions>

    <urlEndPathActions>
        <!-- "My Network" is not used here to avoid exceeding the per-run processing limits -->
        <item>click|navbar|Home|3000|1</item>
    </urlEndPathActions>

### Old config
        <item>input|navbarFocus|"hiring"/"hiring"/"hiring"/"hiring"/"hiring"|4401|1</item> <!-- must be 1, for skip iFrame and init random-->
        <item>input|navbarText|"flutter hiring"/"kotlin hiring"/"swift hiring"/"react hiring"/"mobile hiring"|4401|0.7</item>
        <item>input|navbarSubmit|flutter hiring/kotlin hiring/swift hiring/react hiring/mobile hiring|9120|1</item>

### Autostart
1) duplicate the default Terminal profile, name it bidder, set the startup command
   > cd /Users/mac/git/bidder; sleep 60; git pull; ./run_mac_design.sh
2) duplicate the default Terminal profile, name it ngrok, set the startup command
   > ngrok tcp 22
3) duplicate the default Terminal profile, name it caffeinate, set the startup command
   > caffeinate
4) create a window group named bidder and enable reopening windows after restart 