package net.tofvesson.libtlang;


import java.util.Set;

public enum StandardOps implements Instruction {
    Condition{
        @Override
        public Object eval(Routine s, Object... params) {
            if(!((Boolean) params[0])) s.stackPointer+=3;
            return null;
        }
        @Override public Class<?>[] getParamTypes() { return new Class<?>[]{ Boolean.class }; }
        @Override public Class getReturnType() { return Void.class; }
    },
    Debug{
        @Override
        public Void eval(Routine s, Object... params) {
            (((Number) params[0]).intValue()%2 == 0 ? System.out : System.err).println(String.valueOf(params[1]));
            return null;
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Number.class, Object.class };
        }

        @Override
        public Class<Void> getReturnType() {
            return Void.class;
        }
    },
    GOTO{
        @Override
        public Object eval(Routine s, Object... params) {
            Instruction t;
            for(int i =0 ; i<s.set.size(); ++i) if((t=(Instruction) s.set.get(i)) instanceof Label && ((Label)t).getName().equals(params[0])){ s.stackPointer = i; break; }
            if(((Boolean) params[1])) s.operands.clear();
            return null;
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ String.class, Boolean.class };
        }

        @Override
        public Class getReturnType() {
            return Void.class;
        }
    },
    LoadVar{
        @Override
        public Variable<?> eval(Routine s, Object... params) {
            for(String n : (Set<String>) s.vars.keySet()) if(n == params[0] || (n != null && n.equals(params[0]))) return (Variable<?>) s.vars.get(n);
            throw new ExecutionException(s.stackPointer, "Couldn't find any variable called "+params[0]);
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ String.class };
        }

        @Override
        public Class getReturnType() {
            return Variable.class;
        }
    },
    StoreVar{
        @Override
        public Object eval(Routine s, Object... params) {
            ((Variable<?>)params[1]).handleSet(params[0]);
            return null;
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Object.class, Variable.class };
        }

        @Override
        public Class getReturnType() {
            return Void.class;
        }
    },
    ReadVar{
        @Override
        public Object eval(Routine s, Object... params) {
            return ((Variable<?>) params[0]).getValue();
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Variable.class };
        }

        @Override
        public Class getReturnType() {
            return Object.class;
        }
    },
    StackPop{
        @Override
        public Object eval(Routine s, Object... params) {
            for(int i = Math.min(((Number)params[0]).intValue(), s.operands.size()); i>0; --i) s.operands.pop();
            return null;
        }

        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Number.class };
        }

        @Override
        public Class getReturnType() {
            return Void.class;
        }
    },
    NOP{
        @Override public Class<?>[] getParamTypes() { return new Class<?>[0]; }
        @Override public Class getReturnType() { return Void.class; }
        @Override public Object eval(Routine s, Object... params) { return null; }
    },
    DUP{
        @Override public Class<?>[] getParamTypes() { return new Class<?>[0]; }
        @Override public Class getReturnType() { return Object.class; }
        @Override public Object eval(Routine s, Object... params) { return s.operands.peek(); }
    },
    StackSwap{
        @Override public Class<?>[] getParamTypes() { return new Class<?>[]{ Number.class, Number.class }; }
        @Override public Class getReturnType() { return Void.class; }
        @Override public Object eval(Routine s, Object... params) {
            int i1 = s.operands.size()-1-((Number)params[0]).intValue(), i2 = s.operands.size()-1-((Number)params[1]).intValue();
            Object o = s.operands.get(i1);
            //noinspection unchecked
            s.operands.set(i1, s.operands.get(i2));
            //noinspection unchecked
            s.operands.set(i2, o);
            return null;
        }
    }
}
