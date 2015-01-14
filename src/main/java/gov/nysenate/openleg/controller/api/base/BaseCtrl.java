package gov.nysenate.openleg.controller.api.base;

import com.google.common.collect.Range;
import com.google.common.eventbus.EventBus;
import gov.nysenate.openleg.client.response.error.ErrorCode;
import gov.nysenate.openleg.client.response.error.ErrorResponse;
import gov.nysenate.openleg.client.response.error.ViewObjectErrorResponse;
import gov.nysenate.openleg.client.view.error.InvalidParameterView;
import gov.nysenate.openleg.client.view.request.ParameterView;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.base.SortOrder;
import gov.nysenate.openleg.model.notification.Notification;
import gov.nysenate.openleg.model.notification.NotificationType;
import gov.nysenate.openleg.model.search.SearchException;
import gov.nysenate.openleg.model.updates.UpdateType;
import gov.nysenate.openleg.util.DateUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.authz.UnauthenticatedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import static gov.nysenate.openleg.model.notification.NotificationType.REQUEST_EXCEPTION;

public abstract class BaseCtrl
{
    private static final Logger logger = LoggerFactory.getLogger(BaseCtrl.class);

    public static final String BASE_API_PATH = "/api/3";
    public static final String BASE_ADMIN_API_PATH = BASE_API_PATH + "/admin";

    /** Maximum number of results that can be requested via the query params. */
    private static final int MAX_LIMIT = 1000;

    @Autowired
    private EventBus eventBus;

    /** --- Param grabbers --- */

    /**
     * Returns a sort order extracted from the given web request parameters
     * Returns the given default sort order if no such parameter exists
     *
     * @param webRequest WebRequest
     * @param defaultSortOrder SortOrder
     * @return SortOrder
     */
    protected SortOrder getSortOrder(WebRequest webRequest, SortOrder defaultSortOrder) {
        try {
            return SortOrder.valueOf(webRequest.getParameter("order").toUpperCase());
        }
        catch (Exception ex) {
            return defaultSortOrder;
        }
    }

    /**
     * Returns a limit + offset extracted from the given web request parameters
     * Returns the given default limit offset if no such parameters exist
     *
     * @param webRequest WebRequest
     * @param defaultLimit int - The default limit to use, 0 for no limit
     * @return LimitOffset
     */
    protected LimitOffset getLimitOffset(WebRequest webRequest, int defaultLimit) {
        int limit = defaultLimit;
        int offset = 0;
        if (webRequest.getParameter("limit") != null) {
            String limitStr = webRequest.getParameter("limit");
            if (limitStr.equalsIgnoreCase("all")) {
                limit = 0;
            }
            else {
                limit = NumberUtils.toInt(limitStr, defaultLimit);
                if (limit > MAX_LIMIT) {
                    throw new InvalidRequestParamEx(limitStr, "limit", "int", "Must be <= " + MAX_LIMIT);
                }
            }
        }
        if (webRequest.getParameter("offset") != null) {
            offset = NumberUtils.toInt(webRequest.getParameter("offset"), 0);
        }
        return new LimitOffset(limit, offset);
    }

