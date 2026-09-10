package eu.kanade.tachiyomi.extension.es.catharsisworld

import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import keiyoushi.utils.tryParseDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Serializable
class DataDto<T>(
    val data: T,
    private val page: Int,
    @SerialName("total_pages") private val totalPages: Int,
) {
    fun hasNextPage(): Boolean = page < totalPages
}

@Serializable
class MangaDto(
    private val id: Int,
    @SerialName("nombre") private val name: String,
    @SerialName("descripcion") private val description: String? = null,
    @SerialName("portada_url") private val cover: String,
    @SerialName("estado") private val status: String? = null,
    @SerialName("fk_generos") private val genres: List<GenreWrapperDto> = emptyList(),
    @SerialName("capitulos") val chapters: List<ChapterDto> = emptyList(),
) {
    fun toSManga(baseUrl: String): SManga = SManga.create().apply {
        url = id.toString()
        title = name
        description = this@MangaDto.description
        thumbnail_url = "$baseUrl/assets/$cover"
        status = when (this@MangaDto.status) {
            "curso", "published" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        genre = this@MangaDto.genres.joinToString(", ") { it.data.name }
    }
}

@Serializable
class GenreWrapperDto(
    @SerialName("generos_id") val data: GenreDto,
)

@Serializable
class GenreDto(
    @SerialName("nombre") val name: String,
)

private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    .withZone(ZoneId.of("UTC"))

@Serializable
class ChapterDto(
    @SerialName("numero") val number: JsonPrimitive,
    @SerialName("titulo") private val name: String,
    @SerialName("date_created") private val date: String,
) {
    fun toSChapter(mangaId: String): SChapter = SChapter.create().apply {
        url = "$mangaId/$number"
        name = this@ChapterDto.name
        date_upload = dateFormat.tryParseDate(date)
    }
}

@Serializable
class PageDto(
    @SerialName("redirect_url") val redirectUrl: String,
    @SerialName("redirect_data") val redirectData: String,
)
