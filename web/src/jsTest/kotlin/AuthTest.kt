import kotlinx.coroutines.test.runTest
import utils.hash
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthTest {

    @Test
    fun hashTest() = runTest {
        val hash = hash("foo")
        assertEquals("9/u6bgY2+JDlb7vzKD5STG+jIErimDgtYkdB0NxmODJuKCxBvl5CVNiCB3LFUYosWowMf37aGVlKfrU5RT4e1w==", hash)
    }

    @Test
    fun hashManyTest() = runTest {
        val hash = hash("foo", "bar", "baz")
        assertEquals("+UgXfdhOoVIqZNkC04ECxn5wEV+Oaij6/7UCPjgx4WENCMm3tq1wqv4Eslte9C5OHtRnoqkHDFhyGb8UWVQI5w==", hash)
    }
}