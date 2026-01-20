package com.autocljs.layout

import android.content.Context
import android.util.Log
import com.autocljs.accessibility.AccessibilityBridge
import com.autocljs.accessibility.AccessibilityService

/**
 * Layout inspector for capturing the current UI hierarchy.
 * Provides access to the accessibility node tree for inspection and automation.
 */
class LayoutInspector(
    private val context: Context,
    private val bridge: AccessibilityBridge
) {
    companion object {
        private const val TAG = "LayoutInspector"
    }

    private var cachedRoot: NodeInfo? = null

    /**
     * Capture the current window layout as a NodeInfo tree.
     * Returns null if accessibility service is not available or no root node is found.
     */
    fun captureCurrentWindow(): NodeInfo? {
        val root = bridge.getRootInActiveWindow()
        if (root == null) {
            Log.w(TAG, "No root node found in active window")
            return null
        }

        return try {
            val nodeInfo = NodeInfo.fromAccessibilityNode(root)
            cachedRoot = nodeInfo
            Log.d(TAG, "Captured layout with ${countNodes(nodeInfo)} nodes")
            nodeInfo
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing layout", e)
            null
        } finally {
            root.recycle()
        }
    }

    /**
     * Get the most recently captured layout (cached).
     */
    fun getCachedLayout(): NodeInfo? = cachedRoot

    /**
     * Find all nodes with the given text.
     */
    fun findByText(text: String, exactMatch: Boolean = false): List<NodeInfo> {
        val root = captureCurrentWindow() ?: return emptyList()
        return root.findDescendants { node ->
            if (exactMatch) {
                node.text?.toString() == text
            } else {
                node.text?.toString()?.contains(text, ignoreCase = true) == true
            }
        }
    }

    /**
     * Find all nodes with the given content description.
     */
    fun findByDescription(desc: String, exactMatch: Boolean = false): List<NodeInfo> {
        val root = captureCurrentWindow() ?: return emptyList()
        return root.findDescendants { node ->
            if (exactMatch) {
                node.desc?.toString() == desc
            } else {
                node.desc?.toString()?.contains(desc, ignoreCase = true) == true
            }
        }
    }

    /**
     * Find the first clickable node with the given text.
     */
    fun findClickableByText(text: String): NodeInfo? {
        val root = captureCurrentWindow() ?: return null
        return root.findFirstDescendant { node ->
            node.clickable && node.text?.toString()?.contains(text, ignoreCase = true) == true
        }
    }

    /**
     * Find all nodes by class name.
     */
    fun findByClassName(className: String): List<NodeInfo> {
        val root = captureCurrentWindow() ?: return emptyList()
        return root.findDescendants { node ->
            node.className.contains(className, ignoreCase = true)
        }
    }

    /**
     * Find all nodes by resource ID.
     */
    fun findById(id: String): List<NodeInfo> {
        val root = captureCurrentWindow() ?: return emptyList()
        return root.findDescendants { node ->
            node.id.endsWith(id)
        }
    }

    /**
     * Find node at the given screen coordinates.
     */
    fun findNodeAt(x: Int, y: Int): NodeInfo? {
        val root = captureCurrentWindow() ?: return null
        return root.findFirstDescendant { node ->
            val bounds = node.boundsInScreen
            x >= bounds.left && x < bounds.right && y >= bounds.top && y < bounds.bottom
        }
    }

    /**
     * Print the layout tree to log for debugging.
     */
    fun printLayout(node: NodeInfo? = cachedRoot, indent: Int = 0) {
        val currentNode = node ?: captureCurrentWindow() ?: return
        val prefix = "  ".repeat(indent)
        val textPreview = currentNode.text?.toString()?.take(20) ?: ""
        val descPreview = currentNode.desc?.toString()?.take(20) ?: ""

        Log.d(TAG, String.format(
            "%s[%s] %s text='%s' desc='%s' %s",
            prefix,
            currentNode.className.substringAfterLast('.'),
            currentNode.boundsAsString(),
            textPreview,
            descPreview,
            if (currentNode.clickable) "[CLICK]" else ""
        ))

        currentNode.children.forEach { printLayout(it, indent + 1) }
    }

    private fun countNodes(node: NodeInfo): Int {
        var count = 1
        node.children.forEach { count += countNodes(it) }
        return count
    }
}

/**
 * Entry function: Get the current layout as a NodeInfo tree.
 * No parameters required. Returns null if accessibility service is not available.
 *
 * Usage:
 *   val layout = getLayout()
 *   if (layout != null) {
 *       // Use the layout tree
 *   }
 *
 * @return The root NodeInfo of the current window, or null if unavailable.
 */
fun getLayout(): NodeInfo? {
    val service = AccessibilityService.instance
        ?: throw IllegalStateException("AccessibilityService is not enabled")

    // Create a minimal bridge wrapper
    val miniBridge = object : AccessibilityBridge(
        service.applicationContext,
        com.autocljs.accessibility.AccessibilityConfig(),
        com.autocljs.util.UiHandler(service.applicationContext)
    ) {
        override fun ensureServiceEnabled() {}
        override fun waitForServiceEnabled() {}
        override fun getService(): AccessibilityService? = service
        override fun getInfoProvider() = throw NotImplementedError()
        override fun getNotificationObserver() = throw NotImplementedError()
    }

    val inspector = LayoutInspector(service.applicationContext, miniBridge)
    return inspector.captureCurrentWindow()
}

/**
 * Convenience entry function: Get the current layout and convert to JSON string.
 */
fun getLayoutAsJson(): String? {
    val layout = getLayout() ?: return null
    return layoutToJson(layout)
}

/**
 * Convert NodeInfo tree to JSON string for easy serialization.
 */
fun layoutToJson(node: NodeInfo, indent: Int = 0): String {
    val sb = StringBuilder()
    val prefix = "  ".repeat(indent)

    sb.append(prefix).append("{\n")
    sb.append(prefix).append("  \"className\": \"${node.className}\",\n")
    sb.append(prefix).append("  \"id\": \"${node.id}\",\n")
    sb.append(prefix).append("  \"text\": \"${node.text}\",\n")
    sb.append(prefix).append("  \"desc\": \"${node.desc}\",\n")
    sb.append(prefix).append("  \"bounds\": \"${node.boundsAsString()}\",\n")
    sb.append(prefix).append("  \"clickable\": ${node.clickable},\n")
    sb.append(prefix).append("  \"depth\": ${node.depth},\n")

    if (node.children.isNotEmpty()) {
        sb.append(prefix).append("  \"children\": [\n")
        node.children.forEachIndexed { index, child ->
            sb.append(layoutToJson(child, indent + 2))
            if (index < node.children.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }
        sb.append(prefix).append("  ]\n")
    } else {
        sb.append(prefix).append("  \"children\": []\n")
    }

    sb.append(prefix).append("}")
    return sb.toString()
}
