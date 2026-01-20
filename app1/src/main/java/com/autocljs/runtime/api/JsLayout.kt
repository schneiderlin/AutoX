package com.autocljs.runtime.api

import com.autocljs.layout.LayoutInspector
import com.autocljs.layout.NodeInfo
import com.autocljs.runtime.ScriptRuntime
import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject

/**
 * JavaScript Layout wrapper for Rhino.
 *
 * Exposes layout.findOne(), layout.findAll(), layout.findByText(), etc. to JavaScript.
 * Delegates to ScriptRuntime's LayoutInspector.
 */
class JsLayout(val runtime: ScriptRuntime) : ScriptableObject() {

    init {
        // Define function properties that will be callable from JavaScript
        defineFunctionProperties(
            arrayOf("findOne", "findAll", "findByText", "findByDescription", "findByClassName", "findById", "findAt", "capture", "getRoot"),
            JsLayout::class.java,
            ScriptableObject.READONLY
        )
    }

    /**
     * Get the layout inspector instance, throwing an error if not initialized.
     */
    private fun getInspector(): LayoutInspector {
        return runtime.getLayoutInspector()
            ?: throw IllegalStateException("LayoutInspector not initialized. Call initLayoutInspector() first.")
    }

    /**
     * Convert JavaScript value to query object.
     */
    private fun toQueryObject(value: Any?): LayoutQuery? {
        if (value == null) return null
        if (value is LayoutQuery) return value

        // Handle NativeObject from JavaScript
        if (value is Scriptable) {
            val query = LayoutQuery()

            (value.get("text", value) as? String)?.let { query.text = it }
            (value.get("desc", value) as? String)?.let { query.desc = it }
            (value.get("className", value) as? String)?.let { query.className = it }
            (value.get("id", value) as? String)?.let { query.id = it }

            val exactVal = value.get("exact", value)
            if (exactVal is Boolean) {
                query.exact = exactVal
            }

            return query
        }

        throw IllegalArgumentException("Expected query object, got ${value::class.simpleName}")
    }

    override fun getClassName(): String {
        return "Layout"
    }

    companion object {
        /**
         * Convert JavaScript number to Int.
         */
        private fun toInt(value: Any?): Int {
            return when (value) {
                is Number -> value.toInt()
                is String -> value.toIntOrNull() ?: throw IllegalArgumentException("Cannot convert '$value' to integer")
                null -> throw IllegalArgumentException("Expected number, got null")
                else -> throw IllegalArgumentException("Expected number, got ${value::class.simpleName}")
            }
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findOne(query)
         */
        @JvmStatic
        fun findOne(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findOne")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findOne() requires at least 1 argument: query")
            }

            val query = jsLayout.toQueryObject(args[0])
                ?: throw IllegalArgumentException("layout.findOne() requires a valid query object")

            val inspector = jsLayout.getInspector()

            // Capture new layout and search
            val root = inspector.captureCurrentWindow() ?: return null

            // Build predicate from query
            val predicate = buildPredicate(query)

            return root.findFirstDescendant(predicate)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findAll(query)
         */
        @JvmStatic
        fun findAll(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findAll")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findAll() requires at least 1 argument: query")
            }

            val query = jsLayout.toQueryObject(args[0])
                ?: throw IllegalArgumentException("layout.findAll() requires a valid query object")

            val inspector = jsLayout.getInspector()

            // Capture new layout and search
            val root = inspector.captureCurrentWindow() ?: return emptyList<NodeInfo>()

            // Build predicate from query
            val predicate = buildPredicate(query)

            return root.findDescendants(predicate)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findByText(text, exactMatch)
         */
        @JvmStatic
        fun findByText(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findByText")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findByText() requires at least 1 argument: text")
            }

            val text = args[0]?.toString()
                ?: throw IllegalArgumentException("layout.findByText() text argument cannot be null")

            val exactMatch = if (args.size >= 2) {
                when (val arg = args[1]) {
                    is Boolean -> arg
                    is Number -> arg.toInt() != 0
                    else -> false
                }
            } else false

            return jsLayout.getInspector().findByText(text, exactMatch)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findByDescription(desc, exactMatch)
         */
        @JvmStatic
        fun findByDescription(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findByDescription")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findByDescription() requires at least 1 argument: desc")
            }

            val desc = args[0]?.toString()
                ?: throw IllegalArgumentException("layout.findByDescription() desc argument cannot be null")

            val exactMatch = if (args.size >= 2) {
                when (val arg = args[1]) {
                    is Boolean -> arg
                    is Number -> arg.toInt() != 0
                    else -> false
                }
            } else false

            return jsLayout.getInspector().findByDescription(desc, exactMatch)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findByClassName(className)
         */
        @JvmStatic
        fun findByClassName(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findByClassName")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findByClassName() requires 1 argument: className")
            }

            val className = args[0]?.toString()
                ?: throw IllegalArgumentException("layout.findByClassName() className argument cannot be null")

            return jsLayout.getInspector().findByClassName(className)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findById(id)
         */
        @JvmStatic
        fun findById(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findById")

            if (args.isEmpty()) {
                throw IllegalArgumentException("layout.findById() requires 1 argument: id")
            }

            val id = args[0]?.toString()
                ?: throw IllegalArgumentException("layout.findById() id argument cannot be null")

            return jsLayout.getInspector().findById(id)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.findAt(x, y)
         */
        @JvmStatic
        fun findAt(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.findAt")

            if (args.size < 2) {
                throw IllegalArgumentException("layout.findAt() requires 2 arguments: x, y")
            }

            val x = toInt(args[0])
            val y = toInt(args[1])

            return jsLayout.getInspector().findNodeAt(x, y)
        }

        /**
         * Called by Rhino when JavaScript code calls layout.capture()
         * Forces a new layout capture and returns the root node.
         */
        @JvmStatic
        fun capture(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.capture")

            return jsLayout.getInspector().captureCurrentWindow()
        }

        /**
         * Called by Rhino when JavaScript code calls layout.getRoot()
         * Returns the cached layout root node.
         */
        @JvmStatic
        fun getRoot(context: Context, thisObj: Scriptable, args: Array<Any?>, funObj: Function): Any? {
            val jsLayout = thisObj as? JsLayout
                ?: throw IllegalStateException("Invalid thisObj for layout.getRoot")

            return jsLayout.getInspector().getCachedLayout()
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
    }
}

/**
 * Query object for layout searches.
 * Can be populated from JavaScript objects.
 */
data class LayoutQuery(
    var text: String? = null,
    var desc: String? = null,
    var className: String? = null,
    var id: String? = null,
    var exact: Boolean = false
)
