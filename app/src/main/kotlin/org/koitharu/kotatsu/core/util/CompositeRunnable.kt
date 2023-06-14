package org.koitharu.kotatsu.core.util

class CompositeRunnable(
	private val children: List<Runnable>,
) : Runnable, Collection<Runnable> by children {

	override fun run() {
		for (child in children) {
			child.run()
		}
	}
}
