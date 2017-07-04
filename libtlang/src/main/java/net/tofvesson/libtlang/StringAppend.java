package net.tofvesson.libtlang;

public class StringAppend implements Instruction<String> {

    protected final boolean paramOrder;

    public StringAppend(boolean paramOrder){ this.paramOrder = paramOrder; }
    public StringAppend(){ this(true); }

    @Override
    public String eval(Routine s, Object... params) {
        return String.valueOf(params[paramOrder?1:0]) + params[paramOrder?0:1];
    }

    @Override
    public Class<?>[] getParamTypes() {
        return new Class<?>[]{ Object.class, Object.class };
    }

    @Override
    public Class<String> getReturnType() {
        return String.class;
    }
}
