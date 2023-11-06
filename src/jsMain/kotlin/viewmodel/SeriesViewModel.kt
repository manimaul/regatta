package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch
import utils.Network
import utils.Scopes.mainScope

class SeriesViewModel {
    private val seriesState = mutableStateOf<List<Series>>(emptyList())
   init {
       mainScope.launch {
           Network.get<List<Series>>("allSeries").let {
               seriesState.value = it.body ?: emptyList()
           }
       }
   }

    fun deleteSeries(series: Series) {
        mainScope.launch {
            Network.delete("series", mapOf("id" to "${series.id}"))
            seriesState.value = Network.get<List<Series>>("allSeries").body ?: emptyList()
        }
    }

    fun addSeries(series: Series) {
        mainScope.launch {
            val newSeries: Series? = Network.post<Series, Series>("series", series).body
            newSeries?.let {
                seriesState.value += newSeries
            }
        }
    }

   val series: List<Series>
       get() = seriesState.value
}

@Composable
fun provideSeriesViewModel(): SeriesViewModel {
    return remember { SeriesViewModel() }
}
