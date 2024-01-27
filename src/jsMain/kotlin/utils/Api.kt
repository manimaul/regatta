package utils

import com.mxmariner.regatta.data.*

object Api {

    suspend fun postBoat(boat: Boat) =
        Network.post<Boat, Boat>("boat", boat)

    suspend fun getAllPeople() =
        Network.get<List<Person>>("people")

    suspend fun getAllBoats() =
        Network.get<List<Boat>>("allBoats")

    suspend fun postPerson(person: Person) =
        Network.post<Person, Person>("person", person)

    suspend fun deletePerson(id: Long) =
        Network.delete("person", mapOf("id" to "$id"))

    suspend fun deleteSeries(id: Long) =
        Network.delete("series", mapOf("id" to "$id"))

    suspend fun allSeries() =
        Network.get<List<Series>>("allSeries")

    suspend fun getSeries(id: Long) =
        Network.get<Series>("series", mapOf("id" to "$id"))

    suspend fun postSeries(series: Series) =
        Network.post<Series, Series>("series", series)

    suspend fun postAuth(auth: AuthRecord) =
        Network.post<AuthRecord, AuthRecord>("auth", auth)

    suspend fun login(login: Login) =
        Network.post<Login, LoginResponse>("login", login)

    suspend fun deleteBoat(id: Long) =
        Network.delete("boat", mapOf("id" to "$id"))

    suspend fun getAllClasses() =
        Network.get<List<RaceClass>>("allClasses")

    suspend fun getAllCategories() =
        Network.get<List<RaceClassCategory>>("allCategories")

    suspend fun postClass(raceClass: RaceClass) =
        Network.post<RaceClass, RaceClass>("raceClass", raceClass)

    suspend fun postCategory(raceClass: RaceClassCat) =
        Network.post<RaceClassCat, RaceClassCat>("raceCategory", raceClass)

    suspend fun getPerson(id: Long) =
        Network.get<Person>("person", mapOf("id" to "$id"))

    suspend fun getBoat(id: Long) =
        Network.get<Boat>("boat", mapOf("id" to "$id"))

    suspend fun getClass(id: Long) =
        Network.get<RaceClass>("raceClass", mapOf("id" to "$id"))

    suspend fun deleteClass(id: Long) =
        Network.delete("raceClass", mapOf("id" to "$id"))

    suspend fun deleteCategory(id: Long) =
        Network.delete("category", mapOf("id" to "$id"))

    suspend fun getCategory(id: Long) =
        Network.get<RaceCategory>("raceCategory", mapOf("id" to "$id"))

    suspend fun getAllRaces() =
        Network.get<List<RaceFull>>("allRaces")

    suspend fun postRace(race: Race) =
        Network.post<Race, RaceFull>("races", race)

    suspend fun deleteRace(id: Long) =
        Network.delete("races", mapOf("id" to "$id"))

    suspend fun getRace(id: Long) =
        Network.get<RaceFull>("races", mapOf("id" to "$id"))

    suspend fun getResults(raceId: Long) =
        Network.get<List<RaceResultFull>>("results", mapOf("raceId" to "$raceId"))
    suspend fun deleteResult(id: Long) =
        Network.delete("results", mapOf("id" to "$id"))

    suspend fun postResult(result: RaceResult) =
        Network.post<RaceResult, RaceResultFull>("results", result)

    suspend fun getReport(raceId: Long) =
        Network.get<RaceReport>("report", mapOf("raceId" to "$raceId"))
}
