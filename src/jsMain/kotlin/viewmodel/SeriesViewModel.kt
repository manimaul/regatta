package viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.mxmariner.regatta.data.Series
import kotlinx.coroutines.launch

class SeriesViewModel {
    private val seriesState = mutableStateOf<List<Series>>(emptyList())
   init {
       Scopes.mainScope.launch {
           seriesState.value = Network.fetch("allSeries")
       }
   }

    fun deleteSeries(series: Series) {
        Scopes.mainScope.launch {
            Network.delete("series", mapOf("id" to "${series.id}"))
            seriesState.value = Network.fetch("allSeries")
        }
    }

    fun addSeries(series: Series) {
        Scopes.mainScope.launch {
            val newSeries: Series = Network.post("series", series)
            seriesState.value += newSeries
        }
    }

   val series: List<Series>
       get() = seriesState.value
}

@Composable
fun provideSeriesViewModel(): SeriesViewModel {
    return remember { SeriesViewModel() }
}
