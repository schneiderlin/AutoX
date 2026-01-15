package com.autocljs.engine

import com.autocljs.script.ScriptSource
import java.util.concurrent.atomic.AtomicInteger

/**
 * Script engine interface for executing ClojureScript code.
 * 
 * A ScriptEngine is created and then can be used to execute scripts.
 * When execution finishes, the engine should be destroyed.
 * 
 * If you want to stop the engine from other threads, call [ScriptEngine.forceStop].
 */
interface ScriptEngine<S : ScriptSource?> {
    var id: Int
    val isDestroyed: Boolean
    val uncaughtException: Throwable?
    
    fun put(name: String, value: Any?)
    fun execute(scriptSource: S): Any?
    fun forceStop()
    fun destroy()
    fun uncaughtException(throwable: Throwable?)
    fun setOnDestroyListener(listener: OnDestroyListener)
    fun init()

    interface OnDestroyListener {
        fun onDestroy(engine: ScriptEngine<*>)
    }

    interface EngineEvent {
        fun emit(name: String, vararg args: Any?)
    }

    abstract class AbstractScriptEngine<S : ScriptSource?> : ScriptEngine<S> {
        private var mOnDestroyListener: OnDestroyListener? = null

        @Volatile
        final override var isDestroyed = false
            private set
        private var mUncaughtException: Throwable? = null

        private val mId = AtomicInteger(NO_ID)
        override var id: Int
            get() = mId.get()
            set(value) {
                mId.compareAndSet(NO_ID, value)
            }

        override fun destroy() {
            mOnDestroyListener?.onDestroy(this)
            isDestroyed = true
        }

        override fun setOnDestroyListener(listener: OnDestroyListener) {
            if (mOnDestroyListener != null) {
                throw SecurityException("setOnDestroyListener can be called only once")
            }
            mOnDestroyListener = listener
        }

        override fun uncaughtException(throwable: Throwable?) {
            mUncaughtException = throwable
            forceStop()
        }

        override val uncaughtException: Throwable?
            get() = mUncaughtException

        companion object {
            const val NO_ID = -1
        }
    }
}

