package eu.kanade.tachiyomi.extension.es.catharsisworld

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.SMangaUpdate
import keiyoushi.annotation.Source
import keiyoushi.network.get
import keiyoushi.network.post
import keiyoushi.network.rateLimit
import keiyoushi.source.KeiSource
import keiyoushi.utils.asJsoup
import keiyoushi.utils.parseAs
import kotlinx.serialization.json.float
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient

@Source
abstract class CatharsisWorld : KeiSource() {

    private val apiUrl = "$baseUrl/api"

    override fun OkHttpClient.Builder.configureClient(): OkHttpClient.Builder = apply {
        rateLimit(3)
    }

    override suspend fun getPopularManga(page: Int): MangasPage {
        val result = client.get("$apiUrl/mangas/ranking?refresh=false")
            .parseAs<List<MangaDto>>()

        val mangas = result.map { it.toSManga(baseUrl) }

        return MangasPage(mangas, false)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val result = client.get("$apiUrl/mangas?limit=$PAGE_SIZE&page=$page&sort=-fecha_ultimo_capitulo")
            .parseAs<DataDto<List<MangaDto>>>()

        val mangas = result.data.map { it.toSManga(baseUrl) }

        return MangasPage(mangas, result.hasNextPage())
    }

    override suspend fun getSearchMangaList(page: Int, query: String, filters: FilterList): MangasPage {
        val url = "$apiUrl/mangas".toHttpUrl().newBuilder()
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("page", page.toString())
            .addQueryParameter("name", query)

        val result = client.get(url.build().toString())
            .parseAs<DataDto<List<MangaDto>>>()

        val mangas = result.data.map { it.toSManga(baseUrl) }

        return MangasPage(mangas, result.hasNextPage())
    }

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/manga/${manga.url}"

    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val result = client.get("$apiUrl/mangas/${manga.url}")
            .parseAs<MangaDto>()

        return SMangaUpdate(
            manga = result.toSManga(baseUrl),
            chapters = result.chapters
                .sortedByDescending { it.number.float }
                .map { it.toSChapter(manga.url) },
        )
    }

    override suspend fun getMangaByUrl(url: HttpUrl): SManga? {
        if (url.pathSegments[0] != "manga") {
            return null
        }

        val mangaId = url.pathSegments[1]
        val result = client.get("$apiUrl/mangas/$mangaId")
            .parseAs<MangaDto>()

        return result.toSManga(baseUrl)
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val (mangaId, chapterId) = chapter.url.split("/")
        return "$baseUrl/manga/$mangaId/chapter/$chapterId"
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        val result = client.get("$apiUrl/mangas/${chapter.url}")
            .parseAs<PageDto>()

        val formBody = FormBody.Builder()
            .add("data", result.redirectData)
            .build()

        val redirectDocument = client.post(result.redirectUrl, formBody)
            .asJsoup()

        return redirectDocument.select("div.reading-content > div.page-break > img").mapIndexed { index, element ->
            Page(index, imageUrl = element.attr("abs:src"))
        }
    }

    companion object {
        const val PAGE_SIZE = 30
    }
}
