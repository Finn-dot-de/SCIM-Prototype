package spring.and.scim.de.prototype.scim;

import com.unboundid.scim2.common.messages.ListResponse;

import java.util.List;

/**
 * Seitenweise Auslieferung einer Trefferliste nach RFC 7644, Abschnitt 3.4.2.4.
 * <p>
 * Reine Funktion ohne Zustand, daher statisch und ohne Spring-Kontext testbar.
 */
public final class ScimPagination {

    /** SCIM zaehlt 1-basiert, nicht 0-basiert. */
    private static final int FIRST_INDEX = 1;

    private ScimPagination() {
    }

    /**
     * @param startIndex 1-basierter Index des ersten Treffers; kleinere Werte
     *                   zaehlen laut RFC als 1
     * @param count      gewuenschte Seitengroesse; negative Werte zaehlen als 0
     */
    public static <T> ListResponse<T> paginate(List<T> allMatches, int startIndex, int count) {
        int effectiveStart = Math.max(FIRST_INDEX, startIndex);
        int effectiveCount = Math.max(0, count);

        int from = Math.min(effectiveStart - FIRST_INDEX, allMatches.size());
        int to = Math.min(allMatches.size(), from + effectiveCount);
        List<T> page = List.copyOf(allMatches.subList(from, to));

        // itemsPerPage ist laut RFC die tatsaechlich gelieferte Anzahl,
        // nicht die angefragte.
        return new ListResponse<>(allMatches.size(), page, effectiveStart, page.size());
    }
}
