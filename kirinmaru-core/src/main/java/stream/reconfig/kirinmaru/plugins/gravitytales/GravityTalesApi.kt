package stream.reconfig.kirinmaru.plugins.gravitytales

import com.google.gson.annotations.SerializedName
import io.reactivex.Single
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url
import stream.reconfig.kirinmaru.core.ChapterId
import stream.reconfig.kirinmaru.core.NovelDetail

/**
 * GravityTales WordPress API for Retrofit with lenient url parameters
 */
internal interface GravityTalesApi {

  @GET("api/novels")
  fun getNovels(): Single<Response<List<GTNovelDetail>>>

  @GET("api/novels/chaptergroups/{id}")
  fun getChapterGroups(@Path("id") novelId: String): Single<Response<List<GTChapterGroups>>>

  @GET("api/novels/chaptergroup/{id}")
  fun getChapterId(@Path("id") groupId: Int): Single<Response<List<GTChapterId>>>

  @GET
  fun getChapterDetail(@Url url: String): Single<Response<ResponseBody>>

  data class GTNovelDetail(
      @SerializedName("Name") override val novelTitle: String = "",
      @SerializedName("Slug") override val url: String = "",
      @SerializedName("Id") override val id: String? = null,
      @Transient override val tags: Set<String> = emptySet()
  ) : NovelDetail {
    @Transient
    override val origin: String = GRAVITYTALES_ORIGIN
  }

  data class GTChapterGroups(
      val chapterGroupId: Int,
      val fromChapterNumber: Int,
      val order: Int,
      val toChapterNumber: Int
  )

  data class GTChapterId(
      @SerializedName("Slug") override val url: String,
      @SerializedName("Name") override val title: String?
  ) : ChapterId
}