package net.tofvesson.coloursbycontrol

import java.lang.reflect.Method
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and

class CodeCtx(ctxName: String, const: Array<String>, code: ByteArray): Operation{
    val name = ctxName
    val vars = HashMap<String, Any?>()
    val pairedCalls = ArrayList<Pair<Int, Byte>>()
    val operands = Stack<Any?>()
    val operators = ArrayList<Operations>()
    val hasReturn = !(code.isEmpty() || ((code[0] and -128).toInt() ushr 7) == 0)
    val paramCount = if(code.isEmpty()) 0 else code[0] and 0b01111111
    val constants = const
    var stackPointer = -1
    var popFlag = false
    var isRunning = false

    private constructor(ctxName: String, const: Array<String>, op: ArrayList<Operations>, pC: ArrayList<Pair<Int, Byte>>) : this(ctxName, const, kotlin.ByteArray(0)) {
        operators.addAll(op)
        for((i, b) in pC) pairedCalls.add(Pair(i, b))
    }

    init{
        var first = true
        var paired = 0
        for(b in code){
            if(!first){
                if(paired>0){
                    pairedCalls.add(Pair(operators.size-1, b))
                    --paired
                }else{
                    val o = Operations.values()[b.toInt()]
                    paired = o.getPairCount()
                    operators.add(o)
                }
            }
            first = false
        }
        var v = paramCount+1
        while(--v!=0) vars.put(v.toString(), null)
    }

    override fun hasReturnValue(): Boolean = hasReturn
    override fun getParamCount(): Int = paramCount.toInt()

    override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
        if(isRunning) return loadContext(stack, name).eval(stack, params) // Prevent errors with multi-threading or self-referencing code
        isRunning = true
        stack.push(this)
        if(params.size > paramCount) throw exception(stack, "Context given too many parameters!")
        params.forEachIndexed { index, it -> vars[index.toString()] = it }
        while (stackPointer<operators.size-1){
            val o = operators[++stackPointer]
            val a = o.eval(stack, loadOperands(stack, o))
            if(o.hasReturnValue()) operands.push(a)
            if(popFlag) operands.pop()
            popFlag = false
        }
        stack.pop()
        val v = if(hasReturn) operands.pop() else null
        operands.clear()
        stackPointer = -1
        isRunning = false
        return v
    }

    private fun loadOperands(stack: Stack<CodeCtx>, op: Operations): Array<Any?> {
        if(op.getParamCount()>operands.size) throw exception(stack, "Operand stack underflow!")
        return Array(op.getParamCount() + op.getPairCount(), {
            if(it<op.getParamCount()) operands.pop() else loadPairedVal(stack, it-op.getParamCount(), stackPointer)
        })
    }

    private fun loadPairedVal(stack: Stack<CodeCtx>, which: Int, atStack: Int): Byte{
        var count = 0
        for((first, second) in pairedCalls)
            if(first == atStack){
                if(count==which) return second
                else ++count
            }
        throw exception(stack, "Can't load paired call value $which from $atStack!")
    }

    fun loadContext(stack: Stack<CodeCtx>, ctxName: String): CodeCtx {
        // TODO: Make better 'n load from files 'n other stuff
        if(ctxName == name) return CodeCtx(name, constants, operators, pairedCalls)
        else throw exception(stack, "Could not find any context called \"$ctxName\"!") // TODO: Do search for other contexts
    }
    override fun toString(): String = "Context{name=$name, pointer=$stackPointer ("+operators[stackPointer].name+")}"
}

interface Operation{
    fun hasReturnValue(): Boolean
    fun getParamCount(): Int
    fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any?
}

infix fun <K, V> HashMap<K, V>.containsKeyI(key: K) = containsKey(key)
fun Any?.asBoolean(): Boolean =
        if(this==null) false else this as? Boolean ?: if((this.toString() == "true") or (this.toString() == "false")) this.toString().toBoolean() else !((this.toString()=="0") or (this.toString()=="0.0"))
fun Any?.asDouble(): Double = if(this==null) 0.0 else (this as? Number ?: try{ this.toString().toDouble() }catch(e: NumberFormatException){ 0.0 }).toDouble()
infix fun Any?.matchesClass(other: Class<*>): Boolean =
        (other.isPrimitive && this!=null && this.javaClass==other) || (this==null && !other.isPrimitive) || (this!=null && other.javaClass.isAssignableFrom(this.javaClass))

fun exception(stack: Stack<CodeCtx>, reason: String?): RuntimeException =
        RuntimeException((reason ?: "")+"  Trace: "+Arrays.toString(stack.toArray())+
                (if(stack.size>0) " at "+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].ordinal+" ("+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].name+")" else ""))
@Suppress("UNCHECKED_CAST")
infix fun <T: Number, K: Number> K.toNumber(type: Class<in T>): T =
        (if(type==Byte::class) this.toByte()
        else if(type==Short::class) this.toShort()
        else if(type==Integer::class) this.toInt()
        else if(type==Long::class) this.toLong()
        else if(type==Float::class) this.toFloat()
        else if(type==Double::class) this.toDouble()
        else this) as T
