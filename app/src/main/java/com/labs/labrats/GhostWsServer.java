package com.labs.labrats;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal RFC6455 WebSocket server (device -> browser media path).
 *
 * Send-only by design: broadcasts binary fMP4 fragments to every connected
 * viewer, answers ping, honors close. No third-party dependency on purpose —
 * ~150 lines, full control, zero Gradle risk.
 */
public class GhostWsServer {

    private static final String TAG = "GhostWsServer";
    private static final String WS_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    /** Supplies the fMP4 init segment each new viewer must receive first. */
    public interface InitProvider {
        byte[] getInit();
    }

    private final Set<Client> clients = Collections.synchronizedSet(new HashSet<Client>());
    private volatile InitProvider initProvider;
    private volatile ServerSocket serverSocket;
    private volatile Thread acceptThread;
    private volatile boolean running = false;

    public void setInitProvider(InitProvider p) {
        initProvider = p;
    }

    public synchronized void start(int port) throws Exception {
        if (running) return;
        serverSocket = new ServerSocket(port);
        running = true;
        acceptThread = new Thread(this::acceptLoop, "GhostWsAccept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        Log.d(TAG, "listening :" + port);
    }

    public synchronized void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (Exception ignored) {}
        synchronized (clients) {
            for (Client c : clients) c.shutdown();
            clients.clear();
        }
    }

    public int viewerCount() {
        return clients.size();
    }

    /** Broadcast one binary message (an fMP4 init or media fragment). Slow/dead clients are dropped. */
    public void broadcast(byte[] data, int off, int len) {
        if (data == null || len <= 0 || clients.isEmpty()) return;
        Client[] snapshot;
        synchronized (clients) {
            snapshot = clients.toArray(new Client[0]);
        }
        for (Client c : snapshot) {
            try {
                c.sendBinary(data, off, len);
            } catch (Exception e) {
                remove(c);
            }
        }
    }

    public void broadcast(byte[] data) {
        if (data != null) broadcast(data, 0, data.length);
    }

