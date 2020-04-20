package com.rnett.kframe.binding

sealed class ChangeDetector<S>(val savesValue: Boolean = true) {
    abstract fun isChanged(old: Any?, new: Any?, saved: S): Pair<Boolean, S>
    abstract fun getSave(initial: Any?): S

    @Deprecated("If you want an observer that is always updated, use Watch")
    object Always : ChangeDetector<Unit>(false) {
        override fun isChanged(old: Any?, new: Any?, saved: Unit) = (true) to Unit
        override fun getSave(initial: Any?) = Unit
    }

    object Equals : ChangeDetector<Unit>(false) {
        override fun isChanged(old: Any?, new: Any?, saved: Unit) = (old != new) to Unit
        override fun getSave(initial: Any?) = Unit
    }

    object RefEquals : ChangeDetector<Unit>(false) {
        override fun isChanged(old: Any?, new: Any?, saved: Unit) = (old !== new) to Unit
        override fun getSave(initial: Any?) = Unit
    }

    object HashCode : ChangeDetector<Int>() {
        override fun isChanged(old: Any?, new: Any?, saved: Int): Pair<Boolean, Int> {
            val newHash = new.hashCode()
            return (saved != newHash) to newHash
        }

        override fun getSave(initial: Any?) = initial.hashCode()
    }

    object ToString : ChangeDetector<String>() {
        override fun isChanged(old: Any?, new: Any?, saved: String): Pair<Boolean, String> {
            val newString = new.toString()
            return (saved != newString) to newString
        }

        override fun getSave(initial: Any?) = initial.toString()
    }
}

class ChangeHolder<S>(val detector: ChangeDetector<S>, initial: Any?) {
    private var lastSave: S = detector.getSave(initial)
    val savesValue = detector.savesValue

    fun isChanged(old: Any?, new: Any?): Boolean {
        val res = detector.isChanged(old, new, lastSave)
        lastSave = res.second
        return res.first
    }
}

typealias Filter<T> = (old: T, new: T) -> Boolean

class ChangeWatcher<T>(initial: T, detectors: List<ChangeDetector<*>>, val filter: Filter<T>) {
    private val detectors = detectors.map { ChangeHolder(it, initial) }

    private var oldValue: T = initial
    val value get() = oldValue

    fun update(newValue: T): Boolean {
        var changeFound = false
        detectors.forEach {
            if (!changeFound || it.savesValue)
                if (it.isChanged(oldValue, newValue))
                    changeFound = true
        }
        val passFilter = filter(oldValue, newValue)
        oldValue = newValue

        return changeFound && passFilter
    }
}