fun getMatchingMethod(name: String, paramCount: Int, clazz: Class<*>, paramTypes: Array<Any?>): Method? {
    @Suppress("UNCHECKED_CAST")
    val methods = ArrayList<Method>()
    clazz.declaredMethods.forEach { if(it.name == name && it.parameterTypes.size == paramCount) methods.add(it) }
    OUTER@
    for(method in methods){
        var i = -1
        for(clasz in method.parameterTypes)
            if(!((paramTypes[++i] matchesClass String::class.java && (
                    (clasz==Double::class.java) ||
                            (clasz==Integer::class.java) ||
                            (clasz==Float::class.java) ||
                            (clasz==Long::class.java) ||
                            (clasz==Short::class.java) ||
                            (clasz==Byte::class.java) ||
                            (clasz==Character::class.java) ||
                            (clasz==Boolean::class.java))) ||
                    ((paramTypes[i]!=null && clasz.isAssignableFrom(paramTypes[i]!!.javaClass)) || ((paramTypes[i]==null) && (!clasz.isPrimitive))) || (clasz == String::class)))
                continue@OUTER
        return method
    }
    return null
}

infix fun Array<Any?>.toRequiredTypes(types: Array<Class<*>>): Array<Any?> =  Array(this.size, {
        index -> run{
            if(this[index] matchesClass types[index]) this[index]
            else if(Number::class.java.isAssignableFrom(types[index])) (this[index] as Number) toNumber types[index]
            else this[index].toString()
        }})

enum class Operations: Operation{
    LDV{ // Load variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
            var i = stack.size-1
            while(i!=-1){
                if(stack[i].vars containsKeyI params[0].toString()){
                    return stack[i].vars[params[0].toString()]
                }
                --i
            }
            throw exception(stack, "Variable \""+params[0]+"\" cannot be found!")
        }
        override fun getPairCount(): Int = 0
    },
    STV{ // Store variable
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): String? {
            var i = stack.size
            while(i!=-1){
                if(stack[i].vars containsKeyI params[0].toString()){
                    stack[i].vars.put(params[0].toString(), params[1].toString())
                    return null
                }
                --i
            }
            throw exception(stack, "Variable \""+params[0]+"\" cannot be found!")
        }
        override fun getPairCount(): Int = 0
    },
    LDC{ // Load constant
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): String? = stack[stack.size-1].constants[params[0].asDouble().toInt()]
        override fun getPairCount(): Int = 1
    },
    EXT{ // Call external
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
            val caller = stack[stack.size-1]
            val c = Class.forName(params[0].toString())
            if(params[2].toString().toInt() > caller.operands.size) throw exception(stack, "Operand stack underflow! Required parameter count: "+params[2].toString())
            val callParams = Array(params[2].toString().toInt(), { caller.operands.pop() })
            val callMethod = getMethod(callParams, params[1].toString(), c) ?: throw exception(stack, "Cannot find Method named \""+params[1]+"\"")
            caller.popFlag = callMethod.returnType==Void.TYPE
            return invoke(callMethod, callParams)
        }
        override fun getPairCount(): Int = 1
    },
    POP{ // Pop one operand
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): String? = null
        override fun getPairCount(): Int = 0
    },
    DCV{ // Declare variable
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
            if(stack[stack.size-1].vars containsKeyI params[0].toString()) return null
            stack[stack.size-1].vars.put(params[0].toString(), params[1])
            return null
        }
        override fun getPairCount(): Int = 0
    },
    CMP{ // Compare
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = (params[0].toString() == params[1].toString()) or (if(params[0]==null) params[1]==null else params[0]?.equals(params[1]) as Boolean)
        override fun getPairCount(): Int = 0
    },
    LNT{ // Logical NOT
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = !params[0].asBoolean()
        override fun getPairCount(): Int = 0
    },
    LOR{ // Logical OR
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = params[0].asBoolean() or params[1].asBoolean()
        override fun getPairCount(): Int = 0
    },
    CND{ // Conditional Jump (false = jump 2, true = no jump)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = if(!params[0].asBoolean()) stack[stack.size-1].stackPointer+=2 else 0
        override fun getPairCount(): Int = 0
    },
    JMP{ // Unconditional jump (1 operand)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any?{
            stack[stack.size-1].stackPointer = (if(params[0].asBoolean()) stack[stack.size-1].stackPointer else 0) + params[1].asDouble().toInt()
            return null
        }
        override fun getPairCount(): Int = 0
    },
    CALL{ // Call context
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
            val caller = stack[stack.size-1]
            val ctx = caller.loadContext(stack, params[0].toString())
            if(ctx.getParamCount() > caller.operands.size) throw exception(stack, "Operand stack underflow! Required parameter count: "+ctx.getParamCount())
            caller.popFlag = !ctx.hasReturn
            return ctx.eval(stack, Array(ctx.getParamCount(), { caller.operands.pop() }))
        }
        override fun getPairCount(): Int = 0
    },
    LDN{ // Load constant number
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = params[0]
        override fun getPairCount(): Int = 1
    },
    CJP{ // Constant jump (unconditional, no operands)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? {
            stack[stack.size-1].stackPointer += params[0].asDouble().toInt()
            return null
        }
        override fun getPairCount(): Int = 1
    },
    VLD{ // Constant load from variable (load constant based on value of variable)
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = stack[stack.size-1].constants[params[0].asDouble().toInt()]
        override fun getPairCount(): Int = 0
    },
    NOP{ // No operation
        override fun hasReturnValue(): Boolean  = false
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = null
        override fun getPairCount(): Int = 0
    },
    INC{ // Increment variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = params[0].asDouble() + 1
        override fun getPairCount(): Int = 0
    },
    DEC { // Decrement variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = params[0].asDouble() - 1
        override fun getPairCount(): Int = 0
    },
    LOP{ // Logical operation
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? =
            if(params[2]==0) params[0].asDouble() + params[1].asDouble()
            else if(params[2]==1) params[0].asDouble() - params[1].asDouble()
            else if(params[2]==2) params[0].asDouble() * params[1].asDouble()
            else if(params[2]==3) params[0].asDouble() / params[1].asDouble()
            else if(params[2]==4) params[0].asDouble() % params[1].asDouble()
            else throw exception(stack, "Invalid operation "+params[2])
        override fun getPairCount(): Int = 1
    },
    DUP{ // Duplicate top operand
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeCtx>, params: Array<Any?>): Any? = stack[stack.size-1].operands.push(params[0])
        override fun getPairCount(): Int = 0
    };
    abstract fun getPairCount(): Int
}

