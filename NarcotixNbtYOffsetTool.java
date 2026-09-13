import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public class NarcotixNbtYOffsetTool {
    static final int TAG_End = 0;
    static final int TAG_Byte = 1;
    static final int TAG_Short = 2;
    static final int TAG_Int = 3;
    static final int TAG_Long = 4;
    static final int TAG_Float = 5;
    static final int TAG_Double = 6;
    static final int TAG_Byte_Array = 7;
    static final int TAG_String = 8;
    static final int TAG_List = 9;
    static final int TAG_Compound = 10;
    static final int TAG_Int_Array = 11;
    static final int TAG_Long_Array = 12;

    static class Data {
        byte type;
        String name;
        Object value;
    }

    static class ListTag {
        byte elemType;
        ArrayList<Object> values = new ArrayList<>();
    }

    static class CompoundTag {
        ArrayList<Data> tags = new ArrayList<>();
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: java NarcotixNbtYOffsetTool <offsetY> <nbt files...>");
            System.exit(2);
        }
        int offsetY = Integer.parseInt(args[0]);
        for (int i = 1; i < args.length; i++) {
            Path path = Paths.get(args[i]);
            if (!Files.exists(path)) {
                System.out.println("Skipping missing file: " + path);
                continue;
            }
            byte[] raw = Files.readAllBytes(path);
            boolean gz = raw.length >= 2 && (raw[0] == (byte)0x1f) && (raw[1] == (byte)0x8b);
            byte[] nbt = gz ? gunzip(raw) : raw;
            Data root = readRoot(nbt);
            adjust(root, "", offsetY);
            byte[] out = writeRoot(root);
            Files.write(path, gz ? gzip(out) : out);
            System.out.println("Raised center station template by " + offsetY + " block(s): " + path);
        }
    }

    static byte[] gunzip(byte[] raw) throws IOException {
        ByteArrayInputStream bin = new ByteArrayInputStream(raw);
        GZIPInputStream gin = new GZIPInputStream(bin);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = gin.read(buf)) >= 0) bout.write(buf, 0, n);
        gin.close();
        return bout.toByteArray();
    }

    static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GZIPOutputStream gout = new GZIPOutputStream(bout);
        gout.write(raw);
        gout.close();
        return bout.toByteArray();
    }

    static Data readRoot(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        byte type = in.readByte();
        if (type == TAG_End) throw new IOException("Root cannot be TAG_End");
        String name = in.readUTF();
        Data d = new Data();
        d.type = type;
        d.name = name;
        d.value = readPayload(in, type);
        return d;
    }

    static Object readPayload(DataInputStream in, byte type) throws IOException {
        switch (type) {
            case TAG_Byte: return Byte.valueOf(in.readByte());
            case TAG_Short: return Short.valueOf(in.readShort());
            case TAG_Int: return Integer.valueOf(in.readInt());
            case TAG_Long: return Long.valueOf(in.readLong());
            case TAG_Float: return Float.valueOf(in.readFloat());
            case TAG_Double: return Double.valueOf(in.readDouble());
            case TAG_Byte_Array: {
                int len = in.readInt();
                byte[] arr = new byte[len];
                in.readFully(arr);
                return arr;
            }
            case TAG_String: return in.readUTF();
            case TAG_List: {
                ListTag list = new ListTag();
                list.elemType = in.readByte();
                int len = in.readInt();
                for (int i = 0; i < len; i++) list.values.add(readPayload(in, list.elemType));
                return list;
            }
            case TAG_Compound: {
                CompoundTag comp = new CompoundTag();
                while (true) {
                    byte t = in.readByte();
                    if (t == TAG_End) break;
                    Data child = new Data();
                    child.type = t;
                    child.name = in.readUTF();
                    child.value = readPayload(in, t);
                    comp.tags.add(child);
                }
                return comp;
            }
            case TAG_Int_Array: {
                int len = in.readInt();
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) arr[i] = in.readInt();
                return arr;
            }
            case TAG_Long_Array: {
                int len = in.readInt();
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) arr[i] = in.readLong();
                return arr;
            }
            default: throw new IOException("Unsupported tag type: " + type);
        }
    }

    static void adjust(Data d, String parent, int offsetY) {
        String n = d.name == null ? "" : d.name;
        if (d.type == TAG_List) {
            ListTag list = (ListTag)d.value;
            String lower = n.toLowerCase(Locale.ROOT);
            if (list.values.size() == 3 && list.elemType == TAG_Int && (lower.equals("pos") || lower.equals("size"))) {
                int y = ((Integer)list.values.get(1)).intValue();
                list.values.set(1, Integer.valueOf(y + offsetY));
                return;
            }
            if (list.values.size() == 3 && list.elemType == TAG_Double && lower.equals("pos")) {
                double y = ((Double)list.values.get(1)).doubleValue();
                list.values.set(1, Double.valueOf(y + offsetY));
                return;
            }
            for (Object v : list.values) {
                if (v instanceof CompoundTag) {
                    adjustCompound((CompoundTag)v, n, offsetY);
                } else if (v instanceof ListTag) {
                    Data child = new Data();
                    child.type = TAG_List;
                    child.name = n;
                    child.value = v;
                    adjust(child, n, offsetY);
                }
            }
        } else if (d.type == TAG_Compound) {
            adjustCompound((CompoundTag)d.value, n, offsetY);
        }
    }

    static void adjustCompound(CompoundTag comp, String parent, int offsetY) {
        for (Data child : comp.tags) adjust(child, parent, offsetY);
    }

    static byte[] writeRoot(Data root) throws IOException {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bout);
        out.writeByte(root.type);
        out.writeUTF(root.name == null ? "" : root.name);
        writePayload(out, root.type, root.value);
        out.flush();
        return bout.toByteArray();
    }

    static void writePayload(DataOutputStream out, byte type, Object value) throws IOException {
        switch (type) {
            case TAG_Byte: out.writeByte(((Byte)value).byteValue()); break;
            case TAG_Short: out.writeShort(((Short)value).shortValue()); break;
            case TAG_Int: out.writeInt(((Integer)value).intValue()); break;
            case TAG_Long: out.writeLong(((Long)value).longValue()); break;
            case TAG_Float: out.writeFloat(((Float)value).floatValue()); break;
            case TAG_Double: out.writeDouble(((Double)value).doubleValue()); break;
            case TAG_Byte_Array: {
                byte[] arr = (byte[])value;
                out.writeInt(arr.length);
                out.write(arr);
                break;
            }
            case TAG_String: out.writeUTF((String)value); break;
            case TAG_List: {
                ListTag list = (ListTag)value;
                out.writeByte(list.elemType);
                out.writeInt(list.values.size());
                for (Object v : list.values) writePayload(out, list.elemType, v);
                break;
            }
            case TAG_Compound: {
                CompoundTag comp = (CompoundTag)value;
                for (Data child : comp.tags) {
                    out.writeByte(child.type);
                    out.writeUTF(child.name == null ? "" : child.name);
                    writePayload(out, child.type, child.value);
                }
                out.writeByte(TAG_End);
                break;
            }
            case TAG_Int_Array: {
                int[] arr = (int[])value;
                out.writeInt(arr.length);
                for (int x : arr) out.writeInt(x);
                break;
            }
            case TAG_Long_Array: {
                long[] arr = (long[])value;
                out.writeInt(arr.length);
                for (long x : arr) out.writeLong(x);
                break;
            }
            default: throw new IOException("Unsupported tag type: " + type);
        }
    }
}