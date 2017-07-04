package net.tofvesson.libtlang;

public enum ConditionOp implements Instruction {
    EQU{
        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Object.class, Object.class };
        }

        @Override
        public Object eval(Routine s, Object... params) {
            return (params[0]==null && params[1]==null) || (params[0]!=null && params[0].equals(params[1])) || (params[1]!=null && params[1].equals(params[0]));
        }
    }, NOT{
        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Boolean.class };
        }
        @Override
        public Object eval(Routine s, Object... params) {
            return !((Boolean)params[0]);
        }
    }, OR{
        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Boolean.class, Boolean.class };
        }
        @Override
        public Object eval(Routine s, Object... params) {
            return ((Boolean)params[0]) || ((Boolean)params[1]);
        }
    }, AND{
        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Boolean.class, Boolean.class };
        }
        @Override
        public Object eval(Routine s, Object... params) {
            return ((Boolean)params[0]) && ((Boolean)params[1]);
        }
    }, XOR{
        @Override
        public Class<?>[] getParamTypes() {
            return new Class<?>[]{ Boolean.class, Boolean.class };
        }
        @Override
        public Boolean eval(Routine s, Object... params) {
            return ((Boolean)params[0]) ^ ((Boolean)params[1]);
        }
    };


    @Override public final Class getReturnType() { return Boolean.class; }
}
