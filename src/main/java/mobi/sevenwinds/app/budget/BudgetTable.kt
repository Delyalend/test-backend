package mobi.sevenwinds.app.budget

import mobi.sevenwinds.app.author.AuthorRecordResponse
import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object BudgetTable : IntIdTable("budget") {
    val year = integer("year")
    val month = integer("month")
    val amount = integer("amount")
    val type = enumerationByName("type", 100, BudgetType::class)
    val authorId = integer("author_id").nullable()
}

class BudgetEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<BudgetEntity>(BudgetTable)

    var year by BudgetTable.year
    var month by BudgetTable.month
    var amount by BudgetTable.amount
    var type by BudgetTable.type
    var authorId by BudgetTable.authorId

    fun toBudgetRecordResponse(author: AuthorRecordResponse? = null): BudgetRecordResponse {
        return BudgetRecordResponse(year, month, amount, type, author)
    }

    fun toBudgetCreateRecordResponse(authorId: Int? = null): BudgetCreateRecordResponse {
        return BudgetCreateRecordResponse(year, month, amount, type, authorId)
    }

}