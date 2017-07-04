package net.tofvesson.libtlang;

import java.util.ArrayList;
import java.util.List;

public interface Compiler {
    byte[] compile(Routine<?> r);
    Instruction<?> load(byte[] b);

    class DefaultCompiler implements Compiler{

        protected static final byte[] LID = "tfvsnlng".getBytes();
        protected long CID = 0b00100100;

        @Override
        public byte[] compile(Routine<?> r) {
            List<Byte> build = new ArrayList<>();
            for(byte b : LID) build.add(b);
            for(byte b : split(CID)) build.add(b);



            byte[] b = new byte[build.size()];
            for(int i = 0; i<b.length; ++i) b[i] = build.get(i);
            return b;
        }

        @Override
        public Instruction<?> load(byte[] b) {
            return null;
        }

        protected byte[] split(long l){
            return new byte[]{
                    (byte)(l&0xFF),
                    (byte)((l>>8)&0xFF),
                    (byte)((l>>16)&0xFF),
                    (byte)((l>>24)&0xFF),
                    (byte)((l>>32)&0xFF),
                    (byte)((l>>40)&0xFF),
                    (byte)((l>>48)&0xFF),
                    (byte)(l>>56)
            };
        }
        protected long combine(byte[] b){
            int size = Math.min(b.length, 8);
            long out = 0;
            for(int i = 0; i<size; ++i) out += b[i] << i*8;
            return out;
        }
    }
}
