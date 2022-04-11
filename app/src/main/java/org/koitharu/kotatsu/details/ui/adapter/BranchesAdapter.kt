package org.koitharu.kotatsu.details.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.parsers.util.replaceWith

class BranchesAdapter : BaseAdapter() {

	private val dataSet = ArrayList<String?>()

	override fun getCount(): Int {
		return dataSet.size
	}

	override fun getItem(position: Int): Any? {
		return dataSet[position]
	}

	override fun getItemId(position: Int): Long {
		return dataSet[position].hashCode().toLong()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(parent.context)
			.inflate(R.layout.item_branch, parent, false)
		(view as TextView).text = dataSet[position]
		return view
	}

	override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
		val view = convertView ?: LayoutInflater.from(parent.context)
			.inflate(R.layout.item_branch_dropdown, parent, false)
		(view as TextView).text = dataSet[position]
		return view
	}

	fun setItems(items: Collection<String?>) {
		dataSet.replaceWith(items)
		notifyDataSetChanged()
	}
}