package io.github.supermonster003.autojs6.plugin.three.slate.studio.project

/**
 * Undo and redo of the timeline editor (roadmap P4.3): snapshots of the immutable state, at most [capacity] steps back
 * (the roadmap asks for at least 10). A gesture (a drag of a clip or a handle) changes [current] many times but makes
 * one step: [begin] remembers the state before it, [update] replaces the current state without a step, [commit] adds
 * the step when the state changed.
 * zh-CN: 时间线编辑器的撤销与重做 (路线图 P4.3): 不可变状态的快照, 至多后退 [capacity] 步 (路线图要求至少 10 步). 一次手势 (拖动
 * 片段或把手) 多次改变 [current] 但只算一步: [begin] 记住手势前的状态, [update] 替换当前状态而不记步, [commit] 在状态有变化时记一步.
 */
internal class EditHistory<T>(initial: T, private val capacity: Int = DEFAULT_CAPACITY) {

    private val undo = ArrayDeque<T>()
    private val redo = ArrayDeque<T>()
    private var gestureStart: T? = null

    var current: T = initial
        private set

    val canUndo: Boolean get() = undo.isNotEmpty() || (gestureStart != null && gestureStart != current)
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoSteps: Int get() = undo.size

    /** One edit as one step. zh-CN: 一次编辑, 记为一步. */
    fun apply(next: T) {
        commit()
        if (next == current) return
        push(current)
        redo.clear()
        current = next
    }

    fun begin() {
        if (gestureStart == null) gestureStart = current
    }

    fun update(next: T) {
        if (gestureStart == null) gestureStart = current
        current = next
    }

    fun commit() {
        val start = gestureStart ?: return
        gestureStart = null
        if (start == current) return
        push(start)
        redo.clear()
    }

    /** Drops the gesture and returns to the state before it. zh-CN: 放弃手势, 回到手势之前的状态. */
    fun cancel() {
        gestureStart?.let { current = it }
        gestureStart = null
    }

    fun undo(): Boolean {
        commit()
        val previous = undo.removeLastOrNull() ?: return false
        redo.addLast(current)
        current = previous
        return true
    }

    fun redo(): Boolean {
        commit()
        val next = redo.removeLastOrNull() ?: return false
        undo.addLast(current)
        current = next
        return true
    }

    /** Starts over from [state] (a project was opened). zh-CN: 从 [state] 重新开始 (打开了工程). */
    fun reset(state: T) {
        undo.clear()
        redo.clear()
        gestureStart = null
        current = state
    }

    private fun push(state: T) {
        undo.addLast(state)
        while (undo.size > capacity) undo.removeFirst()
    }

    companion object {
        const val DEFAULT_CAPACITY = 50
    }

}
