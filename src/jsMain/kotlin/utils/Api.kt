package utils

import com.mxmariner.regatta.data.*

object Api {

    suspend fun postBoat(boat: Boat) =
        Network.post<Boat, Boat>(ApiPaths.boat, boat)

    suspend fun getAllPeople() =
        Network.get<List<Person>>(ApiPaths.allSkippers)

    suspend fun getAllBoats() =
        Network.get<List<BoatSkipper>>(ApiPaths.allBoats)

    suspend fun postPerson(person: Person) =
        Network.post<Person, Person>(ApiPaths.skipper, person)

    suspend fun deletePerson(id: Long) =
        Network.delete(ApiPaths.skipper, mapOf("id" to "$id"))

    suspend fun deleteSeries(id: Long) =
        Network.delete(ApiPaths.series, mapOf("id" to "$id"))

    suspend fun allSeries() =
        Network.get<List<Series>>(ApiPaths.allSeries)

    suspend fun getSeries(id: Long) =
        Network.get<Series>(ApiPaths.series, mapOf("id" to "$id"))

    suspend fun postSeries(series: Series) =
        Network.post<Series, Series>(ApiPaths.series, series)

    suspend fun postAuth(auth: AuthRecord) =
        Network.post<AuthRecord, AuthRecord>(ApiPaths.auth, auth)

    suspend fun login(login: Login) =
        Network.post<Login, LoginResponse>(ApiPaths.login, login)

    suspend fun deleteBoat(id: Long) =
        Network.delete(ApiPaths.boat, mapOf("id" to "$id"))

//    suspend fun getAllClasses() =
//        Network.get<List<Bracket>>(ApiPaths.allBrackets)

    suspend fun getAllClasses() =
        Network.get<List<RaceClassBrackets>>(ApiPaths.allClasses)

    suspend fun postBracket(bracket: Bracket) =
        Network.post<Bracket, Bracket>(ApiPaths.bracket, bracket)

    suspend fun postClass(raceClass: RaceClass) =
        Network.post<RaceClass, RaceClass>(ApiPaths.raceClass, raceClass)

    suspend fun getPerson(id: Long) =
        Network.get<Person>(ApiPaths.skipper, mapOf("id" to "$id"))

    suspend fun getBoatSkipper(id: Long) =
        Network.get<BoatSkipper>(ApiPaths.boat, mapOf("id" to "$id"))

    suspend fun getClass(id: Long) =
        Network.get<Bracket>(ApiPaths.bracket, mapOf("id" to "$id"))

    suspend fun deleteBracket(id: Long) =
        Network.delete(ApiPaths.bracket, mapOf("id" to "$id"))

    suspend fun deleteClass(id: Long) =
        Network.delete(ApiPaths.raceClass, mapOf("id" to "$id"))

    suspend fun getCategory(id: Long) =
        Network.get<RaceClass>(ApiPaths.raceClass, mapOf("id" to "$id"))

    suspend fun getAllRaces(year: Int) =
        Network.get<List<RaceSchedule>>(ApiPaths.allRaces, mapOf("year" to "$year"))

    suspend fun postRace(race: Race) =
        Network.post<Race, Race>(ApiPaths.race, race)

    suspend fun deleteRace(id: Long) =
        Network.delete(ApiPaths.race, mapOf("id" to "$id"))

    suspend fun getRaceSchedule(id: Long) =
        Network.get<RaceSchedule>(ApiPaths.raceSchedule, mapOf("id" to "$id"))
    suspend fun getRace(id: Long) =
        Network.get<Race>(ApiPaths.race, mapOf("id" to "$id"))

    suspend fun getResults(raceId: Long) =
        Network.get<List<RaceResult>>(ApiPaths.results, mapOf("raceId" to "$raceId"))
    suspend fun deleteResult(id: Long) =
        Network.delete(ApiPaths.results, mapOf("id" to "$id"))

    suspend fun postResult(result: RaceResult) =
        Network.post<RaceResult, RaceResult>(ApiPaths.results, result)

    suspend fun getReport(raceId: Long) =
        Network.get<RaceReport>(ApiPaths.report, mapOf("raceId" to "$raceId"))

    suspend fun getYears() = Network.get<List<String>>(ApiPaths.years)
}
