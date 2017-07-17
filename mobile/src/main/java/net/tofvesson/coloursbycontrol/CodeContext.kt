@file:Suppress("unused")

package net.tofvesson.coloursbycontrol

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.experimental.and
import kotlin.experimental.or


class ContextNotFoundException(message: String?) : Exception(message)
class ContextAlreadyLoadedException(message: String?) : RuntimeException(message)
class CodeMismatchException(message: String?) : RuntimeException(message)
class ValidationException(message: String?): Exception(message)


fun ByteArray.subSequence(start: Int, end: Int): ByteArray = ByteArray(end-start, {this[it+start]})
inline fun <reified T> Array<T>.subSequence(start: Int, end: Int): Array<T> = Array(end-start, {this[it+start]})
inline fun <reified T> Array<T>.editElement(index: Int, action: (T) -> T) { this[index] = action(this[index]) }
inline fun <reified T> Array<T>.lastElement(): T = this[this.lastIndex]
inline fun <reified T> ArrayList<T>.lastElement(): T = this[this.lastIndex]
fun String.splitExcept(ignore: String, regex: String): Array<String>{
    val refined = ArrayList<String>()
    val beforeRGX = ignore.indexOf(regex)
    this.split(regex).forEach {
        if(!refined.isEmpty() && (refined.lastElement().substring(refined.lastElement().length-beforeRGX) + regex + it).startsWith(ignore)) refined[refined.lastIndex] = refined[refined.lastIndex] + regex + it
        else refined.add(it)
    }
    return refined.toTypedArray()
}
inline fun <reified T> Array<T>.forEachUpdate(action: (T) -> T){
    for(i in this.indices) this[i] = action(this[i])
}

class CodeBuilder(hasReturn: Boolean, paramSize: Byte, internal val name: String, vararg constants: String){
    constructor(hasReturn: Boolean, paramSize: Int, name: String, vararg constants: String): this(hasReturn, paramSize.toByte(), name, *constants)
    constructor(hasReturn: Boolean, name: String, vararg constants: String): this(hasReturn, 0.toByte(), name, *constants)
    constructor(paramSize: Byte, name: String, vararg constants: String): this(false, paramSize, name, *constants)
    constructor(paramSize: Int, name: String, vararg constants: String): this(false, paramSize.toByte(), name, *constants)
    constructor(name: String, vararg constants: String): this(false, 0.toByte(), name, *constants)

    var signature: Byte = ((if(hasReturn) 0b10000000 else 0) or (paramSize.toInt() and 0b01111111)).toByte()
    internal val code = ArrayList<Byte>()
    internal val constantPool = ArrayList<String>()
    init{ constants.forEach { constantPool.add(it) } }
    fun add(operation: Byte, vararg pairedValues: Byte): CodeBuilder {
        if(operation<0 || operation>=Operations.values().size) throw ValidationException("Operation $operation is out of range!")
        var pairCount = -1
        code.add(operation)
        while(++pairCount<Operations.values()[operation.toInt()].getPairCount()) code.add(if(pairCount>=pairedValues.size) 0 else pairedValues[pairCount])
        return this
    }
    fun add(operation: Operations, vararg pairedValues: Byte): CodeBuilder = add(operation.ordinal.toByte(), *pairedValues)
    fun add(operation: Byte, vararg pairedValues: Int) = add(operation, *ByteArray(pairedValues.size, { pairedValues[it].toByte() }))
    fun add(operation: Operations, vararg pairedValues: Int) = add(operation, *ByteArray(pairedValues.size, { pairedValues[it].toByte() }))
    fun setHasReturn(hasReturn: Boolean){ signature = (signature and 0b01111111) or (if(hasReturn) 0b10000000 else 0).toByte() }
    fun setParamCount(paramCount: Byte){ signature = (signature and -128) or (paramCount and 0b01111111) }
    fun addConstant(const: String): Int {
        val v: Int = constantPool.indices.firstOrNull { constantPool[it]==const } ?: -1
        if(v==-1) constantPool.add(const)
        return if(v==-1) constantPool.size-1 else v
    }
}

