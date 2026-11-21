package app.kairo.data.db

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalTime

/** Room stores only simple types; dates go in as numbers so they sort and compare correctly. */
class Converters {
    @TypeConverter fun dateToDays(d: LocalDate?): Long? = d?.toEpochDay()
    @TypeConverter fun daysToDate(v: Long?): LocalDate? = v?.let(LocalDate::ofEpochDay)
    @TypeConverter fun timeToSeconds(t: LocalTime?): Int? = t?.toSecondOfDay()
    @TypeConverter fun secondsToTime(v: Int?): LocalTime? = v?.let { LocalTime.ofSecondOfDay(it.toLong()) }
}
