package com.labs.labrats;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal fragmented-MP4 (fMP4) muxer for live H.264 streaming.
 *
 * Produces exactly what a browser <video> + Media Source Extensions needs:
 * one init segment (ftyp+moov with avcC) followed by per-GOP media fragments
 * (moof+mdat). Dependency-free on purpose — no new Gradle artifacts, no
 * desugaring risk, full control over fragment cadence for low latency.
 *
 * Input samples carry length-prefixed (avcC-style) NAL units, 90 kHz clock.
 */
public class Fmp4Muxer {

    /** One encoded video frame: all its NAL units length-prefixed in one buffer. */
    public static final class Sample {
        public final byte[] data;
        public final long ptsUs;
        public final boolean key;

        public Sample(byte[] data, long ptsUs, boolean key) {
            this.data = data;
            this.ptsUs = ptsUs;
            this.key = key;
        }
    }

    private static final long TIMESCALE = 90000L;

    private final byte[] sps;
    private final byte[] pps;
    private final int width;
    private final int height;
    private int seqNum = 0;
    private long basePtsUs = -1;

    public Fmp4Muxer(byte[] sps, byte[] pps, int width, int height) {
        if (sps == null || sps.length < 4 || pps == null || pps.length < 1) {
            throw new IllegalArgumentException("bad csd");
        }
        this.sps = sps.clone();
        this.pps = pps.clone();
        this.width = width;
        this.height = height;
    }

    /** MSE codec string, e.g. avc1.64001F, derived from the SPS. */
    public String getCodecString() {
        return String.format(java.util.Locale.US, "avc1.%02X%02X%02X",
                sps[1] & 0xFF, sps[2] & 0xFF, sps[3] & 0xFF);
    }

    private long ts(long ptsUs) {
        if (basePtsUs < 0) basePtsUs = ptsUs;
        long d = ptsUs - basePtsUs;
        if (d < 0) d = 0;
        return d * TIMESCALE / 1000L;
    }

    // ---------------- init segment ----------------

    public byte[] buildInitSegment() throws Exception {
        byte[] avcC = buildAvcC();
        byte[] avc1 = box("avc1", visualEntry(), boxRaw(avcC, "avcC"));
        byte[] stsd = fullBox("stsd", 0, u32(1), avc1);
        byte[] stbl = box("stbl",
                stsd,
                fullBox("stts", 0, u32(0)),
                fullBox("stsc", 0, u32(0)),
                fullBox("stsz", 0, u32(0), u32(0)),
                fullBox("stco", 0, u32(0)));
        byte[] minf = box("minf",
                fullBox("vmhd", 1, u16(0), u16(0), u16(0), u16(0)),
                box("dinf", fullBox("dref", 0, u32(1), fullBox("url ", 1))),
                stbl);
        byte[] mdia = box("mdia",
                fullBox("mdhd", 0, u32(0), u32(0), u32(TIMESCALE), u32(0), u16(0x55C4), u16(0)),
                fullBox("hdlr", 0, u32(0), raw("vide"), u32(0), u32(0), u32(0), cstr("VideoHandler")),
                minf);
        byte[] trak = box("trak", tkhd(), mdia);
        byte[] mvex = box("mvex", fullBox("trex", 0, u32(1), u32(1), u32(0), u32(0), u32(0)));
        byte[] moov = box("moov", mvhd(), trak, mvex);
        byte[] ftyp = box("ftyp", raw("isom"), u32(0), raw("isom"), raw("iso6"), raw("avc1"), raw("mp41"));
        return concat(ftyp, moov);
    }

