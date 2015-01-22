package gov.nysenate.openleg.service.notification;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import gov.nysenate.openleg.config.Environment;
import gov.nysenate.openleg.dao.notification.NotificationDao;
import gov.nysenate.openleg.model.notification.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationDispatcher {

    @Autowired
    private EventBus eventBus;

    @Autowired
    private Environment environment;

    @Autowired
    private NotificationDao notificationDao;

    @Autowired
    private NotificationSubscriptionDataService subscriptionDataService;

    @Autowired
    private List<NotificationSender> notificationSenders;

    private ImmutableMap<NotificationTarget, NotificationSender> senderMap;

    @PostConstruct
    public void init() {
        Map<NotificationTarget, NotificationSender> senderProtoMap = new HashMap<>();
        notificationSenders.forEach(sender -> senderProtoMap.put(sender.getTargetType(), sender));
        senderMap = ImmutableMap.copyOf(senderProtoMap);

        eventBus.register(this);
    }

    /**
     * Sends a registered notification to all pertinent subscribers
     * @param notification NotificationBody
     */
    @Async
    public void dispatchNotification(RegisteredNotification notification) {
        if (environment.isNotificationsEnabled()) {
            Multimap<NotificationTarget, NotificationSubscription> subscriptionMap = ArrayListMultimap.create();
            subscriptionDataService.getSubscriptions(notification.getType()).forEach(subscription ->
                    subscriptionMap.put(subscription.getTarget(), subscription));

            subscriptionMap.keySet().forEach(target ->
                    senderMap.get(target).sendNotification(notification, subscriptionMap.get(target)));
        }
    }

    @Subscribe
    public void handleNotificationEvent(Notification notification) {
        RegisteredNotification registeredNotification = notificationDao.registerNotification(notification);
        dispatchNotification(registeredNotification);
    }
}