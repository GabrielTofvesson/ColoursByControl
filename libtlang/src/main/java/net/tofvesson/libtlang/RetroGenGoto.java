package net.tofvesson.libtlang;

import java.util.Stack;

public class RetroGenGoto implements Instruction {

    final Label link;

    public RetroGenGoto(){ this.link = new Label(""); }

    @Override
    public Object eval(Routine s, Object... params) {
        for(int i = 0 ; i<s.set.size(); ++i) if(s.set.get(i)==link){ s.stackPointer = i; break; }
        if(((Boolean) params[0])) s.operands.clear();
        return null;
    }

    @Override
    public Class<?>[] getParamTypes() {
        return new Class<?>[]{ Boolean.class };
    }

    @Override
    public Class getReturnType() {
        return Void.class;
    }

    public void retrogen(Routine<?> r){
        Stack<Character> s = new Stack<>();
        s.push((char)0);
        String genName;
        OUTER:
        do{
            StringBuilder sb = new StringBuilder();
            for(Character c : s) sb.append(c);
            genName = sb.toString();
            for(int i = 0; i < r.set.size(); ++i)
                if(r.set.get(i)==link) break;
                else if(r.set.get(i) instanceof Label && ((Label)r.set.get(i)).name.equals(genName)){
                    boolean next = true;
                    for(int j = 0; j < s.size(); ++j)
                        if(next){
                            char c = s.get(j);
                            if(++c != 0) next = false;
                            s.set(j, c);
                        }else break;
                    if(next) s.push((char)0);
                    continue OUTER;
                }
            link.name = genName;
            break;
        }while(true);
    }
}
