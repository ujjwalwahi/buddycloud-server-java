package org.buddycloud.channelserver.packetprocessor.message.event;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;

import org.buddycloud.channelserver.Configuration;
import org.buddycloud.channelserver.channel.ChannelManager;
import org.buddycloud.channelserver.db.exception.NodeStoreException;
import org.buddycloud.channelserver.pubsub.affiliation.Affiliations;
import org.buddycloud.channelserver.utils.NotificationScheme;
import org.dom4j.Element;
import org.xmpp.packet.JID;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;

public class AffiliationProcessor extends AbstractMessageProcessor {

    private JID jid;
    private Affiliations affiliation;

    public AffiliationProcessor(BlockingQueue<Packet> outQueue,
            Properties configuration, ChannelManager channelManager) {
        super(channelManager, configuration, outQueue);
    }

    @Override
    public void process(Message packet) throws Exception {
        message = packet;

        handleAffiliationElement();

        if (true == Configuration.getInstance().isLocalNode(node)) {
            return;
        }
        if (null == affiliation) {
            return;
        }
        if (affiliation.equals(Affiliations.outcast)) {
            sendLocalNotifications(NotificationScheme.ownerOrModerator, jid);
        } else {
            sendLocalNotifications(NotificationScheme.validSubscribers);
        }
    }

    private void handleAffiliationElement() throws NodeStoreException {
        Element affiliationsElement = message.getElement().element("event")
                .element("affiliations");
        Element affiliationElement = affiliationsElement.element("affiliation");
        node = affiliationsElement.attributeValue("node");
        if (null == affiliationElement) {
            return;
        }
        jid = new JID(affiliationElement.attributeValue("jid"));
        affiliation = Affiliations.valueOf(affiliationElement
                .attributeValue("affiliation"));
        if (true == Configuration.getInstance().isLocalNode(node)) {
            return;
        }
        storeNewAffiliation();
    }

    private void storeNewAffiliation() throws NodeStoreException {
        addRemoteNode();
        channelManager.setUserAffiliation(node, jid, affiliation);
    }

    private void addRemoteNode() throws NodeStoreException {
        if (false == channelManager.nodeExists(node)) {
            channelManager.addRemoteNode(node);
        }
    }
}
