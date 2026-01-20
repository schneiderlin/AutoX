package com.autocljs.runtime.api

import com.autocljs.layout.LayoutInspector
import com.autocljs.layout.NodeInfo
import com.autocljs.runtime.ScriptRuntime
import com.caoccao.javet.annotations.V8Function
import com.caoccao.javet.interop.V8Runtime
import com.caoccao.javet.values.V8Value
import com.caoccao.javet.values.reference.V8ValueObject

/**
 * JavaScript Layout wrapper for Node.js/V8 engine.
 *
 * Exposes layout.findOne(), layout.findAll(), layout.findByText(), etc. to JavaScript.
 * Delegates to ScriptRuntime's LayoutInspector.
 */
class NodeLayout(val runtime: ScriptRuntime) {
    private var v8ValueObject: V8ValueObject? = null

    /**
     * Install layout API in the global scope.
     */
    fun install(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8Runtime.getExecutor(SCRIPT).execute<V8ValueObject>().let {
            v8ValueObject = it
            it.bind(this)
            global.set("layout", it)
        }
    }

    /**
     * Clean up layout API.
     */
    fun recycle(v8Runtime: V8Runtime, global: V8ValueObject) {
        v8ValueObject?.use { it.unbind(this) }
        v8ValueObject = null
    }

    /**
     * Get the layout inspector instance, throwing an error if not initialized.
     */
    private fun getInspector(): LayoutInspector {
        return runtime.getLayoutInspector()
            ?: throw IllegalStateException("LayoutInspector not initialized. Call initLayoutInspector() first.")
    }

    /**
     * Convert V8 value to Int.
     */
    private fun toInt(value: V8Value?): Int {
        if (value == null) {
            throw IllegalArgumentException("Expected number, got null")
        }

        val obj = value.v8Runtime.converter.toObject<Any?>(value)
        return when (obj) {
            is Number -> obj.toInt()
            is String -> obj.toIntOrNull() ?: throw IllegalArgumentException("Cannot convert '$obj' to integer")
            else -> throw IllegalArgumentException("Expected number, got ${obj?.javaClass?.simpleName ?: "null"}")
        }
    }

    /**
     * Convert V8 value to query object.
     */
    private fun toQueryObject(value: V8Value?): LayoutQuery {
        if (value == null) {
            throw IllegalArgumentException("Expected query object, got null")
        }

        // Use converter to get the Java object (may be a Map for JavaScript objects)
        val obj = value.v8Runtime.converter.toObject<Any?>(value)

        return when (obj) {
            is LayoutQuery -> obj
            is Map<*, *> -> {
                val query = LayoutQuery()
                (obj["text"] as? String)?.let { query.text = it }
                (obj["desc"] as? String)?.let { query.desc = it }
                (obj["className"] as? String)?.let { query.className = it }
                (obj["id"] as? String)?.let { query.id = it }
                (obj["exact"] as? Boolean)?.let { query.exact = it }
                query
            }
            else -> throw IllegalArgumentException("Expected query object, got ${obj?.javaClass?.simpleName ?: "null"}")
        }
    }

