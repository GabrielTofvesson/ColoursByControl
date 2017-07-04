package net.tofvesson.libtlang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class Routine<T> implements Instruction<T> {

    protected final Class<T> returnType;
    protected final Class<?>[] paramTypes;

    protected boolean retroNames = false;

    protected int stackPointer = 0;
    protected List<Instruction<?>> set = new ArrayList<>();
    protected HashMap<String, Variable<?>> vars = new HashMap<>();
    protected Stack<Object> operands = new Stack<>();
    protected boolean strictOperands = true;

    public Routine(Class<T> returnType, Class<?>... paramTypes){ this.returnType = returnType; this.paramTypes = paramTypes; }

    @Override
    public T eval(Routine<?> s, Object... params) {
        for(int i = 0; i<params.length; ++i) set.add(0, new DeclareVariable<>("$"+i, Object.class, params[i]));
        operands.clear();
        if(!retroNames){
            for(int i = 0; i<set.size(); ++i) if(set.get(i) instanceof RetroGenGoto) ((RetroGenGoto)set.get(i)).retrogen(this);
            retroNames = true;
        }
        Instruction<?> i;
        for(stackPointer = 0; stackPointer < set.size(); ++stackPointer){
            Object o = (i=set.get(stackPointer)).eval(this, popOpStack(i.getParamTypes()));
            if(i.getReturnType()!=Void.class) pushOpStack(o);
        }
        vars.clear();
        if(strictOperands && ((returnType==Void.class && operands.size()!=0)||
                (returnType!=Void.class && operands.size()>1)||
                (returnType!=Void.class && operands.peek()!=null && !returnType.isAssignableFrom(operands.peek().getClass()))))
            throw new RuntimeException("Unclean operand stack at end of routine: "+ Arrays.toString(operands.toArray()));
        List<DeclareVariable<?>> idx = new ArrayList<>();
        for(int j = 0; j<params.length; ++j)
            for(int k = 0; k<set.size(); ++k)
                if(set.get(k) instanceof DeclareVariable && ((DeclareVariable<?>)set.get(k)).name.equals("$"+j))
                    idx.add((DeclareVariable<?>) set.get(k));
        set.removeAll(idx);
        //noinspection unchecked
        return returnType==Void.class ? null : operands.size() == 0 ? null : (T) operands.pop();
    }

    public T eval(){ return eval(null); }

    protected Object[] popOpStack(Class<?>[] operandTypes){
        if(operandTypes.length>operands.size()) throw new ExecutionException(stackPointer, "Operand stack underflow (Operator stack pointer value: "+stackPointer+")");
        Object o;
        for(int i = 0; i < operandTypes.length; ++i)
            if(((o=operands.get(operands.size()-i-1))==null && operandTypes[i]==Number.class)
                    || (o!=null && !operandTypes[i].isAssignableFrom(o.getClass()) && !Number.class.isAssignableFrom(operandTypes[i]) && !String.class.isAssignableFrom(operandTypes[i])))
                throw new ExecutionException(stackPointer, "Parameter mismatch for routine eval: "+Arrays.toString(operands.toArray()));
        Object[] o1 = new Object[operandTypes.length];
        for(int i = 0; i < operandTypes.length; ++i)
            o1[i] = String.class==operandTypes[i] ? String.valueOf(operands.pop()) :
                    Number.class.isAssignableFrom(operandTypes[i]) ? Double.parseDouble(String.valueOf(operands.pop())) : operands.pop();
        return o1;
    }

    protected void pushOpStack(Object v){ operands.push(v); }

    public void pushMath(MathOp op, String var1, String var2){
        loadVar(var1);
        loadVar(var2);
        set.add(op);
    }

    public void pushMath(MathOp op, Number var1, String var2){
        set.add(new StackPush(var1));
        loadVar(var2);
        set.add(op);
    }

    public void pushMath(MathOp op, String var1, Number var2){
        loadVar(var1);
        set.add(new StackPush(var2));
        set.add(op);
    }

    public void pushMath(MathOp op, Number var1, Number var2){
        set.add(new StackPush(var1));
        set.add(new StackPush(var2));
        set.add(op);
    }

    public void pushMath(MathOp op, Routine<? extends Number> var1, String var2){
        set.add(var1);
        loadVar(var2);
        set.add(op);
    }

    public void pushMath(MathOp op, String var1, Routine<? extends Number> var2){
        loadVar(var1);
        set.add(var2);
        set.add(op);
    }

    public void pushMath(MathOp op, Routine<? extends Number> var1, Routine<? extends Number> var2){
        set.add(var1);
        set.add(new StackPush(var2));
        set.add(op);
    }

    public void pushMath(MathOp op, Routine<? extends Number> var1, Number var2){
        set.add(var1);
        set.add(new StackPush(var2));
        set.add(op);
    }

    public void pushMath(MathOp op, Number var1, Routine<? extends Number> var2){
        set.add(new StackPush(var1));
        set.add(var2);
        set.add(op);
    }

    public void pushPop(int maxPopCount){
        set.add(new StackPush(maxPopCount));
        set.add(StandardOps.StackPop);
    }

    public void pushPop(String maxPopCount){
        loadVar(maxPopCount);
        set.add(StandardOps.StackPop);
    }

    public void pushPop(){ pushPop(1); }

    public void loadVar(String var){
        set.add(new StackPush(var));
        set.add(StandardOps.LoadVar);
        set.add(StandardOps.ReadVar);
    }

    public void storeVal(String var, Object val){
        set.add(new StackPush(var));
        set.add(StandardOps.LoadVar);
        set.add(new StackPush(val));
        set.add(StandardOps.StoreVar);
    }

    public void gotoLabel(String name, boolean clearOperands){
        set.add(new StackPush(clearOperands));
        set.add(new StackPush(name));
        set.add(StandardOps.GOTO);
    }

    public <V> void condition(V _true, V _false){
        set.add(StandardOps.Condition);
        RetroGenGoto t = new RetroGenGoto();
        set.add(new StackPush(_true));
        set.add(new StackPush(false));
        set.add(t);
        set.add(new StackPush(_false));
        set.add(t.link);
    }

    public void gotoLabel(String name){ gotoLabel(name, true); }

    public void add(Instruction<?> i){ set.add(i); }

    public void print(int output){
        set.add(new StackPush(0));
        set.add(StandardOps.Debug);
    }

    public void print(String s, int output){
        set.add(new StackPush(s));
        set.add(new StackPush(output));
        set.add(StandardOps.Debug);
    }

    public void inline(Routine<?> r){
        r.strictOperands = false;
        r.operands = operands; // Simulate inline actions (as if it were run in the same routine)
        set.add(r);
    }

    public void swapStack(int idx1, int idx2){
        set.add(new StackPush(idx1));
        set.add(new StackPush(idx2));
        set.add(StandardOps.StackSwap);
    }

    List<Instruction<?>> getSet(){ return set; }
    HashMap<String, Variable<?>> getVars(){ return vars; }
    Stack<Object> getOperands(){ return operands; }
    public int getStackPointer(){ return stackPointer; }
    int setStackPointer(int newVal){ return stackPointer = newVal; }

    @Override public Class<?>[] getParamTypes() { return paramTypes; }

    @Override public Class<T> getReturnType() { return returnType; }
}