/**
 * Selects the most plausible method that caller is trying to get based on method name, parameters and owning class etc.
 */
fun getMethod(params: Array<Any?>, name: String, owner: Class<*>): Method? {
    val allMethods = ArrayList<Method>()
    var cur = owner
    while(cur!=Object::javaClass){
        cur.declaredMethods.filterNot {
            it.name != name || allMethods.contains(it) || (it.parameterTypes.size<params.size && (it.modifiers and 8==0)) || (it.parameterTypes.size<params.size-1 && (it.modifiers and 8==1))
        }.forEach { allMethods.add(it) }
        cur = cur.superclass
    }

    var match: Method? = null

    outer@
    for(it in allMethods) {
        if(match==null) match = it
        else if(it.parameterTypes.size<match.parameterTypes.size){
            for(i in match.parameterTypes.indices)
                if(i<params.size && !(getOrCreate(match.parameterTypes[i], params[i], true, false) as Boolean))
                    continue@outer
            match = it
        }
    }
    return match
}

fun invoke(call: Method, params: Array<Any?>): Any? {
    try{
        call.isAccessible = true
        return call.invoke(if(call.modifiers and 8 == 0) params[0] else null, Array(call.parameterTypes.size - (((call.modifiers and 8) shr 3) and 0), { if(it + (if((call.modifiers and 8)==1) 0 else 1)>=params.size) null else getOrCreate(call.parameterTypes[0], params[it + (if((call.modifiers and 8)==1) 0 else 1)], true, true) }))
    }catch(e: Exception){ return null }
}

fun getOrCreate(matchType: Class<*>, param: Any?, ignoreSafety: Boolean, create: Boolean): Any? {
    if(param==null || matchType.isAssignableFrom(param.javaClass)) { return if(create) param else return true }

    // At this point, we know that "param" MUST be non-null
    matchType.declaredConstructors
            .filter { (ignoreSafety || it.isAccessible) && it.parameterTypes.size==1 && (it.parameterTypes[0].isAssignableFrom(param.javaClass) || it.parameterTypes[0].isPrimitive) }
            .forEach {
                try{
                    it.isAccessible = true
                    return if(create) it.newInstance(if(it.parameterTypes[0].isPrimitive){ if(it.parameterTypes[0]==Boolean::class.java) param.asBoolean() else param.asDouble().toNumber(it.parameterTypes[0])} else param) else true
                }catch (e: Exception){}
            }
    matchType.declaredMethods
            .filter { (ignoreSafety || it.isAccessible) && ((it.modifiers and 8)!=0) && it.parameterTypes.size==1 && (it.parameterTypes[0].isAssignableFrom(param.javaClass) || it.parameterTypes[0].isPrimitive) }
            .forEach {
                try{
                    it.isAccessible = true
                    return if(create) it.invoke(null, if(it.parameterTypes[0].isPrimitive){ if(it.parameterTypes[0]==Boolean::class.java) param.asBoolean() else param.asDouble().toNumber(it.parameterTypes[0])} else param) else true
                }catch (e: Exception){}
            }
    return if(create) null else false
}