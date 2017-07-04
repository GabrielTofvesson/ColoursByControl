package net.tofvesson.libtlang;

public class Label implements Instruction {
    protected String name;
    public Label(String name){ this.name = name; }
    @Override public Object eval(Routine s, Object... params) { return null; }
    @Override public Class<?>[] getParamTypes() { return new Class<?>[0]; }
    @Override public Class getReturnType() { return Void.class; }
    public String getName(){ return name; }
}
