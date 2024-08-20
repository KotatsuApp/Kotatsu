package org.koitharu.kotatsu.core.db

import androidx.sqlite.db.SimpleSQLiteQuery
import org.koitharu.kotatsu.list.domain.ListFilterOption
import java.util.LinkedList

class MangaQueryBuilder(
	private val table: String,
	private val conditionCallback: ConditionCallback
) {

	private var filterOptions: Collection<ListFilterOption> = emptyList()
	private var whereConditions = LinkedList<String>()
	private var orderBy: String? = null
	private var groupBy: String? = null
	private var extraJoins: String? = null
	private var limit: Int = 0

	fun filters(options: Collection<ListFilterOption>) = apply {
		filterOptions = options
	}

	fun where(condition: String) = apply {
		whereConditions.add(condition)
	}

	fun orderBy(orderBy: String?) = apply {
		this@MangaQueryBuilder.orderBy = orderBy
	}

	fun groupBy(groupBy: String?) = apply {
		this@MangaQueryBuilder.groupBy = groupBy
	}

	fun limit(limit: Int) = apply {
		this@MangaQueryBuilder.limit = limit
	}

	fun join(join: String?) = apply {
		extraJoins = join
	}

	fun build() = buildString {
		append("SELECT * FROM ")
		append(table)
		extraJoins?.let {
			append(' ')
			append(it)
		}
		if (whereConditions.isNotEmpty()) {
			whereConditions.joinTo(
				buffer = this,
				prefix = " WHERE ",
				separator = " AND ",
			)
		}
		if (filterOptions.isNotEmpty()) {
			if (whereConditions.isEmpty()) {
				append(" WHERE")
			} else {
				append(" AND")
			}
			var isFirst = true
			val groupedOptions = filterOptions.groupBy { it.groupKey }
			for ((_, group) in groupedOptions) {
				if (group.isEmpty()) {
					continue
				}
				if (isFirst) {
					isFirst = false
					append(' ')
				} else {
					append(" AND ")
				}
				if (group.size > 1) {
					group.joinTo(
						buffer = this,
						separator = " OR ",
						prefix = "(",
						postfix = ")",
						transform = ::getConditionOrThrow,
					)
				} else {
					append(getConditionOrThrow(group.single()))
				}
			}
		}
		groupBy?.let {
			append(" GROUP BY ")
			append(it)
		}
		orderBy?.let {
			append(" ORDER BY ")
			append(it)
		}
		if (limit > 0) {
			append(" LIMIT ")
			append(limit)
		}
	}.let { SimpleSQLiteQuery(it) }

	private fun getConditionOrThrow(option: ListFilterOption): String = when (option) {
		is ListFilterOption.Inverted -> "NOT(${getConditionOrThrow(option.option)})"
		else -> requireNotNull(conditionCallback.getCondition(option)) {
			"Unsupported filter option $option"
		}
	}

	fun interface ConditionCallback {

		fun getCondition(option: ListFilterOption): String?
	}
}
