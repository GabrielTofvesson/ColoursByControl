package net.tofvesson.libtlang;

public class DeclareVariable<T> implements Instruction {

    protected final Variable<T> var;
    protected final String name;
    protected final T initialValue;

    public DeclareVariable(String name, Class<T> type, T initialValue){
        var = new Variable<>(initialValue, type);
        this.name = name;
        this.initialValue = initialValue;
    }

    @Override
    public Object eval(Routine s, Object... params) {
        if(s.vars.containsKey(name)) throw new ExecutionException(s.stackPointer, "Attempted to declare variable that already exists: "+name);
        var.handleSet(initialValue);
        s.vars.put(name, var);
        return null;
    }

    @Override public Class<?>[] getParamTypes() { return new Class<?>[0]; }
    @Override public Class getReturnType() { return Void.class; }
}