    private byte[] mvhd() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(0); // version + flags
        o.writeInt(0); o.writeInt(0); // ctime mtime
        o.writeInt(1000); o.writeInt(0); // timescale duration
        o.writeInt(0x00010000); o.writeShort(0x0100); o.writeShort(0); // rate volume
        o.writeLong(0); o.writeLong(0); // reserved
        // unity matrix
        o.writeInt(0x00010000); o.writeInt(0); o.writeInt(0);
        o.writeInt(0); o.writeInt(0x00010000); o.writeInt(0);
        o.writeInt(0); o.writeInt(0); o.writeInt(0x40000000);
        for (int i = 0; i < 6; i++) o.writeInt(0);
        o.writeInt(2); // next_track_ID
        o.flush();
        return fullBox("mvhd", 0, b.toByteArray());
    }

    private byte[] tkhd() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(0); o.writeInt(0); // ctime mtime
        o.writeInt(1); o.writeInt(0); o.writeInt(0); // track_ID reserved duration
        o.writeLong(0); // reserved
        o.writeShort(0); o.writeShort(0); o.writeShort(0); o.writeShort(0); // layer alt_group volume reserved
        o.writeInt(0x00010000); o.writeInt(0); o.writeInt(0);
        o.writeInt(0); o.writeInt(0x00010000); o.writeInt(0);
        o.writeInt(0); o.writeInt(0); o.writeInt(0x40000000);
        o.writeInt(width << 16); o.writeInt(height << 16);
        o.flush();
        return fullBox("tkhd", 7, b.toByteArray());
    }

    private byte[] visualEntry() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.write(new byte[6]); o.writeShort(1); // reserved + data_reference_index
        o.write(new byte[16]); // pre_defined + reserved
        o.writeShort(width); o.writeShort(height);
        o.writeInt(0x00480000); o.writeInt(0x00480000); // horiz/vert resolution 72dpi
        o.writeInt(0); o.writeShort(1); // reserved + frame_count
        byte[] comp = new byte[32]; o.write(comp); // compressorname
        o.writeShort(0x0018); o.writeShort(0xFFFF); // depth + pre_defined
        o.flush();
        return b.toByteArray();
    }

    private byte[] buildAvcC() throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeByte(1);
        o.writeByte(sps[1]); o.writeByte(sps[2]); o.writeByte(sps[3]);
        o.writeByte(0xFF); // lengthSizeMinusOne = 3 (4-byte NAL lengths)
        o.writeByte(0xE1); // numOfSPS
        o.writeShort(sps.length); o.write(sps);
        o.writeByte(1); // numOfPPS
        o.writeShort(pps.length); o.write(pps);
        o.flush();
        return b.toByteArray();
    }

    // ---------------- media fragment (one GOP) ----------------

    /**
     * @param gop             samples of one GOP, first sample must be a keyframe
     * @param nominalDur90k   fallback sample duration (90 kHz ticks) for the trailing sample
     */
    public byte[] buildFragment(List<Sample> gop, long nominalDur90k) throws Exception {
        int n = gop.size();
        long[] pts = new long[n];
        for (int i = 0; i < n; i++) pts[i] = ts(gop.get(i).ptsUs);

        // trun: header(8) + version/flags(4) + count(4) + data_offset(4) + 16*n
        int trunSize = 8 + 4 + 4 + 4 + 16 * n;
        int moofSize = 8 + 16 + 8 + 12 + 20 + trunSize; // moof hdr + mfhd + traf hdr + tfhd(12) + tfdt + trun
        int dataOffset = moofSize + 8; // + mdat header

        ByteArrayOutputStream tb = new ByteArrayOutputStream();
        DataOutputStream t = new DataOutputStream(tb);
        t.writeInt(n);
        t.writeInt(dataOffset);
        for (int i = 0; i < n; i++) {
            long dur = (i + 1 < n) ? Math.max(1, pts[i + 1] - pts[i]) : Math.max(1, nominalDur90k);
            t.writeInt((int) Math.min(dur, 0x7FFFFFFFL));
            t.writeInt(gop.get(i).data.length);
            t.writeInt(gop.get(i).key ? 0x02000000 : 0x01010000);
            t.writeInt(0); // composition offset (no B-frames on baseline surface input)
        }
        t.flush();
        byte[] trun = fullBoxRaw("trun", new byte[]{0, 0, 0x0F, 0x01}, tb.toByteArray());

        ByteArrayOutputStream fb = new ByteArrayOutputStream();
        DataOutputStream f = new DataOutputStream(fb);
        f.writeLong(pts[0]); // tfdt v1 baseMediaDecodeTime
        f.flush();
        byte[] tfdt = fullBoxRaw("tfdt", new byte[]{1, 0, 0, 0}, fb.toByteArray());

        seqNum++;
        byte[] traf = box("traf",
                fullBox("tfhd", 0x020000), // default-base-is-moof
                tfdt,
                trun);
        byte[] moof = box("moof", fullBox("mfhd", 0, u32(seqNum)), traf);

        int total = 0;
        for (Sample s : gop) total += s.data.length;
        ByteArrayOutputStream mb = new ByteArrayOutputStream(total + 8);
        DataOutputStream m = new DataOutputStream(mb);
        m.writeInt(total + 8); m.writeBytes("mdat");
        for (Sample s : gop) m.write(s.data);
        m.flush();
        return concat(moof, mb.toByteArray());
    }

    // ---------------- NAL helpers ----------------

    /** Split Annex-B buffer into raw NAL units (no start codes). Drops empties. */
    public static List<byte[]> splitAnnexB(byte[] buf, int off, int len) {
        List<byte[]> out = new ArrayList<>();
        int end = off + len;
        int start = -1;
        int i = off;
        while (i < end - 3) {
            int sc = 0;
            if (buf[i] == 0 && buf[i + 1] == 0) {
                if (buf[i + 2] == 1) sc = 3;
                else if (i + 3 < end && buf[i + 2] == 0 && buf[i + 3] == 1) sc = 4;
            }
            if (sc > 0) {
                if (start >= 0 && i > start) out.add(slice(buf, start, i));
                start = i + sc;
                i = start;
            } else {
                i++;
            }
        }
        if (start >= 0 && end > start) out.add(slice(buf, start, end));
        else if (start < 0 && len > 0) out.add(slice(buf, off, end));
        return out;
    }

    private static byte[] slice(byte[] b, int s, int e) {
        byte[] r = new byte[e - s];
        System.arraycopy(b, s, r, 0, r.length);
        return r;
    }

    /** Join NAL units with 4-byte big-endian lengths (avcC sample format). */
    public static byte[] toLengthPrefixed(List<byte[]> nalus) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        for (byte[] nalu : nalus) {
            o.writeInt(nalu.length);
            o.write(nalu);
        }
        o.flush();
        return b.toByteArray();
    }

    // ---------------- box plumbing ----------------

    private static byte[] box(String type, byte[]... payloads) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeInt(0); // patched below
        o.writeBytes(type);
        for (byte[] p : payloads) o.write(p);
        o.flush();
        byte[] r = b.toByteArray();
        int size = r.length;
        r[0] = (byte) (size >>> 24); r[1] = (byte) (size >>> 16);
        r[2] = (byte) (size >>> 8); r[3] = (byte) size;
        return r;
    }

    private static byte[] boxRaw(byte[] payload, String type) throws Exception {
        return box(type, payload);
    }

    private static byte[] fullBox(String type, int flags, byte[]... payloads) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        DataOutputStream o = new DataOutputStream(b);
        o.writeByte(0); o.writeByte((flags >>> 16) & 0xFF);
        o.writeByte((flags >>> 8) & 0xFF); o.writeByte(flags & 0xFF);
        for (byte[] p : payloads) o.write(p);
        o.flush();
        return box(type, b.toByteArray());
    }

    private static byte[] fullBoxRaw(String type, byte[] verFlags, byte[] body) throws Exception {
        return box(type, concat(verFlags, body));
    }

    private static byte[] u32(long v) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream(4);
        new DataOutputStream(b).writeInt((int) v);
        return b.toByteArray();
    }

    private static byte[] u16(int v) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream(2);
        new DataOutputStream(b).writeShort(v);
        return b.toByteArray();
    }

    private static byte[] raw(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private static byte[] cstr(String s) {
        byte[] r = raw(s);
        byte[] out = new byte[r.length + 1];
        System.arraycopy(r, 0, out, 0, r.length);
        return out;
    }

    private static byte[] concat(byte[]... parts) throws Exception {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        for (byte[] p : parts) b.write(p);
        return b.toByteArray();
    }
}
