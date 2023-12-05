package edu.illinois.starts.smethods;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConstantPoolParser {
    public static final int HEAD=0xcafebabe;
    // Constant pool types
    public static final byte CONSTANT_Utf8               = 1;
    public static final byte CONSTANT_Integer            = 3;
    public static final byte CONSTANT_Float              = 4;
    public static final byte CONSTANT_Long               = 5;
    public static final byte CONSTANT_Double             = 6;
    public static final byte CONSTANT_Class              = 7;
    public static final byte CONSTANT_String             = 8;
    public static final byte CONSTANT_FieldRef           = 9;
    public static final byte CONSTANT_MethodRef          =10;
    public static final byte CONSTANT_InterfaceMethodRef =11;
    public static final byte CONSTANT_NameAndType        =12;
    public static final byte CONSTANT_MethodHandle       =15;
    public static final byte CONSTANT_MethodType         =16;
    public static final byte CONSTANT_InvokeDynamic      =18;
    public static final byte CONSTANT_Module             =19;
    public static final byte CONSTANT_Package            =20;

    public static Set<String> getClassNames(ByteBuffer buf) {
        Set<Integer> classIndexes = new HashSet<>();
        Map<Integer, String> index2String = new HashMap();

        Set<String> classNames = new HashSet<>();

        if(buf.order(ByteOrder.BIG_ENDIAN).getInt()!=HEAD) {
            System.out.println("not a valid class file");
            return classNames;
        }
        int minor=buf.getChar(), ver=buf.getChar();
        for(int ix=1, num=buf.getChar(); ix<num; ix++) {
            String s; int index1=-1, index2=-1;
            byte tag = buf.get();
            switch(tag) {
                default:
                    System.out.println("unknown pool item type "+buf.get(buf.position()-1));
                    return classNames;
                case CONSTANT_Utf8:
                    String str = decodeString(ix, buf);
                    index2String.put(ix, str);
                    continue;
                case CONSTANT_Class: case CONSTANT_String: case CONSTANT_MethodType:
                case CONSTANT_Module: case CONSTANT_Package:
                    s="%d:\t%s ref=%d%n"; index1=buf.getChar();
                    if (tag == CONSTANT_Class){
                        classIndexes.add(index1);
                    }
                    break;
                case CONSTANT_FieldRef: case CONSTANT_MethodRef:
                case CONSTANT_InterfaceMethodRef: case CONSTANT_NameAndType:
                    s="%d:\t%s ref1=%d, ref2=%d%n";
                    index1=buf.getChar(); index2=buf.getChar();
                    break;
                case CONSTANT_Integer: s="%d:\t%s value="+buf.getInt()+"%n"; break;
                case CONSTANT_Float: s="%d:\t%s value="+buf.getFloat()+"%n"; break;
                case CONSTANT_Double: s="%d:\t%s value="+buf.getDouble()+"%n"; ix++; break;
                case CONSTANT_Long: s="%d:\t%s value="+buf.getLong()+"%n"; ix++; break;
                case CONSTANT_MethodHandle:
                    s="%d:\t%s kind=%d, ref=%d%n"; index1=buf.get(); index2=buf.getChar();
                    break;
                case CONSTANT_InvokeDynamic:
                    s="%d:\t%s bootstrap_method_attr_index=%d, ref=%d%n";
                    index1=buf.getChar(); index2=buf.getChar();
                    break;
            }
//            System.out.printf(s, ix, FMT[tag], index1, index2);
        }

        for (Integer index : classIndexes){
            classNames.add(index2String.getOrDefault(index, ""));
        }
        return classNames;
    }
    private static String[] FMT= {
            null, "Utf8", null, "Integer", "Float", "Long", "Double", "Class",
            "String", "Field", "Method", "Interface Method", "Name and Type",
            null, null, "MethodHandle", "MethodType", null, "InvokeDynamic",
            "Module", "Package"
    };

    private static String decodeString(int poolIndex, ByteBuffer buf) {
        int size=buf.getChar(), oldLimit=buf.limit();
        buf.limit(buf.position()+size);
        StringBuilder sb=new StringBuilder(size+(size>>1)+16);
        while(buf.hasRemaining()) {
            byte b=buf.get();
            if(b>0) sb.append((char)b);
            else
            {
                int b2 = buf.get();
                if((b&0xf0)!=0xe0)
                    sb.append((char)((b&0x1F)<<6 | b2&0x3F));
                else
                {
                    int b3 = buf.get();
                    sb.append((char)((b&0x0F)<<12 | (b2&0x3F)<<6 | b3&0x3F));
                }
            }
        }
        buf.limit(oldLimit);
//        System.out.println(sb);
        return sb.toString();
    }
}
