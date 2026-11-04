package com.example.galeria.domain

import com.example.galeria.data.model.MediaItem
import com.example.galeria.domain.model.GalleryEntry
import com.example.galeria.domain.model.GroupedGallery
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Convierte la lista ordenada (más reciente primero) en listas planas con un
 * encabezado al comienzo de cada mes y, por separado, de cada año.
 * Con muchos elementos es una operación pesada: ejecutar fuera del hilo principal.
 */
class GroupMediaByDateUseCase(private val locale: Locale = Locale.getDefault()) {

    operator fun invoke(items: List<MediaItem>): GroupedGallery {
        val mediaEntries = items.map { GalleryEntry.Media(it) }
        val byMonth = ArrayList<GalleryEntry>(items.size + MONTHS_ESTIMATE)
        val byYear = ArrayList<GalleryEntry>(items.size + YEARS_ESTIMATE)

        val calendar = Calendar.getInstance()
        val monthFormat = SimpleDateFormat("MMM yyyy", locale)
        var currentMonthKey = Int.MIN_VALUE
        var currentYear = Int.MIN_VALUE

        for (i in items.indices) {
            val item = items[i]
            calendar.timeInMillis = item.dateMillis
            val year = calendar.get(Calendar.YEAR)
            val monthKey = year * 12 + calendar.get(Calendar.MONTH)

            if (monthKey != currentMonthKey) {
                currentMonthKey = monthKey
                val title = monthFormat.format(calendar.time)
                    .replace(".", "")
                    .replaceFirstChar { it.titlecase(locale) }
                byMonth.add(GalleryEntry.Header(key = "h_$monthKey", title = title))
            }
            if (year != currentYear) {
                currentYear = year
                byYear.add(GalleryEntry.Header(key = "y_$year", title = year.toString()))
            }

            byMonth.add(mediaEntries[i])
            byYear.add(mediaEntries[i])
        }
        return GroupedGallery(byMonth = byMonth, byYear = byYear)
    }

    private companion object {
        const val MONTHS_ESTIMATE = 240
        const val YEARS_ESTIMATE = 30
    }
}
