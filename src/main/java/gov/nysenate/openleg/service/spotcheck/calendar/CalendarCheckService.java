package gov.nysenate.openleg.service.spotcheck.calendar;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import gov.nysenate.openleg.model.base.Version;
import gov.nysenate.openleg.model.calendar.Calendar;
import gov.nysenate.openleg.model.calendar.*;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.service.spotcheck.base.SpotCheckService;
import gov.nysenate.openleg.util.DateUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class CalendarCheckService implements SpotCheckService<CalendarId, Calendar, Calendar> {

    @Override
    public SpotCheckObservation<CalendarId> check(Calendar content) throws ReferenceDataNotFoundEx {
        throw new NotImplementedException(":P");
    }

    @Override
    public SpotCheckObservation<CalendarId> check(Calendar content, LocalDateTime start, LocalDateTime end) throws ReferenceDataNotFoundEx {
        throw new NotImplementedException(":P");
    }

    @Override
    public SpotCheckObservation<CalendarId> check(Calendar content, Calendar reference) {
        SpotCheckReferenceId referenceId = new SpotCheckReferenceId(
                SpotCheckRefType.LBDC_CALENDAR_ALERT, reference.getPublishedDateTime());
        SpotCheckObservation<CalendarId> observation = new SpotCheckObservation<>(referenceId, reference.getId());

        if (calendarsEqual(content, reference)) {
            return observation;
        } else {
            compareSupplementals(observation, content, reference);
            compareActiveLists(observation, content, reference);
            return observation;
        }
    }

    /**
     * Compare Calendar equality without checking published date.
     */
    private boolean calendarsEqual(Calendar content, Calendar other) {
        return Objects.equals(content.getId(), other.getId()) &&
               Objects.equals(content.getSupplementalMap(), other.getSupplementalMap()) &&
               Objects.equals(content.getActiveListMap(), other.getActiveListMap());
    }

    private void compareSupplementals(SpotCheckObservation<CalendarId> observation, Calendar content, Calendar reference) {
        Set<CalendarSupplemental> contentSupplementals = ImmutableSet.copyOf(content.getSupplementalMap().values());
        Set<CalendarSupplemental> referenceSupplementals = ImmutableSet.copyOf(reference.getSupplementalMap().values());

        Set<CalendarSupplemental> differenceSet = Sets.symmetricDifference(contentSupplementals, referenceSupplementals).immutableCopy();
        for (CalendarSupplemental diff : differenceSet) {
            recordMismatch(observation, content, reference, diff);
        }
    }

    private void recordMismatch(SpotCheckObservation<CalendarId> observation, Calendar content, Calendar reference, CalendarSupplemental diff) {
        CalendarSupplemental contentSuppDiff = getMatchingSupplementalIfExists(content, diff);
        CalendarSupplemental referenceSuppDiff = getMatchingSupplementalIfExists(reference, diff);

        if (contentSuppDiff == null || referenceSuppDiff == null) {
            recordVersionMismatch(observation, contentSuppDiff, referenceSuppDiff);
        } else {
            checkForSupplementalCalDateMismatch(observation, contentSuppDiff, referenceSuppDiff);

            Set<CalendarSectionType> contentSectionTypes = contentSuppDiff.getSectionEntries().keySet();
            Set<CalendarSectionType> referenceSectionTypes = referenceSuppDiff.getSectionEntries().keySet();

            checkForTypeMismatch(observation, contentSectionTypes, referenceSectionTypes);
            checkForSuppEntryMismatch(observation, contentSuppDiff, referenceSuppDiff);
        }
    }

    private void recordVersionMismatch(SpotCheckObservation<CalendarId> observation, CalendarSupplemental contentSuppDiff,
                                       CalendarSupplemental referenceSuppDiff) {
        String contentTypeString = getVersionString(contentSuppDiff);
        String referenceTypeString = getVersionString(referenceSuppDiff);
        observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.SUPPLEMENTAL_VERSION, referenceTypeString, contentTypeString));
    }

    /**
     * Converts a supplemental's {@link Version} into a more informative string for mismatches.
     * <p>Returns:</p>
     * <ul>
     * <li>Empty string if the supplemental is null</li>
     * <li>"DEFAULT" if the Version = Version.DEFAULT</li>
     * </ul>
     * This is different from the Version.toString() behavior of returning an empty string for the Default version.
     */
    private String getVersionString(CalendarSupplemental contentSuppDiff) {
        String versionString = "";
        if (contentSuppDiff != null) {
            Version v = contentSuppDiff.getVersion();
            versionString = v.equals(Version.DEFAULT) ? "DEFAULT" : v.toString();
        }
        return versionString;
    }

    private void checkForSupplementalCalDateMismatch(SpotCheckObservation<CalendarId> observation, CalendarSupplemental contentSuppDiff,
                                                     CalendarSupplemental referenceSuppDiff) {
        String contentDate = contentSuppDiff.getCalDate() == null ? "" : contentSuppDiff.getCalDate().toString();
        String referenceDate = referenceSuppDiff.getCalDate() == null ? "" : referenceSuppDiff.getCalDate().toString();
        if (!StringUtils.equals(contentDate, referenceDate)) {
            observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.SUPPLEMENTAL_CAL_DATE, referenceDate, contentDate));
        }

    }

    private void checkForTypeMismatch(SpotCheckObservation<CalendarId> observation, Set<CalendarSectionType> contentSectionTypes,
                                      Set<CalendarSectionType> referenceSectionTypes) {
        if (!Sets.symmetricDifference(contentSectionTypes, referenceSectionTypes).isEmpty()) {
            observation.addMismatch(new SpotCheckMismatch(
                    SpotCheckMismatchType.SUPPLEMENTAL_SECTION_TYPE, StringUtils.join(referenceSectionTypes, "\n"),
                    StringUtils.join(contentSectionTypes, "\n")));
        }
    }

    private void checkForSuppEntryMismatch(SpotCheckObservation<CalendarId> observation, CalendarSupplemental contentSuppDiff,
                                           CalendarSupplemental referenceSuppDiff) {
        Map<String, CalendarSupplementalEntry> contentEntryMap = getStringToEntryMap(contentSuppDiff);
        Map<String, CalendarSupplementalEntry> referenceEntryMap = getStringToEntryMap(referenceSuppDiff);

        Set<String> entryDiffs = Sets.symmetricDifference(contentEntryMap.keySet(), referenceEntryMap.keySet());
        for (String diff : entryDiffs) {
            CalendarSupplementalEntry contentDiff = contentEntryMap.get(diff);
            CalendarSupplementalEntry referenceDiff = referenceEntryMap.get(diff);

            observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.SUPPLEMENTAL_ENTRY,
                                                          referenceDiff == null ? "" : referenceDiff.toString(),
                                                          contentDiff == null ? "" : contentDiff.toString()));
        }
    }

    private Map<String, CalendarSupplementalEntry> getStringToEntryMap(CalendarSupplemental supplemental) {
        Map<String, CalendarSupplementalEntry> stringToEntryMap = new HashMap<>();
        for (CalendarSupplementalEntry entry : supplemental.getAllEntries()) {
            stringToEntryMap.put(entry.toString(), entry);
        }
        return stringToEntryMap;
    }

    /**
     * Returns the matching supplemental from a calendar, null if it no match exists.
     */
    private CalendarSupplemental getMatchingSupplementalIfExists(Calendar calendar, CalendarSupplemental diff) {
        return calendar.getSupplementalMap().keySet().contains(diff.getVersion()) ? calendar.getSupplemental(diff.getVersion()) : null;
    }

    private void compareActiveLists(SpotCheckObservation<CalendarId> observation, Calendar content, Calendar reference) {
        if (content.getActiveListMap().size() > 0 && reference.getActiveListMap().size() > 0) {
            CalendarActiveList contentMostRecent = getMostRecentActiveList(content);
            CalendarActiveList referenceMostRecent = getMostRecentActiveList(reference);

            checkForActiveListCalDateMismatch(observation, contentMostRecent, referenceMostRecent);
            checkForActiveListEntryMismatch(observation, contentMostRecent, referenceMostRecent);
        }
        else if (content.getActiveListMap().size() > 0 || reference.getActiveListMap().size() > 0) {
            observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.ACTIVE_LIST_SIZE,
                                    reference.getActiveListMap().size(), content.getActiveListMap().size()));
        }
    }

    private CalendarActiveList getMostRecentActiveList(Calendar calendar) {
        int seqNo = 0;
        LocalDateTime releaseDateTime = DateUtils.LONG_AGO.atStartOfDay();
        for (CalendarActiveList activeList : calendar.getActiveListMap().values()) {
            if (activeList.getReleaseDateTime().isAfter(releaseDateTime)) {
                seqNo = activeList.getSequenceNo();
                releaseDateTime = activeList.getReleaseDateTime();
            }
        }
        return calendar.getActiveList(seqNo);
    }

    private void checkForActiveListCalDateMismatch(SpotCheckObservation<CalendarId> observation, CalendarActiveList contentDiff,
                                                   CalendarActiveList referenceDiff) {
        String contentCalDate = contentDiff.getCalDate() == null ? "" : contentDiff.getCalDate().toString();
        String referenceCalDate = referenceDiff.getCalDate() == null ? "" : referenceDiff.getCalDate().toString();
        if (!StringUtils.equals(contentCalDate, referenceCalDate)) {
            observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.ACTIVE_LIST_CAL_DATE, referenceCalDate, contentCalDate));
        }
    }

    private void checkForActiveListEntryMismatch(SpotCheckObservation<CalendarId> observation, CalendarActiveList contentDiff,
                                                 CalendarActiveList referenceDiff) {
        Set<CalendarEntry> contentDiffEntries = Sets.newHashSet(contentDiff.getEntries());
        Set<CalendarEntry> referenceDiffEntries = Sets.newHashSet(referenceDiff.getEntries());
        if (!Sets.symmetricDifference(contentDiffEntries, referenceDiffEntries).isEmpty()) {
            observation.addMismatch(new SpotCheckMismatch(
                    SpotCheckMismatchType.ACTIVE_LIST_ENTRY,
                    StringUtils.join(referenceDiffEntries, "\n"), StringUtils.join(contentDiffEntries, "\n")));
        }
    }

}