open class ContextPool(superContextPool: ContextPool?) {
    companion object {
        internal val signature = ByteArray(8, {
            if(it==0) 1.toByte()
            else if(it<3)  3.toByte()
            else if(it==3) 7.toByte()
            else if(it==4) 116.toByte()
            else if(it==5) 108.toByte()
            else if(it==6) 110.toByte()
            else           103.toByte()
        })
        fun getSignature(): ByteArray = signature.clone()
        internal fun sign(code: ArrayList<Byte>) = getSignature().forEachIndexed { index, byte -> code.add(index, byte) }
        internal fun addPool(code: ArrayList<Byte>, name: String, vararg constants: String){
            val offset = if(isSigned(code)) 8 else 0
            var consts = ""
            for(s in constants) consts += s.replace("%", "\\%") + "%"
            (consts+name+";").toByteArray().forEachIndexed({index, byte -> code.add(index+offset, byte) })
        }
        internal fun addCode(codeTarget: ArrayList<Byte>, code: ByteArray) = code.forEach{ codeTarget.add(it) }
        internal fun addCode(codeTarget: ArrayList<Byte>, code: ArrayList<Byte>) = codeTarget.addAll(code)
        private fun isSigned(code: List<Byte>): Boolean{
            if(code.size<8) return false
            signature.forEachIndexed({ index, byte -> if(code[index]!=byte) return false })
            return true
        }
    }
    constructor() : this(null)
    protected val superPool: ContextPool? = superContextPool
    protected val pool = ArrayList<CodeContext>()

    @Throws(ContextAlreadyLoadedException::class, CodeMismatchException::class)
    fun load(code: ByteArray): CodeContext {
        getSignature().forEachIndexed { index, byte -> if(code[index] != byte) throw CodeMismatchException("Invalid signature!") }
        val codeIdx: Int =
                code.indices.firstOrNull {
                    code[it]==0x3b.toByte() && (it==0 || code[it-1]!='\\'.toByte())
                } ?: throw CodeMismatchException("Constant pool not delimited!")
        val constants: Array<String> = String(code.subSequence(8, codeIdx)).splitExcept("\\%", "%")
        if(constants.isEmpty()) throw CodeMismatchException("No context name supplied!")
        constants.forEachUpdate { it.replace("\\%", "%") }
        try{
            lookup(constants.lastElement())
            throw ContextAlreadyLoadedException("Cannot load context "+constants.lastElement())
        }catch(e: ContextNotFoundException){}
        val v = _loadAfter(_loadBefore(constants.lastElement(), code.subSequence(codeIdx+1, code.size), constants.subSequence(0, constants.lastIndex)))
        pool.add(v)
        return v
    }
    fun load(code: List<Byte>): CodeContext = load(ByteArray(code.size, { code[it] }))
    fun load(code: CodeBuilder): CodeContext{
        val collected = ArrayList<Byte>()
        sign(collected)
        addPool(collected, code.name, *code.constantPool.toTypedArray())
        collected.add(code.signature)
        addCode(collected, code.code)
        return load(collected)
    }

    fun load(context: CodeContext): CodeContext {
        try{
            lookup(context.name)
            throw ContextAlreadyLoadedException("Cannot load context "+context.name)
        }catch(e: ContextNotFoundException){ }
        pool.add(context)
        return context
    }

    @Throws(ContextNotFoundException::class)
    fun lookup(name: String): CodeContext {
        return _lookup(name) ?: superPool?.lookup(name) ?: throw ContextNotFoundException(name)
    }

    protected open fun _lookup(name: String): CodeContext?{
        pool.filter { it.name==name }.forEach { return it }
        return null
    }

    protected open fun _loadBefore(name: String, code: ByteArray, constants: Array<String>): CodeContext { return CodeContext(name, constants, code, this) }
    protected open fun _loadAfter(ctx: CodeContext): CodeContext = ctx
}

open class CodeContext : Operation{
    val name: String
    val vars = HashMap<String, Any?>()
    val pairedCalls = ArrayList<Pair<Int, Byte>>()
    val operands = Stack<Any?>()
    val operators = ArrayList<Operations>()
    val hasReturn: Boolean
    val paramCount: Byte
    val constants: Array<String>
    var stackPointer = -1
    var popFlag = false
    var isRunning = false
    protected val ctxPool: ContextPool