    /**
     * Extracts a date range from the query parameters 'startDate' and 'endDate'.
     *
     * @param webRequest WebRequest
     * @param defaultRange Range<LocalDate>
     * @return Range<LocalDate>
     */
    protected Range<LocalDate> getDateRangeFromParams(WebRequest webRequest, Range<LocalDate> defaultRange) {
        try {
            LocalDate startDate = null;
            LocalDate endDate = null;
            if (webRequest.getParameterMap().containsKey("startDate")) {
                startDate = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(webRequest.getParameter("startDate")));
            }
            if (webRequest.getParameterMap().containsKey("endDate")) {
                endDate = LocalDate.from(DateTimeFormatter.ISO_DATE.parse(webRequest.getParameter("endDate")));
            }
            return (startDate == null && endDate == null)
                ? defaultRange
                : Range.closed(startDate != null ? startDate : DateUtils.LONG_AGO,
                                 endDate != null ? endDate   : DateUtils.THE_FUTURE);
        }
        catch (Exception ex) {
            return defaultRange;
        }
    }

    /**
     * Attempts to parse a date request parameter
     * Throws an InvalidRequestParameterException if the parsing went wrong
     *
     * @param dateString The parameter value to be parsed
     * @param parameterName The name of the parameter.  Used to generate the exception
     * @return LocalDate
     * @throws InvalidRequestParamEx
     */
    protected LocalDate parseISODate(String dateString, String parameterName) {
        try {
            return LocalDate.from(DateTimeFormatter.ISO_DATE.parse(dateString));
        }
        catch (DateTimeParseException ex) {
            throw new InvalidRequestParamEx(dateString, parameterName,
                "date", "ISO 8601 date formatted string e.g. 2014-10-27 for October 27, 2014");
        }
    }

    /**
     * Attempts to parse a date time request parameter
     * Throws an InvalidRequestParameterException if the parsing went wrong
     *
     * @param dateTimeString The parameter value to be parsed
     * @param parameterName The name of the parameter.  Used to generate the exception
     * @return LocalDateTime
     * @throws InvalidRequestParamEx
     */
    protected LocalDateTime parseISODateTime(String dateTimeString, String parameterName) {
        try {
            return LocalDateTime.from(DateTimeFormatter.ISO_DATE_TIME.parse(dateTimeString));
        }
        catch (DateTimeParseException ex) {
            throw new InvalidRequestParamEx(dateTimeString, parameterName,
                "date-time", "ISO 8601 date and time formatted string e.g. 2014-10-27T09:44:55 for October 27, 2014 9:44:55 AM");
        }
    }

    /**
     * Parses the specified query param as a boolean or returns the default value if the param is not set.
     *
     * @param param WebRequest
     * @param defaultVal boolean
     * @return boolean
     */
    protected boolean getBooleanParam(WebRequest request, String param, boolean defaultVal) {
        return (request.getParameter(param) != null) ? BooleanUtils.toBoolean(request.getParameter(param)) : defaultVal;
    }

    /**
     * Parses the update type from the request parameters.
     *
     * @param request WebRequest
     * @return UpdateType
     */
    protected UpdateType getUpdateTypeFromParam(WebRequest request) {
        String type = request.getParameter("type");
        return (type != null && type.equalsIgnoreCase("processed"))
            ? UpdateType.PROCESSED_DATE : UpdateType.PUBLISHED_DATE;
    }

    protected NotificationType getNotificationTypeFromString(String text) {
        try {
            return NotificationType.getValue(text);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestParamEx(text, "type", "String",
                    NotificationType.getAllNotificationTypes().stream()
                            .map(NotificationType::toString)
                            .reduce("", (a, b) -> a + "|" + b));
        }
    }

    /** --- Generic Exception Handlers --- */

    @ExceptionHandler(Exception.class)
    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    protected ErrorResponse handleUnknownError(Exception ex) {
        logger.error("Caught unhandled servlet exception:\n{}", ExceptionUtils.getStackTrace(ex));
        pushExceptionNotification(ex);
        return new ErrorResponse(ErrorCode.UNKNOWN_ERROR);
    }

    @ExceptionHandler(InvalidRequestParamEx.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleInvalidRequestParameterException(InvalidRequestParamEx ex) {
        logger.debug(ExceptionUtils.getStackTrace(ex));
        return new ViewObjectErrorResponse(ErrorCode.INVALID_ARGUMENTS, new InvalidParameterView(ex));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    protected ErrorResponse handleMissingParameterException(MissingServletRequestParameterException ex) {
        logger.debug(ExceptionUtils.getStackTrace(ex));
        return new ViewObjectErrorResponse(ErrorCode.MISSING_PARAMETERS,
            new ParameterView(ex.getParameterName(), ex.getParameterType()));
    }

    @ExceptionHandler(SearchException.class)
    @ResponseStatus(value = HttpStatus.BAD_REQUEST)
    public ViewObjectErrorResponse searchExceptionHandler(SearchException ex) {
        logger.debug("Search Exception!", ex);
        return new ViewObjectErrorResponse(ErrorCode.SEARCH_ERROR, ex.getMessage());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleUnauthenticatedException(UnauthenticatedException ex) {
        logger.debug("Unauthenticated Exception! {}", ex.getMessage());
        return new ErrorResponse(ErrorCode.UNAUTHORIZED);
    }

    private void pushExceptionNotification(Exception ex) {
        LocalDateTime occurred = LocalDateTime.now();
        String summary = ex.getMessage();
        String message = "The following exception was thrown while handling a request at " + occurred + "\n\n"
                + ExceptionUtils.getStackTrace(ex);
        Notification notification = new Notification(REQUEST_EXCEPTION, occurred, summary, message);

        eventBus.post(notification);
    }
}