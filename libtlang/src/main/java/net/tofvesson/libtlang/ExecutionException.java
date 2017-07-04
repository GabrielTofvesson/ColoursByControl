package net.tofvesson.libtlang;

public class ExecutionException extends RuntimeException {
    public final int stackPointer;
    public ExecutionException(int stackPointer) { this.stackPointer = stackPointer; }
    public ExecutionException(int stackPointer, String message) { super(message); this.stackPointer = stackPointer; }
    public ExecutionException(int stackPointer, String message, Throwable cause) { super(message, cause); this.stackPointer = stackPointer;}
    public ExecutionException(int stackPointer, Throwable cause) { super(cause); this.stackPointer = stackPointer; }
}
