package net.tofvesson.coloursbycontrol

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

infix fun <K, V> HashMap<K, V>.containsKeyI(key: K) = contains(key)
fun Any?.asBoolean(): Boolean =
        if(this==null) false else this as? Boolean ?: if((this.toString() == "true") or (this.toString() == "false")) this.toString().toBoolean() else !((this.toString()=="0") or (this.toString()=="0.0"))
fun Any?.asDouble(): Double = if(this==null) 0.0 else (this as? Number ?: try{ this.toString().toDouble() }catch(e: NumberFormatException){ 0.0 }).toDouble()

fun exception(stack: Stack<CodeCtx>, reason: String?): RuntimeException =
        RuntimeException((reason ?: "")+"  Trace: "+Arrays.toString(stack.toArray())+
                (if(stack.size>0) " at "+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].ordinal+" ("+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].name+")" else ""))

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
            val m = Class.forName(params[0].toString()).getDeclaredMethod(params[1].toString(), Object::class.java)
            m.isAccessible = true
            stack[stack.size-1].popFlag = m.returnType==Void::class.java
            val static = (m.modifiers and 0x8) == 0
            val paramCount = m.parameterTypes.size + if(static) 0 else 1
            val caller = stack[stack.size-1]
            if(paramCount > caller.operands.size) throw exception(stack, "Operand stack underflow! Required parameter count: "+paramCount)
            val callee = if(static) null else caller.operands.pop()
            val v = Array(paramCount, { caller.operands.pop() })
            caller.popFlag = m.returnType == Void.TYPE
            return m.invoke(callee, v)
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