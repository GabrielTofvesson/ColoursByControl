package net.tofvesson.libtlang;

public class Variable<T>{

    protected T value;
    protected final Class<T> type;

    public Variable(T t, Class<T> type){
        this.type = type;
        handleSet(t);
    }
    public Variable( Class<T> type){ this(null, type); }

    public void handleSet(Object value){
        if(value!=null && !type.isAssignableFrom(value.getClass())) throw new RuntimeException("Variable assignment type mismatch: "+value);
        this.value = (T) (Number.class.isAssignableFrom(type) && value==null ? 0 : (value instanceof Number && Number.class.isAssignableFrom(type)) ? ((Number) value).doubleValue() : value);
    }

    public T getValue(){ return value; }
}
