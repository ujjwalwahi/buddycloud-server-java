package test.org.buddycloud.channels.packetHandler.IQ.Namespace;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.buddycloud.channels.jedis.JedisKeys;
import org.buddycloud.channels.packet.ErrorPacket;
import org.buddycloud.channels.packet.ErrorPacketBuilder;
import org.buddycloud.channels.packetHandler.IQ.IQHandler;
import org.buddycloud.channels.packetHandler.IQ.Namespace.JabberDiscoInfo;
import org.buddycloud.channels.packetHandler.IQ.Namespace.JabberDiscoItems;
import org.buddycloud.channels.packetHandler.IQ.Namespace.JabberRegister;
import org.buddycloud.channels.queue.ErrorQueue;
import org.buddycloud.channels.queue.OutQueue;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.junit.After;
import org.junit.Before;
import org.xmpp.packet.IQ;
import org.xmpp.packet.IQ.Type;

import redis.clients.jedis.Jedis;


public class JabberRegisterTest extends TestCase {

	public OutQueue outQueue;
	public ErrorQueue errorQueue;
	public Jedis jedis;
	
	@Before
	public void setUp() throws Exception {
		this.outQueue = new OutQueue(null, null, false);
		this.errorQueue = new ErrorQueue(outQueue);
		this.jedis = new Jedis("localhost", 9876);
		
		this.jedis.flushDB();
	}

	@After
	public void tearDown() throws Exception {
		this.jedis.disconnect();
	}

	public void testJabberRegisterSuccess() throws InterruptedException {
		
		IQHandler iqHandler = new IQHandler(outQueue, errorQueue, jedis);
		
		String id = "testJabberRegisterSuccess";
		
		IQ mockIQ = new IQ(IQ.Type.set);
		mockIQ.setID(id);
		mockIQ.setTo("channels.koski.com");
		mockIQ.setFrom("tuomas@koski.com/client");
		
		mockIQ.setChildElement("query", JabberRegister.NAMESPACE_URI);
		
		iqHandler.ingestPacket(mockIQ);
		
		Thread.sleep(50);
		
		IQ result = (IQ)iqHandler.outQueue.getQueue().poll();
		
		assertEquals(IQ.Type.result, result.getType());
		assertEquals(id, result.getID());
		
		String type = jedis.type("node:/user/tuomas@koski.com/status:conf");
		assertEquals("hash", type);
		
		assertTrue(jedis.sismember(JedisKeys.LOCAL_USERS, mockIQ.getFrom().toBareJID()));
		
	}

	public void testJabberRegisterAlreadyRegistered() throws InterruptedException {
		
		IQHandler iqHandler = new IQHandler(outQueue, errorQueue, jedis);
		
		String id = "testJabberRegisterAlreadyRegistered";
		
		IQ mockIQ = new IQ(IQ.Type.set);
		mockIQ.setID(id);
		mockIQ.setTo("channels.koski.com");
		mockIQ.setFrom("tuomas@koski.com/client");
		
		jedis.sadd(JedisKeys.LOCAL_USERS, mockIQ.getFrom().toBareJID());
		
		mockIQ.setChildElement("query", JabberRegister.NAMESPACE_URI);
		
		iqHandler.ingestPacket(mockIQ.createCopy());
		
		Thread.sleep(50);
		
		IQ result = (IQ)iqHandler.outQueue.getQueue().poll();
		//System.out.println(result.toXML());
		
		Element error = new DOMElement("error");
		error.addAttribute("type", "cancel");
		Element conflict = new DOMElement("conflict",
					 								 new org.dom4j.Namespace("", ErrorPacket.NS_XMPP_STANZAS));
		error.add(conflict);
		
		assertEquals(result.getError().toXML(), error.asXML());
		
		assertEquals(IQ.Type.error, result.getType());
		assertEquals(id, result.getID());
		
	}
	
}
