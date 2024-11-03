package mobi.sevenwinds.app.budget

import io.restassured.RestAssured
import mobi.sevenwinds.app.author.AuthorRecordRequest
import mobi.sevenwinds.app.author.AuthorRecordResponse
import mobi.sevenwinds.common.ServerTest
import mobi.sevenwinds.common.jsonBody
import mobi.sevenwinds.common.toResponse
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.joda.time.DateTime
import org.junit.Assert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class BudgetApiKtTest : ServerTest() {

    @BeforeEach
    internal fun setUp() {
        transaction { BudgetTable.deleteAll() }
    }

    @Test
    fun testBudgetPagination() {
        addRecord(BudgetCreateRecordRequest(2020, 5, 10, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 5, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 20, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 30, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 40, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2030, 1, 1, BudgetType.Расход))

        RestAssured.given()
            .queryParam("limit", 3)
            .queryParam("offset", 1)
            .get("/budget/year/2020/stats")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println("${response.total} / ${response.items} / ${response.totalByType}")

                Assert.assertEquals(5, response.total)
                Assert.assertEquals(3, response.items.size)
                Assert.assertEquals(105, response.totalByType[BudgetType.Приход.name])
            }
    }

    @Test
    fun testStatsSortOrder() {
        addRecord(BudgetCreateRecordRequest(2020, 5, 100, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 1, 5, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 50, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 1, 30, BudgetType.Приход))
        addRecord(BudgetCreateRecordRequest(2020, 5, 400, BudgetType.Приход))

        // expected sort order - month ascending, amount descending

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                println(response.items)

                Assert.assertEquals(30, response.items[0].amount)
                Assert.assertEquals(5, response.items[1].amount)
                Assert.assertEquals(400, response.items[2].amount)
                Assert.assertEquals(100, response.items[3].amount)
                Assert.assertEquals(50, response.items[4].amount)
            }
    }

    @Test
    fun testStatsWithFioFilter() {
        addAuthorRecord(AuthorRecordRequest("Pushkin"))
        addAuthorRecord(AuthorRecordRequest("Esenin"))
        addAuthorRecord(AuthorRecordRequest("Bonopart"))

        addRecord(BudgetCreateRecordRequest(2020, 5, 100, BudgetType.Приход, 1))
        addRecord(BudgetCreateRecordRequest(2020, 1, 5, BudgetType.Приход, 1))
        addRecord(BudgetCreateRecordRequest(2020, 1, 5, BudgetType.Приход, 1))
        addRecord(BudgetCreateRecordRequest(2020, 5, 50, BudgetType.Приход, 2))
        addRecord(BudgetCreateRecordRequest(2020, 1, 30, BudgetType.Приход, 2))
        addRecord(BudgetCreateRecordRequest(2020, 5, 400, BudgetType.Приход, 3))

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fio=Pushkin")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(3, response.total)
            }

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fio=Esenin")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(2, response.total)
            }

        RestAssured.given()
            .get("/budget/year/2020/stats?limit=100&offset=0&fio=Bonopart")
            .toResponse<BudgetYearStatsResponse>().let { response ->
                Assert.assertEquals(1, response.total)
            }
    }

    @Test
    fun testInvalidMonthValues() {
        RestAssured.given()
            .jsonBody(BudgetCreateRecordRequest(2020, -5, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)

        RestAssured.given()
            .jsonBody(BudgetCreateRecordRequest(2020, 15, 5, BudgetType.Приход))
            .post("/budget/add")
            .then().statusCode(400)
    }

    private fun addRecord(record: BudgetCreateRecordRequest) {
        RestAssured.given()
            .jsonBody(record)
            .post("/budget/add")
            .toResponse<BudgetCreateRecordRequest>().let { response ->
                Assert.assertEquals(record, response)
            }
    }

    private fun addAuthorRecord(authorRecordRequest: AuthorRecordRequest) {
        RestAssured.given()
            .jsonBody(authorRecordRequest)
            .post("/author/add")
            .toResponse<AuthorRecordResponse>().let { response ->
                val newResponse = AuthorRecordResponse(authorRecordRequest.fio, DateTime.now().toString())
                Assert.assertEquals(authorRecordRequest.fio, newResponse.fio)
            }
    }
}