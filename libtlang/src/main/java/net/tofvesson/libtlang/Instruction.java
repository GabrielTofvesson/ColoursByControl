package net.tofvesson.libtlang;

public interface Instruction<T> { T eval(Routine<?> s, Object... params); Class<?>[] getParamTypes(); Class<T> getReturnType(); }