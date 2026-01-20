package com.autocljs.layout

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Represents a node in the accessibility layout tree.
 * Contains essential properties for UI inspection and automation.
 */
data class NodeInfo(
    // Identity
    val id: String = "",
    val idHex: String = "",
    val className: String = "",
    val packageName: String = "",

    // Content
    val text: CharSequence? = null,
    val desc: CharSequence? = null,

    // Bounds
    val boundsInScreen: Rect = Rect(),
    val boundsInParent: Rect = Rect(),

    // State
    val enabled: Boolean = false,
    val checked: Boolean = false,
    val selected: Boolean = false,
    val focused: Boolean = false,
    val accessibilityFocused: Boolean = false,

    // Interactivity
    val clickable: Boolean = false,
    val longClickable: Boolean = false,
    val checkable: Boolean = false,
    val focusable: Boolean = false,
    val editable: Boolean = false,
    val scrollable: Boolean = false,

    // Position
    val depth: Int = 0,
    val childCount: Int = 0,

    // Other
    val visibleToUser: Boolean = false,
    val dismissable: Boolean = false,

    // Tree structure
    val parent: NodeInfo? = null,
    val children: MutableList<NodeInfo> = mutableListOf()
) {
    /**
     * Get bounds as a readable string
     */
    fun boundsAsString(): String {
        return boundsInScreen.run { "[$left, $top][$right, $bottom]" }
    }

    /**
     * Get the full path from root to this node
     */
    fun getPath(): String {
        val path = mutableListOf<String>()
        var current: NodeInfo? = this
        while (current != null) {
            path.add(0, current.className)
            current = current.parent
        }
        return path.joinToString(" > ")
    }

    /**
     * Find all descendants matching the predicate
     */
    fun findDescendants(predicate: (NodeInfo) -> Boolean): List<NodeInfo> {
        val results = mutableListOf<NodeInfo>()
        fun traverse(node: NodeInfo) {
            if (predicate(node)) {
                results.add(node)
            }
            node.children.forEach { traverse(it) }
        }
        traverse(this)
        return results
    }

    /**
     * Find first descendant matching the predicate
     */
    fun findFirstDescendant(predicate: (NodeInfo) -> Boolean): NodeInfo? {
        if (predicate(this)) return this
        children.forEach { child ->
            val found = child.findFirstDescendant(predicate)
            if (found != null) return found
        }
        return null
    }

    /**
     * Get the center point of the node's bounds in screen coordinates.
     */
    fun center(): Pair<Int, Int> {
        return Pair(
            boundsInScreen.left + boundsInScreen.width() / 2,
            boundsInScreen.top + boundsInScreen.height() / 2
        )
    }

    /**
     * Check if the node has valid bounds (non-empty and non-zero area).
     */
    fun hasValidBounds(): Boolean {
        return !boundsInScreen.isEmpty &&
            boundsInScreen.width() > 0 &&
            boundsInScreen.height() > 0
    }

    /**
     * Check if this node or any of its ancestors is clickable.
     */
    fun isClickable(): Boolean {
        if (clickable) return true
        var current = parent
        while (current != null) {
            if (current.clickable) return true
            current = current.parent
        }
        return false
    }

    companion object {
        /**
         * Create a NodeInfo from AccessibilityNodeInfo
         */
        fun fromAccessibilityNode(
            node: AccessibilityNodeInfo,
            parent: NodeInfo? = null,
            depth: Int = 0
        ): NodeInfo {
            val boundsInScreen = Rect()
            node.getBoundsInScreen(boundsInScreen)

            val boundsInParent = Rect()
            node.getBoundsInParent(boundsInParent)

            val nodeInfo = NodeInfo(
                id = node.viewIdResourceName ?: "",
                idHex = "0x${Integer.toHexString(node.hashCode())}",
                className = node.className?.toString() ?: "",
                packageName = node.packageName?.toString() ?: "",

                text = node.text,
                desc = node.contentDescription,

                boundsInScreen = boundsInScreen,
                boundsInParent = boundsInParent,

                enabled = node.isEnabled,
                checked = node.isChecked,
                selected = node.isSelected,
                focused = node.isFocused,
                accessibilityFocused = node.isAccessibilityFocused,

                clickable = node.isClickable,
                longClickable = node.isLongClickable,
                checkable = node.isCheckable,
                focusable = node.isFocusable,
                editable = node.isEditable,
                scrollable = node.isScrollable,

                depth = depth,
                childCount = node.childCount,

                visibleToUser = node.isVisibleToUser,
                dismissable = node.isDismissable,

                parent = parent
            )

            // Recursively add children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val childNodeInfo = fromAccessibilityNode(child, nodeInfo, depth + 1)
                nodeInfo.children.add(childNodeInfo)
                child.recycle()
            }

            return nodeInfo
        }
    }
}