    /**
     * Called by V8 when JavaScript code calls layout.findOne(query)
     */
    @V8Function
    fun findOne(query: V8Value?): NodeInfo? {
        val layoutQuery = toQueryObject(query)
        val inspector = getInspector()

        // Capture new layout and search
        val root = inspector.captureCurrentWindow() ?: return null

        // Build predicate from query
        val predicate = buildPredicate(layoutQuery)

        return root.findFirstDescendant(predicate)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findAll(query)
     */
    @V8Function
    fun findAll(query: V8Value?): List<NodeInfo> {
        val layoutQuery = toQueryObject(query)
        val inspector = getInspector()

        // Capture new layout and search
        val root = inspector.captureCurrentWindow() ?: return emptyList()

        // Build predicate from query
        val predicate = buildPredicate(layoutQuery)

        return root.findDescendants(predicate)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findByText(text, exactMatch)
     */
    @V8Function
    fun findByText(text: V8Value?, exactMatch: V8Value? = null): List<NodeInfo> {
        if (text == null) {
            throw IllegalArgumentException("layout.findByText() text argument cannot be null")
        }

        val textStr = text.v8Runtime.converter.toObject<Any?>(text)?.toString()
            ?: throw IllegalArgumentException("layout.findByText() text argument cannot be null")

        val exact = when (val obj = exactMatch?.v8Runtime?.converter?.toObject<Any?>(exactMatch)) {
            is Boolean -> obj
            is Number -> obj.toInt() != 0
            null -> false
            else -> false
        }

        return getInspector().findByText(textStr, exact)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findByDescription(desc, exactMatch)
     */
    @V8Function
    fun findByDescription(desc: V8Value?, exactMatch: V8Value? = null): List<NodeInfo> {
        if (desc == null) {
            throw IllegalArgumentException("layout.findByDescription() desc argument cannot be null")
        }

        val descStr = desc.v8Runtime.converter.toObject<Any?>(desc)?.toString()
            ?: throw IllegalArgumentException("layout.findByDescription() desc argument cannot be null")

        val exact = when (val obj = exactMatch?.v8Runtime?.converter?.toObject<Any?>(exactMatch)) {
            is Boolean -> obj
            is Number -> obj.toInt() != 0
            null -> false
            else -> false
        }

        return getInspector().findByDescription(descStr, exact)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findByClassName(className)
     */
    @V8Function
    fun findByClassName(className: V8Value?): List<NodeInfo> {
        if (className == null) {
            throw IllegalArgumentException("layout.findByClassName() className argument cannot be null")
        }

        val classNameStr = className.v8Runtime.converter.toObject<Any?>(className)?.toString()
            ?: throw IllegalArgumentException("layout.findByClassName() className argument cannot be null")

        return getInspector().findByClassName(classNameStr)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findById(id)
     */
    @V8Function
    fun findById(id: V8Value?): List<NodeInfo> {
        if (id == null) {
            throw IllegalArgumentException("layout.findById() id argument cannot be null")
        }

        val idStr = id.v8Runtime.converter.toObject<Any?>(id)?.toString()
            ?: throw IllegalArgumentException("layout.findById() id argument cannot be null")

        return getInspector().findById(idStr)
    }

    /**
     * Called by V8 when JavaScript code calls layout.findAt(x, y)
     */
    @V8Function
    fun findAt(x: V8Value?, y: V8Value?): NodeInfo? {
        val xInt = toInt(x)
        val yInt = toInt(y)
        return getInspector().findNodeAt(xInt, yInt)
    }

    /**
     * Called by V8 when JavaScript code calls layout.capture()
     * Forces a new layout capture and returns the root node.
     */
    @V8Function
    fun capture(): NodeInfo? {
        return getInspector().captureCurrentWindow()
    }

    /**
     * Called by V8 when JavaScript code calls layout.getRoot()
     * Returns the cached layout root node.
     */
    @V8Function
    fun getRoot(): NodeInfo? {
        return getInspector().getCachedLayout()
    }

    /**
     * Build a predicate function from a LayoutQuery.
     */
    private fun buildPredicate(query: LayoutQuery): (NodeInfo) -> Boolean {
        return { node ->
            var matches = true

            query.text?.let { text ->
                matches = matches && if (query.exact) {
                    node.text?.toString() == text
                } else {
                    node.text?.toString()?.contains(text, ignoreCase = true) == true
                }
            }

            query.desc?.let { desc ->
                matches = matches && if (query.exact) {
                    node.desc?.toString() == desc
                } else {
                    node.desc?.toString()?.contains(desc, ignoreCase = true) == true
                }
            }

            query.className?.let { className ->
                matches = matches && node.className.contains(className, ignoreCase = true)
            }

            query.id?.let { id ->
                matches = matches && node.id.endsWith(id)
            }

            matches
        }
    }

    companion object {
        /**
         * JavaScript code to create layout object.
         */
        private val SCRIPT = """
            (function () {
                const nativeLayout = {};
                return nativeLayout;
            })()
        """.trimIndent()
    }
}
