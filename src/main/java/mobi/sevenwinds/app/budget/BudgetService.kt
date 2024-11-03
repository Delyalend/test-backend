package mobi.sevenwinds.app.budget

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mobi.sevenwinds.app.author.AuthorEntity
import mobi.sevenwinds.app.author.AuthorTable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

object BudgetService {
    suspend fun addRecord(body: BudgetCreateRecordRequest): BudgetCreateRecordResponse = withContext(Dispatchers.IO) {
        transaction {
            val entity = BudgetEntity.new {
                this.year = body.year
                this.month = body.month
                this.amount = body.amount
                this.type = body.type
                this.authorId = body.authorId
            }

            return@transaction entity.toBudgetCreateRecordResponse(body.authorId)
        }
    }

    suspend fun getYearStats(param: BudgetYearParam): BudgetYearStatsResponse = withContext(Dispatchers.IO) {
        transaction {
            val query =
                if (param.fio == null) {
                    BudgetTable
                        .select { BudgetTable.year eq param.year }
                        .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                } else {
                    val authorQueryByFio = AuthorTable
                        .select { AuthorTable.fio eq param.fio }

                    val authorIdList = AuthorEntity.wrapRows(authorQueryByFio).map { it.toId() }

                    BudgetTable
                        .select {
                            (BudgetTable.year eq param.year) and (BudgetTable.authorId inList authorIdList)
                        }
                        .orderBy(BudgetTable.month to SortOrder.ASC, BudgetTable.amount to SortOrder.DESC)
                }

            val total = query.count()

            val unlimitedData = BudgetEntity.wrapRows(query).map { it.toBudgetRecordResponse() }
            val sumByType = unlimitedData.groupBy { it.type.name }.mapValues { it.value.sumOf { v -> v.amount } }

            query.limit(param.limit, param.offset)
            val limitedData = BudgetEntity.wrapRows(query).map {
                val authorId = it.authorId
                if (authorId == null) {
                    it.toBudgetRecordResponse()
                } else {
                    val authorQuery = AuthorTable
                        .select { AuthorTable.id eq authorId }
                        .single()

                    val author = AuthorEntity.wrapRow(authorQuery)
                    it.toBudgetRecordResponse(author.toResponse())
                }
            }

            return@transaction BudgetYearStatsResponse(
                total = total,
                totalByType = sumByType,
                items = limitedData,
            )
        }
    }
}