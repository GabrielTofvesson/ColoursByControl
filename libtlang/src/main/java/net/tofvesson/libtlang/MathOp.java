package net.tofvesson.libtlang;

public enum MathOp implements Instruction<Double> {
    ADD {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return n1 + n2;
        }
    }, SUB {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return n1 - n2;
        }
    }, MUL {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return n1 * n2;
        }
    }, DIV {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return n1 / n2;
        }
    }, POW {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return Math.pow(n1, n2);
        }
    }, MOD {
        @Override
        public Double evaluate(Double n1, Double n2) {
            return n1 % n2;
        }
    };

    public abstract Double evaluate(Double n1, Double n2);

    @Override
    public Class<?>[] getParamTypes() {
        return new Class<?>[]{Number.class, Number.class};
    }

    @Override
    public Class<Double> getReturnType() {
        return Double.class;
    }

    @Override
    public Double eval(Routine<?> s, Object... params) {
        //noinspection unchecked
        return evaluate(((Number) params[0]).doubleValue(), ((Number) params[1]).doubleValue());
    }
}
