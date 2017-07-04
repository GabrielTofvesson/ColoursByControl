package net.tofvesson.libtlang;

public class StackPush implements Instruction {

    protected final Object item;

    public StackPush(Object o){ item = o; }

    @Override
    public Object eval(Routine s, Object... params) {
        //noinspection unchecked
        s.operands.push(item);
        return null;
    }

    @Override
    public Class<?>[] getParamTypes() {
        return new Class<?>[0];
    }

    @Override
    public Class getReturnType() {
        return Void.class;
    }
}