    @Throws(ValidationException::class)
    internal constructor(ctxName: String, constants: Array<String>, code: ByteArray, ctxPool: ContextPool){
        name = if(ctxName.contains("%") || ctxName.contains(";")) throw ValidationException("Invalid context name: "+ctxName) else ctxName
        constants.filter { it.contains("%") || it.contains(";") }.forEach { throw ValidationException("Invalid constant name: "+it) }
        hasReturn = !(code.isEmpty() || ((code[0] and -128).toInt() ushr 7) == 0)
        paramCount = if(code.isEmpty()) 0 else code[0] and 0b01111111
        this.constants = constants
        this.ctxPool = ctxPool
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

    private constructor(ctxName: String, const: Array<String>, op: ArrayList<Operations>, pC: ArrayList<Pair<Int, Byte>>, ctxPool: ContextPool) : this(ctxName, const, kotlin.ByteArray(0), ctxPool) {
        operators.addAll(op)
        for((i, b) in pC) pairedCalls.add(Pair(i, b))
    }

    fun toBytes(): ByteArray {
        val emptyStack = Stack<CodeContext>()
        val build = ArrayList<Byte>()
        var constants = ""

        ContextPool.getSignature().forEach { build.add(it) }

        for(s in this.constants) constants+=s.replace("%", "\\%")+"%"
        constants+=name+";"
        constants.toByteArray().forEach { build.add(it) }
        build.add((paramCount and 0b01111111) or if(hasReturn) 0b10000000.toByte() else 0)
        for(i in operators.indices){
            build.add(operators[i].ordinal.toByte())
            val max = operators[i].getPairCount()
            var v = -1
            while(++v<max) build.add(loadPairedVal(emptyStack, v, i))
        }
        return ByteArray(build.size, { build[it] })
    }

    override fun hasReturnValue(): Boolean = hasReturn
    override fun getParamCount(): Int = paramCount.toInt()

    override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
        if(isRunning) return loadContext(stack, name).eval(stack, *params) // Prevent errors with multi-threading or self-referencing code
        isRunning = true
        stack.push(this)
        if(params.size > paramCount) throw exception(stack, "Context given too many parameters!")
        params.forEachIndexed { index, it -> vars[index.toString()] = it }
        while (stackPointer<operators.size-1){
            val o = operators[++stackPointer]
            val a = o.eval(stack, *(loadOperands(stack, o)))
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

    private fun loadOperands(stack: Stack<CodeContext>, op: Operations): Array<Any?> {
        if(op.getParamCount()>operands.size) throw exception(stack, "Operand stack underflow!")
        return Array(op.getParamCount() + op.getPairCount(), {
            if(it<op.getParamCount()) operands.pop() else loadPairedVal(stack, it-op.getParamCount(), stackPointer)
        })
    }

    private fun loadPairedVal(stack: Stack<CodeContext>, which: Int, atStack: Int): Byte{
        var count = 0
        for((first, second) in pairedCalls)
            if(first == atStack){
                if(count==which) return second
                else ++count
            }
        throw exception(stack, "Can't load paired call value $which from $atStack!")
    }

    fun loadContext(stack: Stack<CodeContext>, ctxName: String): CodeContext {
        if(ctxName == name) return CodeContext(name, constants, operators, pairedCalls, ctxPool)
        try {
            return ctxPool.lookup(ctxName)
        }catch(e: Exception){ throw exception(stack, e.message) }
    }
    override fun toString(): String = "Context{name=$name, pointer=$stackPointer ("+operators[stackPointer].name+")}"
}

interface Operation{
    fun hasReturnValue(): Boolean
    fun getParamCount(): Int
    fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any?
}

infix fun <K, V> HashMap<K, V>.containsKeyI(key: K): Boolean = containsKey(key)
fun Any?.asBoolean(): Boolean =
        if(this==null) false else this as? Boolean ?: if((this.toString() == "true") or (this.toString() == "false")) this.toString().toBoolean() else !((this.toString()=="0") or (this.toString()=="0.0"))
fun Any?.asDouble(): Double = if(this==null) 0.0 else (this as? Number ?: try{ this.toString().toDouble() }catch(e: NumberFormatException){ 0.0 }).toDouble()

fun exception(stack: Stack<CodeContext>, reason: String?): RuntimeException =
        RuntimeException((reason ?: "")+"  Trace: "+Arrays.toString(stack.toArray())+
                (if(stack.size>0) " at "+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].ordinal+" ("+stack[stack.size-1].operators[stack[stack.size-1].stackPointer].name+")" else ""))
@Suppress("UNCHECKED_CAST")
infix fun <T: Number, K: Number> K.toNumber(type: Class<in T>): T =
        (if(type==Byte::class.java || type==Byte::class) this.toByte()
        else if(type==Short::class.java || type==Short::class) this.toShort()
        else if(type==Integer::class.java || type==Integer::class) this.toInt()
        else if(type==Long::class.java || type==Long::class) this.toLong()
        else if(type==Float::class.java || type==Float::class) this.toFloat()
        else if(type==Double::class.java || type==Double::class) this.toDouble()
        else this) as T

enum class Operations: Operation{
    LDV{ // Load variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
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
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): String? {
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
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): String? = stack[stack.size-1].constants[params[0].asDouble().toInt()]
        override fun getPairCount(): Int = 1
    },
    EXT{ // Call external
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
            val caller = stack[stack.size-1]
            val c = Class.forName(params[0].toString())
            if(params[2].toString().toInt() > caller.operands.size) throw exception(stack, "Operand stack underflow! Required parameter count: "+params[2].toString())
            var callParams = Array(params[2].toString().toInt(), { caller.operands.pop() })
            val callMethod = getMethod(callParams, params[1].toString(), c, -1) ?: throw exception(stack, "Cannot find Method named \""+params[1]+"\"")
            if(!Modifier.isStatic(callMethod.modifiers)){
                if(caller.operands.isEmpty()) throw exception(stack, "Operand stack underflow! Required parameter count: "+params[2].toString() + " (plus 1 object to call on) ")
                callParams = arrayOf(caller.operands.pop(), *callParams)
            }
            caller.popFlag = callMethod.returnType==Void.TYPE
            val v = invoke(callMethod, *callParams)
            return v
        }
        override fun getPairCount(): Int = 1
    },
    POP{ // Pop one operand
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): String? = null
        override fun getPairCount(): Int = 0
    },
    DCV{ // Declare variable
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
            if(stack[stack.size-1].vars containsKeyI params[0].toString()) return null
            stack[stack.size-1].vars.put(params[0].toString(), params[1])
            return null
        }
        override fun getPairCount(): Int = 0
    },
    CMP{ // Compare
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = (params[0].toString() == params[1].toString()) or (if(params[0]==null) params[1]==null else params[0]?.equals(params[1]) as Boolean)
        override fun getPairCount(): Int = 0
    },
    LNT{ // Logical NOT
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = !params[0].asBoolean()
        override fun getPairCount(): Int = 0
    },
    LOR{ // Logical OR
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = params[0].asBoolean() or params[1].asBoolean()
        override fun getPairCount(): Int = 0
    },
    CND{ // Conditional Jump (false = jump 2, true = no jump)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = if(!params[0].asBoolean()) stack[stack.size-1].stackPointer+=2 else 0
        override fun getPairCount(): Int = 0
    },
    JMP{ // Unconditional jump (1 operand)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any?{
            stack[stack.size-1].stackPointer = (if(params[0].asBoolean()) stack[stack.size-1].stackPointer else 0) + params[1].asDouble().toInt()
            return null
        }
        override fun getPairCount(): Int = 0
    },
    CALL{ // Call context
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
            val caller = stack[stack.size-1]
            val ctx = caller.loadContext(stack, params[0].toString())
            if(ctx.getParamCount() > caller.operands.size) throw exception(stack, "Operand stack underflow! Required parameter count: "+ctx.getParamCount())
            caller.popFlag = !ctx.hasReturn
            return ctx.eval(stack, *Array(ctx.getParamCount(), { caller.operands.pop() }))
        }
        override fun getPairCount(): Int = 0
    },
    LDN{ // Load constant number
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = params[0]
        override fun getPairCount(): Int = 1
    },
    CJP{ // Constant jump (unconditional, no operands)
        override fun hasReturnValue(): Boolean = false
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? {
            stack[stack.size-1].stackPointer += params[0].asDouble().toInt()
            return null
        }
        override fun getPairCount(): Int = 1
    },
    VLD{ // Constant load from variable (load constant based on value of variable)
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = stack[stack.size-1].constants[params[0].asDouble().toInt()]
        override fun getPairCount(): Int = 0
    },
    NOP{ // No operation
        override fun hasReturnValue(): Boolean  = false
        override fun getParamCount(): Int = 0
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = null
        override fun getPairCount(): Int = 0
    },
    INC{ // Increment variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = params[0].asDouble() + 1
        override fun getPairCount(): Int = 0
    },
    DEC { // Decrement variable
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 1
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = params[0].asDouble() - 1
        override fun getPairCount(): Int = 0
    },
    LOP{ // Logical operation
        override fun hasReturnValue(): Boolean = true
        override fun getParamCount(): Int = 2
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? =
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
        override fun eval(stack: Stack<CodeContext>, vararg params: Any?): Any? = stack[stack.size-1].operands.push(params[0])
        override fun getPairCount(): Int = 0
    };
    abstract fun getPairCount(): Int
}

