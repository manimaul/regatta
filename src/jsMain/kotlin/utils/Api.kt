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
    suspend fun postCategory(raceClass: RaceClassCategory) =
        Network.post<RaceClassCategory, RaceClassCategory>("raceCategory", raceClass)

    suspend fun getPerson(id: Long) =
        Network.get<Person>("person", mapOf("id" to "$id"))

}
