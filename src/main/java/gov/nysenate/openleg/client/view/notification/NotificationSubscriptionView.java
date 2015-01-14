package gov.nysenate.openleg.client.view.notification;

import gov.nysenate.openleg.client.view.base.ViewObject;
import gov.nysenate.openleg.model.notification.NotificationSubscription;
import gov.nysenate.openleg.model.notification.NotificationTarget;
import gov.nysenate.openleg.model.notification.NotificationType;

public class NotificationSubscriptionView implements ViewObject
{
    protected String userName;
    protected NotificationType type;
    protected NotificationTarget target;
    protected String targetAddress;

    public NotificationSubscriptionView(NotificationSubscription subscription) {
        this.userName = subscription.getUserName();
        this.type = subscription.getType();
        this.target = subscription.getTarget();
        this.targetAddress = subscription.getTargetAddress();
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public NotificationTarget getTarget() {
        return target;
    }

    public void setTarget(NotificationTarget target) {
        this.target = target;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String targetAddress) {
        this.targetAddress = targetAddress;
    }

    @Override
    public String getViewType() {
        return "notification-subscription";
    }
}