/**
 * Selects the most plausible method that caller is trying to get based on method name, parameters and owning class etc.
 */
fun getMethod(params: Array<Any?>, name: String, owner: Class<*>, searchMask: Int): Method? {
    val allMethods = ArrayList<Method>()
    var cur: Class<*>? = null
    while(true){
        cur = if(cur==null) owner else cur.superclass
        if(cur==null) break
        cur.declaredMethods.filterNot {
            (searchMask!=-1 && it.parameterTypes.size!=searchMask) ||
                    it.name != name ||
                    allMethods.contains(it) ||
                    (it.parameterTypes.size<params.size && Modifier.isStatic(it.modifiers)) ||
                    (it.parameterTypes.size<params.size-1 && !Modifier.isStatic(it.modifiers))
        }.forEach { allMethods.add(it) }
        cur = cur.superclass
    }

    var match: Method? = null
    var matchCount: Double = 0.0

    outer@
    for(it in allMethods) {
        val isStatic = Modifier.isStatic(it.modifiers)
        if(match==null) match = it
        else{
            var percent: Double = 0.0
            for(i in it.parameterTypes.indices) {
                val got = (getOrCreate(it.parameterTypes[i], if(i<params.size) params[i + if(isStatic) 0 else 1] else null, true, false) as Double)
                if (got==0.0) continue@outer
                percent += got/it.parameterTypes.size
            }
            if(percent>matchCount){
                match = it
                matchCount = percent
            }
        }
    }
    return match
}