    private void remove(Client c) {
        clients.remove(c);
        c.shutdown();
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket s = serverSocket.accept();
                s.setTcpNoDelay(true);
                Thread t = new Thread(new Handshake(s), "GhostWsHs");
                t.setDaemon(true);
                t.start();
            } catch (Exception e) {
                if (running) Log.w(TAG, "accept: " + e.getMessage());
            }
        }
    }

    private class Handshake implements Runnable {
        private final Socket socket;

        Handshake(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                InputStream in = socket.getInputStream();
                OutputStream out = socket.getOutputStream();
                String req = readHttpHeader(in);
                if (req == null || !req.toLowerCase().contains("upgrade: websocket")) {
                    socket.close();
                    return;
                }
                String key = headerValue(req, "sec-websocket-key");
                if (key == null || key.isEmpty()) {
                    socket.close();
                    return;
                }
                String accept = sha1Base64(key.trim() + WS_GUID);
                String resp = "HTTP/1.1 101 Switching Protocols\r\n"
                        + "Upgrade: websocket\r\n"
                        + "Connection: Upgrade\r\n"
                        + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
                out.write(resp.getBytes("UTF-8"));
                out.flush();
                Client c = new Client(socket, in, out);
                clients.add(c);
                // New viewers must start from the init segment or MSE cannot decode.
                InitProvider p = initProvider;
                if (p != null) {
                    try {
                        byte[] init = p.getInit();
                        if (init != null) c.sendBinary(init, 0, init.length);
                    } catch (Exception e) {
                        remove(c);
                        return;
                    }
                }
                c.readLoop();
            } catch (Exception ignored) {
                try {
                    socket.close();
                } catch (Exception ignored2) {}
            }
        }
    }

    private class Client {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private volatile boolean open = true;

        Client(Socket socket, InputStream in, OutputStream out) {
            this.socket = socket;
            this.in = in;
            this.out = out;
        }

        synchronized void sendBinary(byte[] data, int off, int len) throws Exception {
            if (!open) throw new EOFException("closed");
            if (len < 126) {
                out.write(0x82);
                out.write(len);
            } else if (len <= 0xFFFF) {
                out.write(0x82);
                out.write(126);
                out.write((len >>> 8) & 0xFF);
                out.write(len & 0xFF);
            } else {
                out.write(0x82);
                out.write(127);
                for (int i = 7; i >= 0; i--) out.write((int) ((len >>> (8L * i)) & 0xFF));
            }
            out.write(data, off, len);
            out.flush();
        }

        synchronized void sendPong(byte[] payload) throws Exception {
            out.write(0x8A);
            out.write(payload.length);
            out.write(payload);
            out.flush();
        }

        void readLoop() {
            try {
                while (open) {
                    int b0 = in.read();
                    int b1 = in.read();
                    if (b0 < 0 || b1 < 0) break;
                    int opcode = b0 & 0x0F;
                    long len = b1 & 0x7F;
                    if (len == 126) {
                        len = (readByte() << 8) | readByte();
                    } else if (len == 127) {
                        len = 0;
                        for (int i = 0; i < 8; i++) len = (len << 8) | readByte();
                    }
                    boolean masked = (b1 & 0x80) != 0;
                    byte[] mask = new byte[4];
                    if (masked) readFull(mask, 0, 4);
                    if (opcode == 0x8) break; // close
                    if (opcode == 0x9) { // ping -> pong
                        byte[] pl = new byte[(int) Math.min(len, 125)];
                        readFullMasked(pl, 0, pl.length, mask);
                        skipMasked(len - pl.length, mask);
                        try {
                            sendPong(pl);
                        } catch (Exception ignored) {}
                        continue;
                    }
                    skipMasked(len, mask); // text/binary from viewer: not a media path, ignore
                }
            } catch (Exception ignored) {
            } finally {
                remove(this);
            }
        }

        private int readByte() throws Exception {
            int v = in.read();
            if (v < 0) throw new EOFException("closed");
            return v;
        }

        private void readFull(byte[] b, int o, int n) throws Exception {
            while (n > 0) {
                int r = in.read(b, o, n);
                if (r < 0) throw new EOFException("closed");
                o += r;
                n -= r;
            }
        }

        private void readFullMasked(byte[] b, int o, int n, byte[] mask) throws Exception {
            readFull(b, o, n);
            for (int i = 0; i < n; i++) b[o + i] ^= mask[i % 4];
        }

        private void skipMasked(long n, byte[] mask) throws Exception {
            byte[] tmp = new byte[4096];
            long done = 0;
            while (done < n) {
                int r = in.read(tmp, 0, (int) Math.min(tmp.length, n - done));
                if (r < 0) throw new EOFException("closed");
                done += r;
            }
        }

        void shutdown() {
            open = false;
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
    }

    private static String readHttpHeader(InputStream in) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        int[] tail = new int[]{-1, -1, -1, -1};
        int total = 0;
        while (total < 8192) {
            int v = in.read();
            if (v < 0) return null;
            b.write(v);
            tail[0] = tail[1]; tail[1] = tail[2]; tail[2] = tail[3]; tail[3] = v;
            total++;
            if (tail[0] == '\r' && tail[1] == '\n' && tail[2] == '\r' && tail[3] == '\n') break;
        }
        return b.toString("UTF-8");
    }

    private static String headerValue(String req, String name) {
        String[] lines = req.split("\r\n");
        for (String l : lines) {
            int c = l.indexOf(':');
            if (c > 0 && l.substring(0, c).trim().equalsIgnoreCase(name)) {
                return l.substring(c + 1).trim();
            }
        }
        return null;
    }

    private static String sha1Base64(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] d = md.digest(s.getBytes("UTF-8"));
        return android.util.Base64.encodeToString(d, android.util.Base64.NO_WRAP);
    }
}
