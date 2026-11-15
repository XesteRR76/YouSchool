package org.youschool.app.ui.navigation

object BottomBarConfig {
    val defaults = listOf("schedule", "grades", "homework", "profile")
    val available = defaults + "messages"

    fun tabIndex(x: Float, width: Float, padding: Float, count: Int): Int {
        if (count <= 0 || width <= padding * 2) return 0
        val ratio = ((x - padding) / (width - padding * 2)).coerceIn(0f, 0.999999f)
        return (ratio * count).toInt().coerceIn(0, count - 1)
    }

    fun normalize(routes: List<String>): List<String> {
        val valid = routes.filter { it in available }.distinct().toMutableList()
        if ("profile" !in valid) valid.add("profile")
        return if (valid.size < 2) defaults else valid
    }
}