fun invoke(call: Method, vararg params: Any?): Any? {
    try{
        call.isAccessible = true
        val isStatic = Modifier.isStatic(call.modifiers)
        val callParams = Array(if(call.parameterTypes.isNotEmpty()) call.parameterTypes.size - (if(isStatic) 0 else 1) else 0,
                {
                    if((it + if(isStatic) 0 else 1)>=params.size) null
                    else getOrCreate(call.parameterTypes[it], params[it + (if(isStatic) 0 else 1)], true, true)
                })
        return call.invoke(if(isStatic) null else params[0], *callParams)
    }catch(e: Exception){ e.printStackTrace(); return null }
}

fun getOrCreate(matchType: Class<*>, param: Any?, ignoreSafety: Boolean, create: Boolean): Any? {
    if(param==null){
        if(matchType.isPrimitive) return if(create && matchType==Boolean::class) false else if(create) 0.0.toNumber(getBoxed(matchType)) else 0.25
        return if(create) param else 1
    }

    // At this point, we know that "param" MUST be non-null
    if(matchType.isAssignableFrom(param.javaClass)) return if(create) param else 1.0
    if((matchType.isPrimitive || Number::class.java.isAssignableFrom(matchType)) && (param is String || param is Number)){
        try{
            java.lang.Double.parseDouble(param.toString())
            @Suppress("UNCHECKED_CAST")
            val v: Class<in Number> = getBoxed(matchType) as Class<in Number>
            return if(create) param.toString().toDouble().toNumber(v) else 1.0
        }catch(e: Exception){ e.printStackTrace() }  // Check if conversion is plausible
    }

    if(CharSequence::class.java.isAssignableFrom(matchType)) return if(create) param.toString() else 0.75

    matchType.declaredConstructors
            .filter { (ignoreSafety || it.isAccessible) && it.parameterTypes.size==1 && (it.parameterTypes[0].isAssignableFrom(param.javaClass) || it.parameterTypes[0].isPrimitive) }
            .forEach {
                try{
                    it.isAccessible = true
                    return if(create) it.newInstance(if(it.parameterTypes[0].isPrimitive){ if(it.parameterTypes[0]==Boolean::class.java) param.asBoolean() else param.asDouble().toNumber(it.parameterTypes[0])} else param) else 0.5
                }catch (e: Exception){}
            }
    matchType.declaredMethods
            .filter { (ignoreSafety || it.isAccessible) && ((it.modifiers and 8)!=0) && it.parameterTypes.size==1 && (it.parameterTypes[0].isAssignableFrom(param.javaClass) || it.parameterTypes[0].isPrimitive) }
            .forEach {
                try{
                    it.isAccessible = true
                    return if(create) it.invoke(null, if(it.parameterTypes[0].isPrimitive){ if(it.parameterTypes[0]==Boolean::class.java) param.asBoolean() else param.asDouble().toNumber(it.parameterTypes[0])} else param) else 0.5
                }catch (e: Exception){}
            }

    return if(create) null else 0.0
}

fun getBoxed(c: Class<*>): Class<*> {
    return if(c==Int::class.javaPrimitiveType) java.lang.Integer::class.java
        else if(c==Short::class.javaPrimitiveType) java.lang.Short::class.java
        else if(c==Float::class.javaPrimitiveType) java.lang.Float::class.java
        else if(c==Float::class.javaPrimitiveType) java.lang.Float::class.java
        else if(c==Double::class.javaPrimitiveType) java.lang.Double::class.java
        else if(c==Byte::class.javaPrimitiveType) java.lang.Byte::class.java
        else if(c==Char::class.javaPrimitiveType) java.lang.Character::class.java
        else c
}