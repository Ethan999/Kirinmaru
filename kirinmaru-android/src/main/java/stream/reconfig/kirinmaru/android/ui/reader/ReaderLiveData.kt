package stream.reconfig.kirinmaru.android.ui.reader

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.support.annotation.MainThread
import android.support.annotation.WorkerThread
import io.reactivex.Flowable
import io.reactivex.Single
import stream.reconfig.kirinmaru.android.db.ChapterDao
import stream.reconfig.kirinmaru.android.logd
import stream.reconfig.kirinmaru.android.parcel.ChapterIdParcel
import stream.reconfig.kirinmaru.android.parcel.NovelParcel
import stream.reconfig.kirinmaru.android.prefs.CurrentReadPref
import stream.reconfig.kirinmaru.android.util.offline.ResourceContract
import stream.reconfig.kirinmaru.android.util.offline.SimpleResourceLiveData
import stream.reconfig.kirinmaru.android.util.textview.HtmlTextUtil
import stream.reconfig.kirinmaru.android.vo.Chapter
import stream.reconfig.kirinmaru.core.ChapterDetail
import stream.reconfig.kirinmaru.core.ChapterId
import stream.reconfig.kirinmaru.core.taxonomy.Taxonomy
import stream.reconfig.kirinmaru.plugins.PluginMap
import stream.reconfig.kirinmaru.plugins.getPlugin
import javax.inject.Inject

class ReaderLiveData @Inject constructor(
    private val pluginMap: PluginMap,
    private val chapterDao: ChapterDao,
    private val currentReadPref: CurrentReadPref
) : SimpleResourceLiveData<ReaderDetail, Chapter, ChapterDetail>() {

  private val readerData = MutableLiveData<ReaderParcel>()

  private val localObserver = Observer<ReaderParcel> { refresh() }

  override fun createContract() = object : ResourceContract<ReaderDetail, Chapter, ChapterDetail> {
    override fun local(): Flowable<Chapter> {
      return chapterDao.chapterAsync(chapterId().url)
    }

    override fun remote(): Single<ChapterDetail> {
      return pluginMap.getPlugin(novel().origin)
          .obtainDetail(novel(), chapterId())
    }

    override fun persist(data: ChapterDetail) {
      val chapter = Chapter(
          origin = novel().origin,
          novelUrl = novel().url,
          url = chapterId().url,
          rawText = data.rawText,
          nextUrl = data.nextUrl,
          previousUrl = data.previousUrl)
      log(chapter, "persist")
      chapterDao.upsert(chapter)
    }

    override fun view(local: Chapter): ReaderDetail {
      log(local, "view")
      return local.toReaderDetail()
    }

    override fun autoFetch() = true
  }

  @MainThread
  fun initReaderData(data: ReaderParcel) {
    readerData.value = data
  }

  fun navigateNext() {
    value?.nextUrl?.run(::navigateActual)
        ?: postError("Next chapter doesn't exist")

  }

  fun navigatePrevious() {
    value?.previousUrl?.run(::navigateActual)
        ?: postError("Previous chapter doesn't exist")
  }

  fun absoluteUrl(): String? {
    return pluginMap.getPlugin(novel().origin).toAbsoluteUrl(novel(), chapterId())
  }

  override fun onActive() {
    super.onActive()
    readerData.observeForever(localObserver)
  }

  override fun onInactive() {
    super.onInactive()
    readerData.removeObserver(localObserver)
    readerData.value?.let {
      currentReadPref.persist(it.novelParcel, it.chapterParcel.url)
    }
  }

  private fun chapterId(): ChapterId = readerData.value!!.chapterParcel

  private fun novel(): NovelParcel = readerData.value!!.novelParcel

  private fun navigateActual(url: String) {
    readerData.value?.let {
      postValue(null)
      readerData.postValue(it.copy(chapterParcel = ChapterIdParcel(url)))
    } ?: throw IllegalStateException("Novel must be set before any operation")
  }

  @WorkerThread
  private fun Chapter.toReaderDetail() = ReaderDetail(
      text = rawText?.let(HtmlTextUtil::toHtmlSpannable),
      url = url,
      previousUrl = previousUrl,
      nextUrl = nextUrl,
      taxon = Taxonomy.createTaxonomicDisplay(Taxonomy.createTaxonomicNumber(url))
  )

  private fun log(chapter: Chapter?, ops: String = "") {
    logd("State: $ops [${resourceState.value}]" +
        "\nLocal -> Length: [${chapter?.rawText?.length}] Pre: [${chapter?.previousUrl}] Nex: [${chapter?.nextUrl}]" +
        "\nNovel: [${novel().url}]" +
        "\nChapter: [${chapterId().url}] ")
  }
}