package com.example.galeria.domain

import com.example.galeria.data.model.MediaItem
import com.example.galeria.domain.model.GalleryEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Convierte la lista ordenada (más reciente primero) en una lista plana
 * con un encabezado «Nov 2026» al comienzo de cada mes.
 * Es una operación pesada con muchos elementos: debe ejecutarse fuera del hilo principal.
 */
class GroupMediaByMonthUseCase(private val locale: Locale = Locale.getDefault()) {

    operator fun invoke(items: List<MediaItem>): List<GalleryEntry> {
        val result = ArrayList<GalleryEntry>(items.size + MONTHS_ESTIMATE)
        val calendar = Calendar.getInstance()
        val format = SimpleDateFormat("MMM yyyy", locale)
        var currentMonthKey = Int.MIN_VALUE

        for (item in items) {
            calendar.timeInMillis = item.dateMillis
            val monthKey = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
            if (monthKey != currentMonthKey) {
                currentMonthKey = monthKey
                val title = format.format(Date(item.dateMillis))
                    .replace(".", "")
                    .replaceFirstChar { it.titlecase(locale) }
                result.add(GalleryEntry.Header(key = "h_$monthKey", title = title))
            }
            result.add(GalleryEntry.Media(item))
        }
        return result
    }

    private companion object {
        const val MONTHS_ESTIMATE = 240
    }
}
